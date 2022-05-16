/*
 * Copyright 2022 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.platform.client.datastructures;

import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.internal.processors.platform.client.ClientConnectionContext;
import org.apache.ignite.internal.processors.platform.client.ClientLongResponse;
import org.apache.ignite.internal.processors.platform.client.ClientResponse;

/**
 * Atomic long add and get request.
 */
public class ClientAtomicLongValueAddAndGetRequest extends ClientAtomicLongRequest {
    /** Operand. */
    private final long operand;

    /**
     * Constructor.
     *
     * @param reader Reader.
     */
    public ClientAtomicLongValueAddAndGetRequest(BinaryRawReader reader) {
        super(reader);

        operand = reader.readLong();
    }

    /** {@inheritDoc} */
    @Override public ClientResponse process(ClientConnectionContext ctx) {
        IgniteAtomicLong atomicLong = atomicLong(ctx);

        if (atomicLong == null)
            return notFoundResponse();

        return new ClientLongResponse(requestId(), atomicLong.addAndGet(operand));
    }
}
