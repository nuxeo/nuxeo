/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.SharedMetricRegistries;

@RunWith(FeaturesRunner.class)
@Features({ CacheFeature.class })
public class CacheComplianceFixture {

    @Inject
    @Named(CacheFeature.DEFAULT_TEST_CACHE_NAME)
    Cache defaultCache;

    @Test
    public void getValue() throws IOException {
        String cachedVal = (String) defaultCache.get(CacheFeature.KEY);
        Assert.assertTrue(defaultCache.hasEntry(CacheFeature.KEY));
        Assert.assertEquals(CacheFeature.VAL, cachedVal);
    }

    @Test
    public void keySet() {
        Assert.assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.put("key2", "val2");
        Set<String> keys = defaultCache.keySet();
        Assert.assertTrue(keys.contains("key2"));
    }

    @Test
    public void keyNotExist() throws IOException {
        Assert.assertNull(defaultCache.get("key-not-exist"));
        Assert.assertFalse(defaultCache.hasEntry("key-not-exist"));
    }

    @Test
    public void putUpdateGet() throws IOException {
        String val2 = "val2";
        defaultCache.put(CacheFeature.KEY, val2);
        val2 = (String) defaultCache.get(CacheFeature.KEY);
        Assert.assertEquals("val2", val2);
    }

    @Test
    public void putNullKey() throws IOException {
        try {
            defaultCache.put(null, "val-null");
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    @Ignore
    public void ttlExpire() throws InterruptedException, IOException {
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) defaultCache.get(CacheFeature.KEY);
        Assert.assertNull(expiredVal);
    }

    @Test
    public void invalidateKey() throws IOException {
        Assert.assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.invalidate(CacheFeature.KEY);
        Assert.assertNull(defaultCache.get(CacheFeature.KEY));
    }

    @Test
    public void invalidateAll() throws IOException {
        Assert.assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.put("key2", "val2");
        Assert.assertNotNull(defaultCache.get("key2"));
        defaultCache.invalidateAll();
        Assert.assertNull(defaultCache.get(CacheFeature.KEY));
        Assert.assertNull(defaultCache.get("key2"));
    }

    @Test
    public void hasMetrics() {
        List<MetricName> expected = Arrays.asList("nuxeo.cache.read", "nuxeo.cache.hit", "nuxeo.cache.hit.ratio",
                "nuxeo.cache.invalidation", "nuxeo.cache.size")
                                          .stream()
                                          .map(name -> MetricName.build(name).tagged("cache", "default-test-cache"))
                                          .collect(Collectors.toList());
        Assert.assertTrue(
                SharedMetricRegistries.getOrCreate(MetricsService.class.getName()).getNames().containsAll(expected));
    }
}
