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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.Protocol;

/**
 * Descriptor for a Redis configuration.
 *
 * @since 5.8
 */
@XObject("redis")
public class RedisConfigurationDescriptor {

    @XNode("@disabled")
    public boolean disabled = false;

    @XNode("prefix")
    public String prefix = "nuxeo:";

    @XNode("password")
    public String password;

    @XNode("database")
    public int database = Protocol.DEFAULT_DATABASE;

    @XNode("timeout")
    public int timeout = Protocol.DEFAULT_TIMEOUT;

    @XNode("hosts")
    public RedisConfigurationHostDescriptor[] hosts = new RedisConfigurationHostDescriptor[0];

    @XNode("master")
    public String master;

    @XNode("failoverTimeout")
    public long failoverTimeout = 3000;

    @XNode("host")
    public void setHost(String name) {
        if (hosts.length == 0) {
            hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                    name, Protocol.DEFAULT_PORT) };
        } else {
            hosts[0].name = name;
        }
    }

    @XNode("port")
    public void setHost(int port) {
        if (hosts.length == 0) {
            hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                    "localhost", port) };
        } else {
            hosts[0].port = port;
        }
    }

    protected boolean isSentinel() {
        return StringUtils.isNotBlank(master);
    }
}
