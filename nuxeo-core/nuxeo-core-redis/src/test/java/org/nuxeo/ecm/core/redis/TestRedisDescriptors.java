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
        xmap.register(RedisHostDescriptor.class);
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
