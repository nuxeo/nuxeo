package org.nuxeo.ecm.core.redis;
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
 *     bdelbosc
 */

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.redis.contribs.RedisClusterInvalidator;
import org.nuxeo.ecm.core.redis.contribs.RedisInvalidations;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RowId;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class, RedisFeature.class})
public class TestRedisClusterInvalidator {

    @Test
    public void testInitializeAndClose() throws Exception {
        RedisClusterInvalidator rci = createRedisClusterInvalidator("node1");
        rci.close();
    }

    private RedisClusterInvalidator createRedisClusterInvalidator(String node) {
        assumeTrueRedisServer();
        RepositoryImpl repository = getDefaultRepository();
        RedisClusterInvalidator rci = new RedisClusterInvalidator();
        rci.initialize(node, repository);
        return rci;
    }

    private RepositoryImpl getDefaultRepository() {
        SQLRepositoryService repositoryService = Framework.getService(SQLRepositoryService.class);
        return repositoryService.getRepositoryImpl(repositoryService.getRepositoryNames().get(0));
    }

    private void assumeTrueRedisServer() {
        Assume.assumeTrue("Require a true Redis server with pipelined and pubsub support",
                "server".equals(Framework.getProperty("nuxeo.test.redis.mode")));
    }

    @Test
    public void testSendReceiveInvalidations() throws Exception {
        RedisClusterInvalidator rci1 = createRedisClusterInvalidator("node1");
        RedisClusterInvalidator rci2 = createRedisClusterInvalidator("node2");
        try {
            Invalidations invals = new Invalidations();
            invals.addModified(new RowId("dublincore", "docid1"));
            invals.addModified(new RowId("dublincore", "docid2"));
            rci1.sendInvalidations(invals);
            Thread.sleep((1000));
            Invalidations invalsReceived = rci2.receiveInvalidations();
            assertEquals(invals.toString(), invalsReceived.toString());
        } finally {
            rci1.close();
            rci2.close();
        }
    }

    @Test
    public void testSendReceiveMultiInvalidations() throws Exception {
        RedisClusterInvalidator rci1 = createRedisClusterInvalidator("node1");
        RedisClusterInvalidator rci2 = createRedisClusterInvalidator("node2");
        try {
            Invalidations invals = new Invalidations();
            invals.addModified(new RowId("dublincore", "docid1"));
            rci1.sendInvalidations(invals);
            invals = new Invalidations();
            invals.addModified(new RowId("dublincore", "docid2"));
            rci1.sendInvalidations(invals);
            Thread.sleep((1000));
            Invalidations invalsReceived = rci2.receiveInvalidations();
            assertEquals(2, invalsReceived.modified.size());
        } finally {
            rci1.close();
            rci2.close();
        }
    }

}
