/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.redis.contribs;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;

/**
 * Redis-based lock manager.
 *
 * @since 6.0
 */
public class RedisLockManager implements LockManager {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(RedisLockManager.class);

    protected final String redisNamespace;

    protected final String repositoryName;

    protected String prefix;

    protected String scriptSetSha;

    protected String scriptRemoveSha;

    /**
     * Creates a lock manager for the given repository.
     * <p>
     * {@link #closeLockManager()} must be called when done with the lock manager.
     */
    public RedisLockManager(String repositoryName) {
        this.repositoryName = repositoryName;
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        redisNamespace = redisAdmin.namespace("lock", repositoryName);
        try {
            scriptSetSha = redisAdmin.load("org.nuxeo.ecm.core.redis", "set-lock");
            scriptRemoveSha = redisAdmin.load("org.nuxeo.ecm.core.redis", "remove-lock");
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
            throw new IllegalArgumentException(
                    "Invalid Redis lock : " + lockString + ", should be " + redisNamespace + "<id>");
        }
        Calendar created = Calendar.getInstance();
        created.setTimeInMillis(Long.parseLong(split[1]));
        return new Lock(split[0], created);
    }

    @Override
    public Lock getLock(final String id) {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        return redisExecutor.execute(new RedisCallable<Lock>() {
            @Override
            public Lock call(Jedis jedis) {
                String lockString = jedis.get(redisNamespace + id);
                return lockFromString(lockString);
            }
        });
    }

    @Override
    public Lock setLock(final String id, final Lock lock) {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        List<String> keys = Collections.singletonList(redisNamespace + id);
        List<String> args = Collections.singletonList(stringFromLock(lock));
        String lockString = (String) redisExecutor.evalsha(scriptSetSha, keys, args);
        return lockFromString(lockString); // existing lock
    }

    @Override
    public Lock removeLock(final String id, final String owner) {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        List<String> keys = Collections.singletonList(redisNamespace + id);
        List<String> args = Collections.singletonList(owner == null ? "" : owner);
        String lockString = (String) redisExecutor.evalsha(scriptRemoveSha, keys, args);
        Lock lock = lockFromString(lockString);
        if (lock != null && owner != null && !owner.equals(lock.getOwner())) {
            lock = new Lock(lock, true); // failed removal
        }
        return lock;
    }

    @Override
    public void closeLockManager() {
    }

    @Override
    public void clearLockManagerCaches() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + repositoryName + ')';
    }

}
