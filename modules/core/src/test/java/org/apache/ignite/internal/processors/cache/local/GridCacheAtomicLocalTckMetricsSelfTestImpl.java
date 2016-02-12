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

package org.apache.ignite.internal.processors.cache.local;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.IgniteCache;

/**
 * Local atomic cache metrics test with tck specific.
 */
public class GridCacheAtomicLocalTckMetricsSelfTestImpl extends GridCacheAtomicLocalMetricsSelfTest {
    /**
     * @throws Exception If failed.
     */
    public void testEntryProcessorRemove() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(null);

        cache.put(1, 20);

        int result = cache.invoke(1, new EntryProcessor<Integer, Integer, Integer>() {
            @Override public Integer process(MutableEntry<Integer, Integer> entry, Object... arguments)
                    throws EntryProcessorException {
                Integer result = entry.getValue();

                entry.remove();

                return result;
            }
        });

        assertEquals(1L, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        assertEquals(20, result);
        assertEquals(1L, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(100.0f, cache.metrics(grid(0).cluster().forLocal()).getCacheHitPercentage());
        assertEquals(0L, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(0f, cache.metrics(grid(0).cluster().forLocal()).getCacheMissPercentage());
        assertEquals(1L, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());
        assertEquals(1L, cache.metrics(grid(0).cluster().forLocal()).getCacheRemovals());
        assertEquals(0L, cache.metrics(grid(0).cluster().forLocal()).getCacheEvictions());
        assert cache.metrics(grid(0).cluster().forLocal()).getAveragePutTime() >= 0;
        assert cache.metrics(grid(0).cluster().forLocal()).getAverageGetTime() >= 0;
        assert cache.metrics(grid(0).cluster().forLocal()).getAverageRemoveTime() >= 0;
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheStatistics() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(null);

        cache.put(1, 10);

        assertEquals(0, cache.metrics(grid(0).cluster().forLocal()).getCacheRemovals());
        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        cache.remove(1);

        assertEquals(0, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCacheRemovals());
        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        cache.remove(1);

        assertEquals(0, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(0, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCacheRemovals());
        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        cache.put(1, 10);
        assertTrue(cache.remove(1, 10));

        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(0, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(2, cache.metrics(grid(0).cluster().forLocal()).getCacheRemovals());
        assertEquals(2, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        assertFalse(cache.remove(1, 10));

        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(1, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(2, cache.metrics(grid(0).cluster().forLocal()).getCacheRemovals());
        assertEquals(2, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());
    }

    /**
     * @throws Exception If failed.
     */
    public void testConditionReplace() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(null);

        long hitCount = 0;
        long missCount = 0;
        long putCount = 0;

        boolean result = cache.replace(1, 0, 10);

        ++missCount;
        assertFalse(result);

        assertEquals(missCount, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(hitCount, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(putCount, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        assertNull(cache.localPeek(1));

        cache.put(1, 10);
        ++putCount;

        assertEquals(missCount, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(hitCount, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(putCount, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        assertNotNull(cache.localPeek(1));

        result = cache.replace(1, 10, 20);

        assertTrue(result);
        ++hitCount;
        ++putCount;

        assertEquals(missCount, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(hitCount, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(putCount, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        result = cache.replace(1, 40, 50);

        assertFalse(result);
        ++hitCount;

        assertEquals(hitCount, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(putCount, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());
        assertEquals(missCount, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutIfAbsent() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(null);

        long hitCount = 0;
        long missCount = 0;
        long putCount = 0;

        boolean result = cache.putIfAbsent(1, 1);

        ++putCount;
        assertTrue(result);

        assertEquals(missCount, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
        assertEquals(hitCount, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(putCount, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());

        result = cache.putIfAbsent(1, 1);

        assertFalse(result);
        assertEquals(hitCount, cache.metrics(grid(0).cluster().forLocal()).getCacheHits());
        assertEquals(putCount, cache.metrics(grid(0).cluster().forLocal()).getCachePuts());
        assertEquals(missCount, cache.metrics(grid(0).cluster().forLocal()).getCacheMisses());
    }
}