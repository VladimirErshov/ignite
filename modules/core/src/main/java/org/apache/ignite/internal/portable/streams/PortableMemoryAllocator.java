/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.portable.streams;

import org.apache.ignite.internal.util.GridUnsafe;
import org.apache.ignite.internal.util.typedef.internal.U;
import sun.misc.Unsafe;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_MARSHAL_BUFFERS_RECHECK;

/**
 * Thread-local memory allocator.
 */
public final class PortableMemoryAllocator {
    /** Memory allocator instance. */
    public static final PortableMemoryAllocator INSTANCE = new PortableMemoryAllocator();

    /** Holders. */
    private static final ThreadLocal<Chunk> holders = new ThreadLocal<>();

    /** Unsafe instance. */
    protected static final Unsafe UNSAFE = GridUnsafe.unsafe();

    /** Array offset: byte. */
    protected static final long BYTE_ARR_OFF = UNSAFE.arrayBaseOffset(byte[].class);

    /**
     * Ensures singleton.
     */
    private PortableMemoryAllocator() {
        // No-op.
    }

    public Chunk chunk() {
        Chunk holder = holders.get();

        if (holder == null)
            holders.set(holder = new Chunk());

        return holder;
    }

    /**
     * Checks whether a thread-local array is acquired or not.
     * The function is used by Unit tests.
     *
     * @return {@code true} if acquired {@code false} otherwise.
     */
    public boolean isAcquired() {
        Chunk holder = holders.get();

        return holder != null && holder.acquired;
    }

    /**
     * Thread-local byte array holder.
     */
    public static class Chunk {
        /** */
        private static final Long CHECK_FREQ = Long.getLong(IGNITE_MARSHAL_BUFFERS_RECHECK, 10000);

        /** Data array */
        private byte[] data;

        /** Max message size detected between checks. */
        private int maxMsgSize;

        /** Last time array size is checked. */
        private long lastCheck = U.currentTimeMillis();

        /** Whether the holder is acquired or not. */
        private boolean acquired;

        /**
         * Allocate.
         *
         * @param size Desired size.
         * @return Data.
         */
        public byte[] allocate(int size) {
            if (acquired)
                return new byte[size];

            acquired = true;

            if (data == null || size > data.length)
                data = new byte[size];

            return data;
        }

        /**
         * Reallocate.
         *
         * @param data Old data.
         * @param size Size.
         * @return New data.
         */
        public byte[] reallocate(byte[] data, int size) {
            byte[] newData = new byte[size];

            if (this.data == data)
                this.data = newData;

            UNSAFE.copyMemory(data, BYTE_ARR_OFF, newData, BYTE_ARR_OFF, data.length);

            return newData;
        }

        /**
         * Shrinks array size if needed.
         */
        public void release(byte[] data, int maxMsgSize) {
            if (this.data != data)
                return;

            this.maxMsgSize = maxMsgSize;
            this.acquired = false;

            long now = U.currentTimeMillis();

            if (now - this.lastCheck >= CHECK_FREQ) {
                int halfSize = data.length >> 1;

                if (this.maxMsgSize < halfSize)
                    this.data = new byte[halfSize];

                this.lastCheck = now;
            }
        }
    }
}