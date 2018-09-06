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

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.work.AbstractWorkManagerTest;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Test of the WorkManager using Redis. Does not run if no Redis is configured through the properties of
 * {@link RedisFeature}.
 *
 * @since 5.8
 */

@Features({ RedisFeature.class })
public class TestRedisWorkManager extends AbstractWorkManagerTest {

    @Override
    public boolean persistent() {
        return true;
    }

    @Test
    @Override
    @Ignore("NXP-15680")
    public void testWorkManagerWork() throws Exception {
        super.testWorkManagerWork();
    }

}
