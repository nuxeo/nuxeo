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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.redis.RedisFeature.Mode;
import org.nuxeo.ecm.core.redis.contribs.RedisDBSClusterInvalidator;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.dbs.DBSInvalidations;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, RedisFeature.class })
public class TestRedisDBSClusterInvalidator {

    @Inject
    protected RedisFeature redisFeature;

    @Test
    public void testInitializeAndClose() throws Exception {
        RedisDBSClusterInvalidator rci = createRedisDBSClusterInvalidator("node1");
        rci.close();
    }

    private RedisDBSClusterInvalidator createRedisDBSClusterInvalidator(String node) {
        assumeTrueRedisServer();
        Repository repository = getDefaultRepository();
        RedisDBSClusterInvalidator rci = new RedisDBSClusterInvalidator();
        rci.initialize(node, repository);
        return rci;
    }

    private Repository getDefaultRepository() {
        RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
        return repositoryService.getRepository("test");
    }

    private void assumeTrueRedisServer() {
        Assume.assumeTrue("Require a true Redis server with pubsub support", Mode.server == redisFeature.getMode());
    }

    @Test
    public void testSendReceiveInvalidations() throws Exception {
        RedisExecutor redisExecutor = Framework.getLocalService(RedisExecutor.class);
        redisExecutor.startMonitor();
        int delayMs = 10000;
        RedisDBSClusterInvalidator rci2 = createRedisDBSClusterInvalidator("node2");
        RedisDBSClusterInvalidator rci1 = createRedisDBSClusterInvalidator("node1");
        try {
            DBSInvalidations invals = new DBSInvalidations();
            invals.add("docid1");
            invals.add("docid2");
            rci1.sendInvalidations(invals);
            DBSInvalidations invalsReceived = waitForInvalidation(rci2, delayMs);
            assertNotNull("No invalidation received after " + delayMs + " ms", invalsReceived.isEmpty());
            assertEquals(invals.toString(), invalsReceived.toString());
        } finally {
            rci1.close();
            rci2.close();
            redisExecutor.stopMonitor();
        }
    }

    private DBSInvalidations waitForInvalidation(RedisDBSClusterInvalidator rci2, int countdown_ms)
            throws InterruptedException {
        DBSInvalidations ret;
        do {
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
        RedisDBSClusterInvalidator rci2 = createRedisDBSClusterInvalidator("node2");
        RedisDBSClusterInvalidator rci1 = createRedisDBSClusterInvalidator("node1");
        try {
            DBSInvalidations invals = new DBSInvalidations();
            invals.add("docid1");
            rci1.sendInvalidations(invals);
            invals = new DBSInvalidations();
            invals.add("docid2");
            rci1.sendInvalidations(invals);
            DBSInvalidations invalsReceived = waitForInvalidation(rci2, delayMs);
            assertNotNull(invals.ids);
            assertNotNull("No invalidation received after " + delayMs + " ms", invalsReceived.ids);
            assertEquals(2, invalsReceived.ids.size());
        } finally {
            rci1.close();
            rci2.close();
            redisExecutor.stopMonitor();
        }
    }

}
