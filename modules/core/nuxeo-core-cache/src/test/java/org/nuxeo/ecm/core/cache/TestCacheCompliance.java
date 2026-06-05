/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mhilaire
 */
package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.SharedMetricRegistries;

@RunWith(FeaturesRunner.class)
@Features(CacheFeature.class)
public class TestCacheCompliance {

    @Inject
    @Named(CacheFeature.DEFAULT_TEST_CACHE_NAME)
    protected Cache defaultCache;

    @Test
    public void getValue() {
        String cachedVal = (String) defaultCache.get(CacheFeature.KEY);
        assertTrue(defaultCache.hasEntry(CacheFeature.KEY));
        assertEquals(CacheFeature.VAL, cachedVal);
    }

    @Test
    public void computeIfAbsent() {
        assertNull(defaultCache.get("key2"));
        assertEquals("val2", defaultCache.computeIfAbsent("key2", () -> "val2"));
        assertEquals("val2", defaultCache.get("key2"));
    }

    @Test
    public void keySet() {
        assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.put("key2", "val2");
        Set<String> keys = defaultCache.keySet();
        assertTrue(keys.contains("key2"));
    }

    @Test
    public void keyNotExist() {
        assertNull(defaultCache.get("key-not-exist"));
        assertFalse(defaultCache.hasEntry("key-not-exist"));
    }

    @Test
    public void putUpdateGet() {
        String val2 = "val2";
        defaultCache.put(CacheFeature.KEY, val2);
        val2 = (String) defaultCache.get(CacheFeature.KEY);
        assertEquals("val2", val2);
    }

    @Test
    public void putNullKey() {
        var e = assertThrows(IllegalArgumentException.class, () -> defaultCache.put(null, "val-null"));
        assertEquals("Can't put a null key for the cache 'default-test-cache'!", e.getMessage());
    }

    /**
     * @since 2025.21
     */
    @Test
    public void computeIfAbsentNullKey() {
        // Null key must not throw: the supplier is invoked and its value returned without being cached,
        // mirroring the null-safe behavior of get(null).
        var counter = new int[1];
        Supplier<Serializable> supplier = () -> {
            counter[0]++;
            return "val-null";
        };
        assertEquals("val-null", defaultCache.computeIfAbsent(null, supplier));
        assertEquals(1, counter[0]);
        // No caching: a second call invokes the supplier again.
        assertEquals("val-null", defaultCache.computeIfAbsent(null, supplier));
        assertEquals(2, counter[0]);
        // And get(null) still returns null.
        assertNull(defaultCache.get(null));
    }

    @Test
    @Ignore
    public void ttlExpire() throws InterruptedException {
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) defaultCache.get(CacheFeature.KEY);
        assertNull(expiredVal);
    }

    @Test
    public void invalidateKey() {
        assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.invalidate(CacheFeature.KEY);
        assertNull(defaultCache.get(CacheFeature.KEY));
    }

    @Test
    public void invalidateAll() {
        assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.put("key2", "val2");
        assertNotNull(defaultCache.get("key2"));
        defaultCache.invalidateAll();
        assertNull(defaultCache.get(CacheFeature.KEY));
        assertNull(defaultCache.get("key2"));
    }

    @Test
    public void hasMetrics() {
        List<MetricName> expected = Stream.of("nuxeo.cache.read", "nuxeo.cache.hit", "nuxeo.cache.hit.ratio",
                "nuxeo.cache.invalidation", "nuxeo.cache.size")
                                          .map(name -> MetricName.build(name).tagged("cache", "default-test-cache"))
                                          .toList();
        assertTrue(SharedMetricRegistries.getOrCreate(MetricsService.class.getName()).getNames().containsAll(expected));
    }
}
