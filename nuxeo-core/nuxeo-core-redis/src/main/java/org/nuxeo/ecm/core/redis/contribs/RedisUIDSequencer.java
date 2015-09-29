/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis.contribs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.uidgen.AbstractUIDSequencer;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Redis-based UID generator.
 *
 * @since 7.4
 */
public class RedisUIDSequencer extends AbstractUIDSequencer {

    protected static final Log log = LogFactory.getLog(RedisUIDSequencer.class);

    protected String namespace;

    @Override
    public void init() {
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        namespace = redisAdmin.namespace("counters");
    }

    @Override
    public void dispose() {
    }

    @Override
    public int getNext(String key) {
        RedisExecutor executor = Framework.getService(RedisExecutor.class);
        try {
            return executor.execute(new RedisCallable<Long>() {
                @Override
                public Long call(Jedis jedis) {
                    return jedis.incr(namespace + key);
                }
            }).intValue();
        } catch (JedisException e) {
            throw new NuxeoException(e);
        }
    }

}
