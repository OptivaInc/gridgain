/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.client.thin;

import org.apache.ignite.client.ClientConnectionException;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.ClientTransactions;
import org.apache.ignite.configuration.ClientTransactionConfiguration;
import org.apache.ignite.internal.binary.BinaryRawWriterEx;
import org.apache.ignite.internal.binary.BinaryWriterExImpl;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.jetbrains.annotations.NotNull;
import org.jsr166.ConcurrentLinkedHashMap;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.ignite.internal.client.thin.ProtocolVersionFeature.TRANSACTIONS;

/**
 * Implementation of {@link ClientTransactions} over TCP protocol.
 */
class TcpClientTransactions implements ClientTransactions {
    /**
     * Channel.
     */
    private final ReliableChannel ch;

    /**
     * Marshaller.
     */
    private final ClientBinaryMarshaller marsh;

    /**
     * Tx counter (used to generate tx UID).
     */
    private final AtomicLong txCnt = new AtomicLong();

    /**
     * Tx map (Tx UID to Tx).
     */
    private final ConcurrentLinkedHashMap<String, TcpClientTransaction> txMap = new ConcurrentLinkedHashMap<>();

    /**
     * Tx config.
     */
    private final ClientTransactionConfiguration txCfg;

    private final SplittableRandom random = new SplittableRandom();

    private static final String[] STR_ARR = new String[0];

