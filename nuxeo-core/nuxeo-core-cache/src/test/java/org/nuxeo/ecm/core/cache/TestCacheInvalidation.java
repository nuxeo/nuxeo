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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.cache.CacheServiceImpl.AbstractCachePubSubInvalidator;
import org.nuxeo.ecm.core.cache.CacheServiceImpl.CacheInvalidation;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

@RunWith(FeaturesRunner.class)
@Features({ TestCacheInvalidation.ClusterFeature.class, CacheFeature.class })
@Deploy("org.nuxeo.runtime.pubsub")
@Deploy("org.nuxeo.ecm.core.cache:inmemory-cache-config.xml")
public class TestCacheInvalidation {

    protected static final String NODE1 = "123";

    protected static final String NODE2 = "456";

    /** Needed so that the cache service uses an invalidator. */
    public static class ClusterFeature implements RunnerFeature {

        @Override
        public void start(FeaturesRunner runner) {
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

    public static List<CacheInvalidation> RECEIVED_INVALIDATIONS = new CopyOnWriteArrayList<>();

    public class DummyCachePubSubInvalidator extends AbstractCachePubSubInvalidator {

        @Override
        public CacheInvalidation deserialize(InputStream in) throws IOException {
            return CacheInvalidation.deserialize(in);
        }

        @Override
        public void receivedMessage(CacheInvalidation invalidation) {
            RECEIVED_INVALIDATIONS.add(invalidation);
            super.receivedMessage(invalidation);
        }

        @Override
        protected Cache getCache(String name) {
            if (!name.equals(cache.getName())) {
                throw new UnsupportedOperationException(name);
            }
            return cache;
        }
    }

    @Inject
    @Named(CacheFeature.DEFAULT_TEST_CACHE_NAME)
    protected Cache cache;

    protected DummyCachePubSubInvalidator invalidator;

    @Before
    public void setUp() {
        // manually register a second invalidator with a different node id
        invalidator = new DummyCachePubSubInvalidator();
        invalidator.initialize(CacheServiceImpl.CACHE_INVAL_PUBSUB_TOPIC, NODE2);
    }

    @After
    public void tearDown() {
        invalidator.close();
    }

    @Test
    public void testInvalidationsSent() {
        // write to the cache
        RECEIVED_INVALIDATIONS.clear();
        cache.put("key2", "val2");
        // no need to wait, everything is synchronous for in-memory pubsub

        // check invalidation
        assertEquals(1, RECEIVED_INVALIDATIONS.size());
        CacheInvalidation inval = RECEIVED_INVALIDATIONS.get(0);
        assertEquals(CacheFeature.DEFAULT_TEST_CACHE_NAME, inval.cacheName);
        assertEquals("key2", inval.key);

        // remove an entry from the cache
        RECEIVED_INVALIDATIONS.clear();
        cache.invalidate("key2");

        // check invalidation
        assertEquals(1, RECEIVED_INVALIDATIONS.size());
        inval = RECEIVED_INVALIDATIONS.get(0);
        assertEquals(CacheFeature.DEFAULT_TEST_CACHE_NAME, inval.cacheName);
        assertEquals("key2", inval.key);

        // invalidate all the keys
        RECEIVED_INVALIDATIONS.clear();
        cache.invalidateAll();

        // check invalidation for all keys
        assertEquals(1, RECEIVED_INVALIDATIONS.size());
        inval = RECEIVED_INVALIDATIONS.get(0);
        assertEquals(CacheFeature.DEFAULT_TEST_CACHE_NAME, inval.cacheName);
        assertEquals(AbstractCachePubSubInvalidator.ALL_KEYS, inval.key);
    }

    @Test
    public void testInvalidationsReceived() {
        // we have an entry in the cache
        assertEquals("val1", cache.get("key1"));

        // send an invalidation
        invalidator.sendInvalidation(CacheFeature.DEFAULT_TEST_CACHE_NAME, "key1");
        // no need to wait, everything is synchronous for in-memory pubsub

        // check the entry is gone
        assertNull(cache.get("key1"));
    }

}
