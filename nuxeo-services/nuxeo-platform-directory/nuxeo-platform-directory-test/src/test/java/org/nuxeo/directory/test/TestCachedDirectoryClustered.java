/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheServiceImpl;
import org.nuxeo.ecm.core.cache.CacheServiceImpl.AbstractCachePubSubInvalidator;
import org.nuxeo.ecm.core.cache.CacheServiceImpl.CacheInvalidation;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Test of DirectoryCache in a clustered setting.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TestCachedDirectoryClustered.ClusterFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml")
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-bundle.xml")
@Deploy("org.nuxeo.ecm.core.cache")
public class TestCachedDirectoryClustered {

    protected static final String USER_DIR = "userDirectory";

    protected static final String SCHEMA = "user";

    protected static final String NODE1 = "123";

    protected static final String NODE2 = "456";

    /** Needed so that the cache service uses an invalidator. */
    public static class ClusterFeature extends SimpleFeature {

        @Override
        public void start(FeaturesRunner runner) throws Exception {
            Framework.addListener(new RuntimeServiceListener() {

                @Override
                public void handleEvent(RuntimeServiceEvent event) {
                    if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
                        return;
                    }
                    Framework.removeListener(this);
                    setClusterId();
                }
            });
        }

        public static void setClusterId() {
            Framework.getProperties().put(CacheServiceImpl.CLUSTERING_ENABLED_PROP, "true");
            Framework.getProperties().put(CacheServiceImpl.NODE_ID_PROP, NODE1);
        }
    }

    protected static List<CacheInvalidation> RECEIVED_INVALIDATIONS = new CopyOnWriteArrayList<>();

    /** Dummy invalidator that records received invalidations. */
    public static class DummyCachePubSubInvalidator extends AbstractCachePubSubInvalidator {

        @Override
        public CacheInvalidation deserialize(InputStream in) throws IOException {
            return CacheInvalidation.deserialize(in);
        }

        @Override
        public void receivedMessage(CacheInvalidation invalidation) {
            RECEIVED_INVALIDATIONS.add(invalidation);
        }

        @Override
        protected Cache getCache(String name) {
            throw new IllegalStateException();
        }
    }

    @Inject
    protected DirectoryService directoryService;

    protected static DummyCachePubSubInvalidator invalidator;

    @BeforeClass
    public static void setUp() {
        // manually register a second invalidator with a different node id
        invalidator = new DummyCachePubSubInvalidator();
        invalidator.initialize(CacheServiceImpl.CACHE_INVAL_PUBSUB_TOPIC, NODE2);
    }

    @AfterClass
    public static void tearDown() {
        invalidator.close();
    }

    protected Session getSession() throws Exception {
        return directoryService.getDirectory(USER_DIR).getSession();
    }

    @Test
    public void testGet() throws Exception {
        try (Session session = getSession()) {
            MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
            Counter hitsCounter = metrics.counter(
                    MetricRegistry.name("nuxeo", "directories", "userDirectory", "cache", "hits"));
            Counter missesCounter = metrics.counter(
                    MetricRegistry.name("nuxeo", "directories", "userDirectory", "cache", "misses"));
            long baseHitsCount = hitsCounter.getCount();
            long baseMissesCount = missesCounter.getCount();

            // get entry
            RECEIVED_INVALIDATIONS.clear();
            DocumentModel entry = session.getEntry("user_1");
            assertNotNull(entry);
            // cache miss
            assertEquals(baseHitsCount, hitsCounter.getCount());
            assertEquals(baseMissesCount + 1, missesCounter.getCount());
            // no invalidations are sent to other nodes
            assertEquals(0, RECEIVED_INVALIDATIONS.size());

            // get entry again, will use the cache
            entry = session.getEntry("user_1");
            assertNotNull(entry);
            // cache hit
            assertEquals(baseHitsCount + 1, hitsCounter.getCount());
            assertEquals(baseMissesCount + 1, missesCounter.getCount());
            // still no invalidations sent
            assertEquals(0, RECEIVED_INVALIDATIONS.size());
        }
    }

    @Test
    public void testUpdate() throws Exception {
        try (Session session = getSession()) {

            // get entry
            DocumentModel entry = session.getEntry("user_1");

            // update entry
            RECEIVED_INVALIDATIONS.clear();
            entry.setProperty(SCHEMA, "company", "mycompany");
            session.updateEntry(entry);

            // check invalidations sent for update
            // currently, to simplify the logic, we invalidate everything
            assertEquals(4, RECEIVED_INVALIDATIONS.size());
            Set<String> invals = new HashSet<>();
            for (CacheInvalidation inval : RECEIVED_INVALIDATIONS) {
                invals.add(inval.cacheName + ":" + inval.key);
            }
            Set<String> expected = new HashSet<>(Arrays.asList( //
                    "cache-userDirectory:__ALL__", //
                    "cache-groupDirectory:__ALL__", //
                    "cacheWithoutReference-userDirectory:__ALL__", //
                    "cacheWithoutReference-groupDirectory:__ALL__" //
            ));
            assertEquals(expected, invals);
        }
    }

}
