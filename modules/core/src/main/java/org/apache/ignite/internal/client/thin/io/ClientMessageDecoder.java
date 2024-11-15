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

package org.apache.ignite.internal.client.thin.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decodes thin client messages from partial buffers.
 */
public class ClientMessageDecoder {
    /** */
    private ByteBuffer data;

    /** */
    private int cnt = -4;

    /** */
    private int msgSize;

    /**
     * Applies the next partial buffer.
     *
     * @param buf Buffer.
     * @return Decoded message, or null when not yet complete.
     */
    public ByteBuffer apply(ByteBuffer buf) {
        boolean msgReady = read(buf);

        return msgReady ? data : null;
    }

    private final List<ByteBuffer> bufferList = new LinkedList<>();
    private final AtomicInteger count = new AtomicInteger();

    public ClientMessageDecoder() {
        for (int i = 0; i < 4; i++) {
            bufferList.add(null);
        }
    }

    private ByteBuffer getBuffer(int msgSize) {
        ByteBuffer buffer = null;
        int firstIndex = -1;
        while (buffer == null) {
            int index = count.getAndIncrement() % bufferList.size();
            index = index >= 0 ? index : index + bufferList.size();
            firstIndex = firstIndex == -1 ? index : firstIndex == index ? -2 : firstIndex;
            buffer = bufferList.get(index);
            if (buffer == null || buffer.capacity() < msgSize) {
                buffer = ByteBuffer.allocate(msgSize).order(ByteOrder.LITTLE_ENDIAN);
                bufferList.set(index, buffer);
                break;
            } else if (buffer.hasRemaining()) {
                if (firstIndex == -2) {
                    buffer = ByteBuffer.allocate(msgSize).order(ByteOrder.LITTLE_ENDIAN);
                    bufferList.add(buffer);
                    break;
                }
                buffer = null;
            }
        }
        buffer.position(0).limit(msgSize);
        return buffer;
    }

    /**
     * Reads the buffer.
     *
     * @param buf Buffer.
     * @return True when a complete message has been received; false otherwise.
     */
    @SuppressWarnings("DuplicatedCode") // A little duplication is better than a little dependency.
    private boolean read(ByteBuffer buf) {
        if (cnt < 0) {
            for (; cnt < 0 && buf.hasRemaining(); cnt++)
                msgSize |= (buf.get() & 0xFF) << (8 * (4 + cnt));

            if (cnt < 0)
                return false;
            data = getBuffer(msgSize);
        }

        assert data != null;
        assert cnt >= 0;
        assert msgSize > 0;

        int remaining = buf.remaining();

        if (remaining > 0) {
            int missing = msgSize - cnt;

            if (missing > 0) {
                int len = Math.min(missing, remaining);

                data.put(buf);

                cnt += len;
            }
        }

        if (cnt == msgSize) {
            data.position(0);
            cnt = -4;
            msgSize = 0;
            return true;
        }

        return false;
    }
}
