/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
