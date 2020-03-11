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
package org.nuxeo.ecm.core.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.redis.contribs.RedisKeyValueStore;
import org.nuxeo.runtime.kv.AbstractKeyValueStoreTest;
import org.nuxeo.runtime.test.runner.Features;

@Features(RedisFeature.class)
public class TestRedisKeyValueStore extends AbstractKeyValueStoreTest {

    @Test
    public void testClass() {
        assertTrue(store instanceof RedisKeyValueStore);
    }

    @Test
    public void testEscapeGlob() {
        String string = "a[b]?ok*\\computer";
        String expected = "a\\[b]\\?ok\\*\\\\computer";
        assertEquals(expected, RedisKeyValueStore.ecapeGlob(string));
    }

}
