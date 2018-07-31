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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.xmap.XMap;

public class TestRedisDescriptors {

    protected XMap xmap;

    protected static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    @Before
    public void setUp() throws Exception {
        xmap = new XMap();
        xmap.register(RedisServerDescriptor.class);
        xmap.register(RedisSentinelDescriptor.class);
    }

    @Test
    public void testServer() throws Exception {
        RedisServerDescriptor desc = (RedisServerDescriptor) xmap.load(getResource("test-redis-server-descriptor.xml"));
        assertEquals("localhost2", desc.host);
        assertEquals(6380, desc.port);
        assertEquals("secret", desc.password);
        assertEquals(1, desc.database);
        assertEquals(2500, desc.timeout);
    }

    @Test
    public void testSentinel() throws Exception {
        RedisSentinelDescriptor desc = (RedisSentinelDescriptor) xmap.load(
                getResource("test-redis-sentinel-descriptor.xml"));
        assertEquals(2, desc.hosts.length);
        assertEquals("localhost", desc.hosts[0].name);
        assertEquals(26379, desc.hosts[0].port);
        assertEquals("localhost2", desc.hosts[1].name);
        assertEquals(26380, desc.hosts[1].port);
        assertEquals("secret", desc.password);
        assertEquals(2, desc.database);
        assertEquals(3000, desc.timeout);
    }

}
