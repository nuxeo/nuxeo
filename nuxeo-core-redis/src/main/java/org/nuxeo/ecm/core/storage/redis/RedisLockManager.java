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
package org.nuxeo.ecm.core.storage.redis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisConfiguration;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.storage.lock.AbstractLockManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Redis-based lock manager.
 *
 * @since 5.9.6
 */
public class RedisLockManager extends AbstractLockManager {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(RedisLockManager.class);

    /**
     * Prefix for keys, added after the globally configured prefix of the
     * {@link RedisService}.
     */
    public static final String PREFIX = "lock:";

    protected final String redisPrefix;

    private static final String SCRIPT_SET = "" //
            + "local v = redis.call('GET', KEYS[1]) " //
            + "if v == false then " //
            + "  redis.call('SET', KEYS[1], ARGV[1]) " //
            + "end " //
            + "return v " //
            + "";

    private static final String SCRIPT_REMOVE = "" //
            + "local v = redis.call('GET', KEYS[1]) " //
            + "if v == false then " //
            + "  return nil " //
            + "else " //
            // unconditional remove if empty owner
            + "  if ARGV[1] == '' then " //
            + "    redis.call('DEL', KEYS[1]) " //
            + "  else " //
            // check lock owner
            + "    local i = string.find(v, ':') " //
            + "    if i == nil then " //
            + "      return v " // error will be seen by caller
            + "    end " //
            // remove if same owner
            + "    if ARGV[1] == string.sub(v, 1, i-1) then " //
            + "      redis.call('DEL', KEYS[1]) " //
            + "    end " //
            + "  end " //
            + "  return v " //
            + "end " //
            + "";

    protected final String repositoryName;

    protected RedisExecutor redisExecutor;

    protected String prefix;

    protected String scriptSetSha;

    protected String scriptRemoveSha;

    /**
     * Creates a lock manager for the given repository.
     * <p>
     * {@link #close} must be called when done with the lock manager.
     */
    public RedisLockManager(String repositoryName) {
        this.repositoryName = repositoryName;
        redisExecutor = Framework.getService(RedisExecutor.class);
        redisPrefix = Framework.getService(RedisConfiguration.class) + PREFIX
                + repositoryName + ':';
        init();
    }

    protected void init() {
        try {
            redisExecutor.execute(new RedisCallable<Void>() {

                @Override
                public Void call() {
                    scriptSetSha = jedis.scriptLoad(SCRIPT_SET);
                    scriptRemoveSha = jedis.scriptLoad(SCRIPT_REMOVE);
                    return null;
                }
            });
        } catch (Exception cause) {
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
                    + lockString + ", should be " + redisPrefix + "<id>");
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
                public Lock call() {
                    String lockString = jedis.get(redisPrefix + id);
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
                public Lock call() {
                    String lockString = (String) jedis.evalsha(scriptSetSha,
                            Arrays.asList(redisPrefix + id),
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
                public Lock call() {
                    String lockString = (String) jedis.evalsha(scriptRemoveSha,
                            Arrays.asList(redisPrefix + id),
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
