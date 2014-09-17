/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.redis.contribs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.storage.lock.AbstractLockManager;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;

/**
 * Redis-based lock manager.
 *
 * @since 5.9.6
 */
public class RedisLockManager extends AbstractLockManager {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(RedisLockManager.class);

    protected final String redisNamespace;

    protected final String repositoryName;

    protected RedisExecutor redisExecutor;

    protected String prefix;

    protected String scriptSetSha;

    protected String scriptRemoveSha;

    protected RedisAdmin redisAdmin;

    /**
     * Creates a lock manager for the given repository.
     * <p>
     * {@link #close} must be called when done with the lock manager.
     */
    public RedisLockManager(String repositoryName) {
        this.repositoryName = repositoryName;
        redisExecutor = Framework.getService(RedisExecutor.class);
        redisAdmin = Framework.getService(RedisAdmin.class);
        redisNamespace = redisAdmin.namespace("lock",repositoryName);
        loadScripts();
    }

    public void loadScripts() {
        try {
            scriptSetSha = redisAdmin.load("org.nuxeo.ecm.core.redis", "set-lock");
            scriptRemoveSha = redisAdmin.load("org.nuxeo.ecm.core.redis",
                    "remove-lock");
        } catch (IOException cause) {
            throw new NuxeoException("Cannot load lock scripts in redis", cause);
        }
    }

    protected String stringFromLock(Lock lock) {
        if (lock == null) {
            throw new NullPointerException("null lock");
        }
        return lock.getOwner() + ":" + lock.getCreated().getTimeInMillis();
    }

    protected Lock lockFromString(String lockString) {
        if (lockString == null) {
            return null;
        }
        String[] split = lockString.split(":");
        if (split.length != 2 || !StringUtils.isNumeric(split[1])) {
            throw new IllegalArgumentException("Invalid Redis lock : "
                    + lockString + ", should be " + redisNamespace + "<id>");
        }
        Calendar created = Calendar.getInstance();
        created.setTimeInMillis(Long.parseLong(split[1]));
        return new Lock(split[0], created);
    }

    @Override
    public Lock getLock(final String id) {
        try {
            return redisExecutor.execute(new RedisCallable<Lock>() {

                @Override
                public Lock call(Jedis jedis) {
                    String lockString = jedis.get(redisNamespace + id);
                    return lockFromString(lockString);
                }
            });
        } catch (IOException cause) {
            throw new RuntimeException("Lock read error on " + id, cause);
        }
    }

    @Override
    public Lock setLock(final String id, final Lock lock) {
        try {
            return redisExecutor.execute(new RedisCallable<Lock>() {

                @Override
                public Lock call(Jedis jedis) {
                    String lockString = (String) jedis.evalsha(scriptSetSha,
                            Arrays.asList(redisNamespace + id),
                            Arrays.asList(stringFromLock(lock)));
                    return lockFromString(lockString); // existing lock
                }
            });
        } catch (IOException cause) {
            throw new RuntimeException("Lock write error on " + id, cause);
        }
    }

    @Override
    public Lock removeLock(final String id, final String owner) {
        try {
            return redisExecutor.execute(new RedisCallable<Lock>() {

                @Override
                public Lock call(Jedis jedis) {
                    String lockString = (String) jedis.evalsha(scriptRemoveSha,
                            Arrays.asList(redisNamespace + id),
                            Arrays.asList(owner == null ? "" : owner));
                    Lock lock = lockFromString(lockString);
                    if (lock != null && owner != null
                            && !owner.equals(lock.getOwner())) {
                        lock = new Lock(lock, true); // failed removal
                    }
                    return lock;
                }
            });
        } catch (IOException cause) {
            throw new RuntimeException("Lock write error on " + id, cause);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void clearCaches() {
    }

}
