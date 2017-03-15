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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.redis;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.redis.RedisFeature.Mode;
import org.nuxeo.ecm.core.redis.contribs.RedisClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RowId;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features(RedisFeature.class)
public class TestRedisClusterInvalidator {

    @Inject
    protected RedisFeature redisFeature;

    @Test
    public void testInitializeAndClose() throws Exception {
        RedisClusterInvalidator rci = createRedisClusterInvalidator("node1");
        rci.close();
    }

    private RedisClusterInvalidator createRedisClusterInvalidator(String node) {
        assumeTrueRedisServer();
        RepositoryImpl repository = mock(RepositoryImpl.class);
        when(repository.getName()).thenReturn("test");
        RedisClusterInvalidator rci = new RedisClusterInvalidator();
        rci.initialize(node, repository);
        return rci;
    }

    private void assumeTrueRedisServer() {
        Assume.assumeTrue("Require a true Redis server with pubsub support", Mode.server == redisFeature.getMode());
    }

    @Test
    public void testSendReceiveInvalidations() throws Exception {
        RedisExecutor redisExecutor = Framework.getLocalService(RedisExecutor.class);
        redisExecutor.startMonitor();
        int delayMs = 10000;
        RedisClusterInvalidator rci2 = createRedisClusterInvalidator("node2");
        RedisClusterInvalidator rci1 = createRedisClusterInvalidator("node1");
        try {
            Invalidations invals = new Invalidations();
            invals.addModified(new RowId("dublincore", "docid1"));
            invals.addModified(new RowId("dublincore", "docid2"));
            rci1.sendInvalidations(invals);
            Invalidations invalsReceived = waitForInvalidation(rci2, delayMs);
            assertNotNull("No invalidation received after " + delayMs + " ms", invalsReceived.isEmpty());
            assertEquals(invals.toString(), invalsReceived.toString());
        } finally {
            rci1.close();
            rci2.close();
            redisExecutor.stopMonitor();
        }
    }

    private Invalidations waitForInvalidation(RedisClusterInvalidator rci2, int countdown_ms) throws InterruptedException {
        Invalidations ret;
        do  {
            Thread.sleep(10);
            countdown_ms -= 10;
            ret = rci2.receiveInvalidations();
        } while (ret.isEmpty() && countdown_ms > 0);
        return ret;
    }

    @Test
    public void testSendReceiveMultiInvalidations() throws Exception {
        int delayMs = 10000;
        RedisExecutor redisExecutor = Framework.getLocalService(RedisExecutor.class);
        redisExecutor.startMonitor();
        RedisClusterInvalidator rci2 = createRedisClusterInvalidator("node2");
        RedisClusterInvalidator rci1 = createRedisClusterInvalidator("node1");
        try {
            Invalidations invals = new Invalidations();
            invals.addModified(new RowId("dublincore", "docid1"));
            rci1.sendInvalidations(invals);
            invals = new Invalidations();
            invals.addModified(new RowId("dublincore", "docid2"));
            rci1.sendInvalidations(invals);
            Invalidations invalsReceived = waitForInvalidation(rci2, delayMs);
            assertNotNull(invals.modified);
            assertNotNull("No invalidation received after " + delayMs + " ms", invalsReceived.modified);
            assertEquals(2, invalsReceived.modified.size());
        } finally {
            rci1.close();
            rci2.close();
            redisExecutor.stopMonitor();
        }
    }

}
