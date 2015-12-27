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

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.redis.contribs.RedisTransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ TransientStoreFeature.class, TransientStoreRedisFeature.class })
public class TestRedisTransientStoreTTL {

    @Test
    public void testTTL() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
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
