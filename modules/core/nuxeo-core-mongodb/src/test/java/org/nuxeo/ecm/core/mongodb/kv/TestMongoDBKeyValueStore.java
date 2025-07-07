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
package org.nuxeo.ecm.core.mongodb.kv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.nuxeo.runtime.kv.AbstractKeyValueStoreTest;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(MongoDBFeature.class)
@Deploy("org.nuxeo.ecm.core.mongodb.test:OSGI-INF/mongodb-keyvalue-test-contrib.xml")
public class TestMongoDBKeyValueStore extends AbstractKeyValueStoreTest {

    @Test
    public void testClass() {
        assertTrue(store instanceof MongoDBKeyValueStore);
    }

    @Override
    protected boolean hasSlowTTLExpiration() {
        return true;
    }

    @Override
    protected void sleepForTTLExpiration() {
        try {
            // the MongoDB TTLMonitor thread runs every 60 seconds
            Thread.sleep((60 + 10) * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRemove() {
        String key = "foo";
        assertEquals(Collections.emptySet(), storeKeys());

        store.put(key, BAR_B);
        assertEquals(BAR, new String(store.remove(key)));
        assertNull(store.remove(key));
        assertNull(store.get(key));

        store.put(key, BAR);
        assertEquals(BAR, store.removeString(key));
        assertNull(store.removeString(key));
        assertNull(store.get(key));

        store.put(key, Long.valueOf(123));
        assertEquals(Long.valueOf(123), store.removeLong(key));
        assertNull(store.removeLong(key));
        assertNull(store.get(key));
    }
}
