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

import redis.clients.jedis.JedisPool;

/**
 * Service allowing configuration and access to a Redis instance.
 *
 * @since 5.8
 */
public interface RedisService {

    /**
     * Gets the configured Jedis pool, or {@code null} if not configured.
     * <p>
     * From the pool, you get a {@link redis.clients.jedis.Jedis Jedis}
     * connection using {@link JedisPool#getResource}, which MUST BE FOLLOWED by
     * a {@code try} and a {@code finally} block in which
     * {@link JedisPool#returnResource} MUST BE CALLED.
     *
     * @return the configured Jedis pool, or {@code null} if none
     * @since 5.8
     */
    JedisPool getJedisPool();

    /**
     * Gets the prefix to use when construction Redis keys.
     *
     * @since 5.8
     * @return the prefix
     */
    String getPrefix();

}
