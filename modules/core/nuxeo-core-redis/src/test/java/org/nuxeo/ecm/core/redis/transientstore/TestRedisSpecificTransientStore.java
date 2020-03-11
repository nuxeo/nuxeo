/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.redis.transientstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.redis.contribs.RedisTransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ TransientStoreFeature.class, TransientStoreRedisFeature.class })
public class TestRedisSpecificTransientStore {

    protected static final String NAMESPACE = "nuxeo:transientStore:testStore:";

    @Inject
    protected TransientStoreService tss;

    @Inject
    protected RedisExecutor redisExecutor;

    @Test
    public void verifyKeySet() {

        TransientStore ts = tss.getStore("testStore");
        TransientStoreProvider tsm = (TransientStoreProvider) ts;

        // Keys with no params nor blobs
        ts.setCompleted("size2", true);
        ts.setCompleted("key1", true);
        // Key with params only
        ts.putParameter("key2", "foo", "bar");
        // Key with params
        ts.setCompleted("key3", true);
        ts.putParameter("key3", "foo", "bar");
        // Key with blobs only
        ts.putBlobs("key4", Arrays.asList(new StringBlob("joe"), new StringBlob("jack")));
        // Key with params and blobs
        ts.putParameter("key5", "foo", "bar");
        ts.putBlobs("key5", Arrays.asList(new StringBlob("joe"), new StringBlob("jack")));

        // Check internal keys
        Set<String> expectedInternalKeys = new HashSet<>();
        expectedInternalKeys.add("size");
        expectedInternalKeys.add("size2");
        expectedInternalKeys.add("key1");
        expectedInternalKeys.add("key2:params");
        expectedInternalKeys.add("key3");
        expectedInternalKeys.add("key3:params");
        expectedInternalKeys.add("key4");
        expectedInternalKeys.add("key4:blobs:0");
        expectedInternalKeys.add("key4:blobs:1");
        expectedInternalKeys.add("key5");
        expectedInternalKeys.add("key5:params");
        expectedInternalKeys.add("key5:blobs:0");
        expectedInternalKeys.add("key5:blobs:1");

        Set<String> internalKeys = redisExecutor.execute((RedisCallable<Set<String>>) jedis -> {
            return jedis.keys(NAMESPACE + "*");
        });
        assertEquals(expectedInternalKeys.stream().map(key -> NAMESPACE + key).collect(Collectors.toSet()),
                internalKeys);

        // Check TransientStore keys
        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add("size2");
        expectedKeys.add("key1");
        expectedKeys.add("key2");
        expectedKeys.add("key3");
        expectedKeys.add("key4");
        expectedKeys.add("key5");

        assertEquals(expectedKeys, tsm.keySet());
    }

    @Test
    public void testTTL() throws Exception {

        TransientStore ts = tss.getStore("testStore");
        RedisTransientStore redisTS = (RedisTransientStore) ts;

        assertTrue(redisTS.getTTL("unknown") < 0);

        // Check first level TTL
        redisTS.putParameter("params", "param1", "value1");
        long ttl = redisTS.getTTL("params");
        assertTrue(ttl > 0 && ttl <= 7200);

        redisTS.putBlobs("blobs", Collections.singletonList(new StringBlob("content")));
        ttl = redisTS.getTTL("blobs");
        assertTrue(ttl > 0 && ttl <= 7200);

        // Check second level TTL
        redisTS.release("params");
        ttl = redisTS.getTTL("params");
        assertTrue(ttl > 0 && ttl <= 600);

        redisTS.release("blobs");
        ttl = redisTS.getTTL("blobs");
        assertTrue(ttl > 0 && ttl <= 600);
    }

}