    /**
     * Constructor.
     */
    TcpClientTransactions(ReliableChannel ch, ClientBinaryMarshaller marsh, ClientTransactionConfiguration txCfg) {
        this.ch = ch;
        this.marsh = marsh;
        this.txCfg = txCfg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientTransaction txStart() {
        return txStart0(null, null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientTransaction txStart(TransactionConcurrency concurrency, TransactionIsolation isolation) {
        return txStart0(concurrency, isolation, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientTransaction txStart(TransactionConcurrency concurrency, TransactionIsolation isolation, long timeout) {
        return txStart0(concurrency, isolation, timeout, null);
    }

    /**
     * @param concurrency Concurrency.
     * @param isolation   Isolation.
     * @param timeout     Timeout.
     */
    private ClientTransaction txStart0(TransactionConcurrency concurrency,
                                       TransactionIsolation isolation,
                                       Long timeout,
                                       String lb) {
        final String label = calculateLabel(lb);
        TcpClientTransaction tx0 = getTxWithLabel(label);
        if (tx0 != null) {
            return tx0;
        }

        tx0 = ch.service(ClientOperation.TX_START, req -> {
            ProtocolContext protocolCtx = req.clientChannel().protocolCtx();

            if (!protocolCtx.isFeatureSupported(TRANSACTIONS)) {
                throw new ClientProtocolError(String.format("Transactions are not supported by the server's "
                                                            + "protocol version %s, required version %s",
                                                            protocolCtx.version(),
                                                            TRANSACTIONS.verIntroduced()));
            }

            try (BinaryRawWriterEx writer = new BinaryWriterExImpl(marsh.context(), req.out(), null, null)) {
                writer.writeByte((byte) (concurrency == null
                                         ? txCfg.getDefaultTxConcurrency()
                                         : concurrency).ordinal());
                writer.writeByte((byte) (isolation == null
                                         ? txCfg.getDefaultTxIsolation()
                                         : isolation).ordinal());
                writer.writeLong(timeout == null
                                 ? txCfg.getDefaultTxTimeout()
                                 : timeout);
                writer.writeString(lb);
            }
        }, res -> new TcpClientTransaction(label, res.in().readInt(), res.clientChannel(), this));

        txMap.put(label, tx0);

        return tx0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientTransactions withLabel(String lb) {
        A.notNull(lb, "lb");

        return new ClientTransactionsWithLabel(lb);
    }

    /**
     * Current thread transaction.
     */
    public TcpClientTransaction tx() {
        Iterator<TcpClientTransaction> iterator = txMap.descendingValues().iterator();
        return iterator.hasNext()
               ? iterator.next()
               : null;
    }

    @NotNull
    private String calculateLabel(String lb) {
        if (lb == null) {
            lb = Integer.toHexString(random.nextInt(Integer.MAX_VALUE));
        }
        return lb;
    }

    @SuppressWarnings("resource")
    public void remove(String label) {
        if (label != null) {
            txMap.remove(label);
        }
    }

    @SuppressWarnings("resource")
    TcpClientTransaction getTxWithLabel(String lb) {
        TcpClientTransaction tx0 = txMap.get(lb);

        if (tx0 == null || tx0.isClosed()) {
            txMap.remove(lb);
            return null;
        } else {
            return tx0;
        }
    }

    public boolean isFree() {
        return txMap.isEmpty();
    }

    public void clean() {
        String[] txns = txMap.keySet().toArray(STR_ARR);
        for (String txn : txns) {
            try {
                TcpClientTransaction tx = txMap.remove(txn);
                if (tx != null) {
                    tx.close();
                }
            } catch (ClientException ignored) {
                // exception is not important at this point
            }
        }
    }

    /**
     * Transactions "withLabel" facade.
     */
    private class ClientTransactionsWithLabel implements ClientTransactions {
        /**
         * Transaction label.
         */
        private final String lb;

        /**
         * @param lb Transaction's label.
         */
        ClientTransactionsWithLabel(String lb) {
            this.lb = lb;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClientTransaction txStart() throws ClientServerError, ClientException {
            return txStart0(null, null, null, lb);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClientTransaction txStart(TransactionConcurrency concurrency,
                                         TransactionIsolation isolation) throws ClientServerError, ClientException {
            return txStart0(concurrency, isolation, null, lb);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClientTransaction txStart(TransactionConcurrency concurrency,
                                         TransactionIsolation isolation,
                                         long timeout) throws ClientServerError, ClientException {
            return txStart0(concurrency, isolation, timeout, lb);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClientTransactions withLabel(String lb) throws ClientException {
            A.notNull(lb, "lb");

            if (lb.equals(this.lb)) {
                return this;
            }

            return new ClientTransactionsWithLabel(lb);
        }
    }

    /**
     *
     */
    class TcpClientTransaction implements ClientTransaction {
        /**
         * Unique client-side transaction id.
         */
        private final String label;

        /**
         * Server-side transaction id.
         */
        private final int txId;

        /**
         * Client channel.
         */
        private final ClientChannel clientCh;

        /**
         * Transaction is closed.
         */
        private volatile boolean closed;

        /**
         * Transaction manager.
         */
        private final TcpClientTransactions transactionManager;

        final String creator;

        final String start;

        /**
         * @param label              Transaction Label.
         * @param id                 Transaction ID.
         * @param clientCh           Client channel.
         * @param transactionManager Transaction Manager.
         */
        TcpClientTransaction(String label, int id, ClientChannel clientCh, TcpClientTransactions transactionManager) {
            this.label = label;
            this.txId = id;
            this.clientCh = clientCh;
            this.transactionManager = transactionManager;
            creator = Thread.currentThread().getName();
            start = LocalDateTime.now().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commit() {
            endTx(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void rollback() {
            endTx(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            try {
                endTx(false);
            } catch (Exception ignore) {
                // No-op.
            }
        }

        /**
         * @param committed Committed.
         */
        private void endTx(boolean committed) {
            try {
                clientCh.service(ClientOperation.TX_END, req -> {
                    req.out().writeInt(txId);
                    req.out().writeBoolean(committed);
                }, null);
            } catch (ClientConnectionException e) {
                throw new ClientException("Transaction context has been lost due to connection errors", e);
            } catch (ClientServerError e) {
                throw new TcpClientServerException(toString(), e);
            } finally {
                transactionManager.remove(label);
                closed = true;
            }
        }

        /**
         * Tx ID.
         */
        public int txId() {
            return txId;
        }

        /**
         * Client channel.
         */
        ClientChannel clientChannel() {
            return clientCh;
        }

        /**
         * Is transaction closed.
         */
        boolean isClosed() {
            return closed;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return "TcpClientTransaction{"
                   + "label='"
                   + label
                   + '\''
                   + ", creator='"
                   + creator
                   + '\''
                   + ", txId="
                   + txId
                   + ", start='"
                   + start
                   + '\''
                   + ", now='"
                   + LocalDateTime.now()
                   + '\''
                   + '}';
        }
    }

}
