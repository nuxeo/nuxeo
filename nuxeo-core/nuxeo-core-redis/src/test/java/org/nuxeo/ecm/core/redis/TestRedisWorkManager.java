/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.work.WorkManagerTest;
import org.nuxeo.runtime.api.Framework;

/**
 * Test of the WorkManager using Redis. Does not run if no Redis is configured through the properties of
 * {@link RedisFeature}.
 *
 * @since 5.8
 */
public class TestRedisWorkManager extends WorkManagerTest {

    private boolean monitorRedis = false;
    private RedisExecutor redisExecutor;

    @Override
    public boolean persistent() {
        return true;
    }

    @Override
    protected void doDeploy() throws Exception {
        super.doDeploy();
        RedisFeature.setup(this);
        if (monitorRedis) {
            redisExecutor = Framework.getLocalService(RedisExecutor.class);
            redisExecutor.startMonitor();
        }
    }

    @Test
    @Override
    @Ignore("NXP-15680")
    public void testWorkManagerWork() throws Exception {
        super.testWorkManagerWork();
    }

    @Test
    @Override
    public void testClearCompletedBefore() throws Exception {
        startMonitorRedis();
        try {
            super.testClearCompletedBefore();
        } finally {
            stopMonitorRedis();
        }
    }

    private void stopMonitorRedis() {
        monitorRedis = false;
        if (redisExecutor != null) {
            redisExecutor.stopMonitor();
        }
    }

    private void startMonitorRedis() {
        monitorRedis = true;
    }
}
