/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.cache.InMemoryCacheImpl;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

import javax.inject.Inject;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@LocalDeploy({ "org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.tests:directory-default-user-contrib.xml" })
public class TestCacheFallbackOnDirectory {

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testGetFromCache() throws Exception {

        Directory dir = directoryService.getDirectory("userDirectory");
        try (Session sqlSession = dir.getSession()) {

            DirectoryCache cache = dir.getCache();
            assertNotNull(cache.getEntryCache());
            assertEquals("cache-" + dir.getName(), cache.getEntryCache().getName());
            assertNotNull(CacheFeature.unwrapImpl(InMemoryCacheImpl.class, cache.getEntryCache()));
            assertNotNull(cache.getEntryCacheWithoutReferences());
            assertEquals("cacheWithoutReference-" + dir.getName(), cache.getEntryCacheWithoutReferences().getName());

            MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
            Counter hitsCounter = metrics.counter(
                    MetricRegistry.name("nuxeo", "directories", "userDirectory", "cache", "hits"));
            Counter missesCounter = metrics.counter(
                    MetricRegistry.name("nuxeo", "directories", "userDirectory", "cache", "misses"));
            long baseHitsCount = hitsCounter.getCount();
            long baseMissesCount = missesCounter.getCount();

            // First call will update cache
            DocumentModel entry = sqlSession.getEntry("user_1");
            assertNotNull(entry);
            assertEquals(baseHitsCount, hitsCounter.getCount());
            assertEquals(baseMissesCount + 1, missesCounter.getCount());

            // Second call will use the cache
            entry = sqlSession.getEntry("user_1");
            assertNotNull(entry);
            assertEquals(baseHitsCount + 1, hitsCounter.getCount());
            assertEquals(baseMissesCount + 1, missesCounter.getCount());

            // Test if cache size (set to 1) is taken into account
            entry = sqlSession.getEntry("user_3");
            assertNotNull(entry);
            assertEquals(baseHitsCount + 1, hitsCounter.getCount());
            assertEquals(baseMissesCount + 2, missesCounter.getCount());

            entry = sqlSession.getEntry("user_3");
            assertNotNull(entry);
            assertEquals(baseHitsCount + 2, hitsCounter.getCount());
            assertEquals(baseMissesCount + 2, missesCounter.getCount());
        }
    }
}