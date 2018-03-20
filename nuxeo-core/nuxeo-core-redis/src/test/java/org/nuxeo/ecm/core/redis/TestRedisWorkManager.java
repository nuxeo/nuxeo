/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.redis;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.work.WorkManagerTest;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test of the WorkManager using Redis. Does not run if no Redis is configured through the properties of
 * {@link RedisFeature}.
 *
 * @since 5.8
 */
public class TestRedisWorkManager extends WorkManagerTest {

    private final boolean monitorRedis = false;

    private RedisExecutor redisExecutor;

    @Inject
    public RuntimeHarness harness;

    @Override
    public boolean persistent() {
        return true;
    }

    @Before
    public void setUp() throws Exception {
        RedisFeature.setup(harness);
        if (monitorRedis) {
            redisExecutor = Framework.getService(RedisExecutor.class);
            redisExecutor.startMonitor();
        }
    }

    @Test
    @Override
    @Ignore("NXP-15680")
    public void testWorkManagerWork() throws Exception {
        super.testWorkManagerWork();
    }

}
