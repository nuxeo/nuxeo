/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.redis.contribs;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.transientstore.AbstractTransientStore;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;

/**
 * Redis implementation (i.e. Cluster Aware) implementation of the {@link TransientStore}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */

public class RedisTransientStore extends AbstractTransientStore {

    protected RedisExecutor redisExecutor;

    protected String namespace;

    protected RedisAdmin redisAdmin;

    protected Log log = LogFactory.getLog(RedisTransientStore.class);

    public RedisTransientStore() {
        redisExecutor = Framework.getService(RedisExecutor.class);
        redisAdmin = Framework.getService(RedisAdmin.class);
        namespace = redisAdmin.namespace("transientCache", getConfig().getName(), "size");
    }

    @Override
    protected void incrementStorageSize(final StorageEntry entry) {
        try {
            redisExecutor.execute(new RedisCallable<Void>() {
                @Override
                public Void call(Jedis jedis) throws IOException {
                    jedis.incrBy(namespace, entry.getSize());
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Error while accesing Redis", e);
        }
    }

    @Override
    protected void decrementStorageSize(final StorageEntry entry) {
        try {
            redisExecutor.execute(new RedisCallable<Void>() {
                @Override
                public Void call(Jedis jedis) throws IOException {
                    jedis.decrBy(namespace, entry.getSize());
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Error while accesing Redis", e);
        }
    }

    @Override
    protected long getStorageSize() {
        try {
            return redisExecutor.execute(new RedisCallable<Long>() {
                @Override
                public Long call(Jedis jedis) throws IOException {
                    String value = jedis.get(namespace);
                    return Long.parseLong(value);
                }
            });
        } catch (Exception e) {
            log.error("Error while accesing Redis", e);
            return 0;
        }
    }

    @Override
    protected void setStorageSize(final long newSize) {
        try {
            redisExecutor.execute(new RedisCallable<Void>() {
                @Override
                public Void call(Jedis jedis) throws IOException {
                    jedis.set(namespace, ""+newSize);
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Error while accesing Redis", e);
        }
    }

    @Override
    public Class<? extends Cache> getCacheImplClass() {
        return RedisCache.class;
    }

}
