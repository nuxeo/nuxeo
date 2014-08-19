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

import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.redis.RedisService;
import org.nuxeo.ecm.core.storage.lock.AbstractLockManager;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis-based lock manager.
 *
 * @since 5.9.6
 */
public class RedisLockManager extends AbstractLockManager {

    private static final Log log = LogFactory.getLog(RedisLockManager.class);

    /**
     * Prefix for keys, added after the globally configured prefix of the
     * {@link RedisService}.
     */
    public static final String PREFIX = "lock:";

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

    protected RedisService redisService;

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
    }

    protected RedisService getRedisService() {
        if (redisService == null) {
            redisService = Framework.getService(RedisService.class);
            prefix = redisService.getPrefix() + PREFIX + repositoryName + ':';
            init();
        }
        return redisService;
    }

    protected void init() {
        Jedis jedis = getJedis();
        try {
            scriptSetSha = jedis.scriptLoad(SCRIPT_SET);
            scriptRemoveSha = jedis.scriptLoad(SCRIPT_REMOVE);
        } finally {
            closeJedis(jedis);
        }
    }

    protected Jedis getJedis() {
        RedisService redisService = getRedisService();
        if (redisService == null) {
            return null;
        }
        JedisPool jedisPool = redisService.getJedisPool();
        if (jedisPool == null) {
            return null;
        }
        return jedisPool.getResource();
    }

    protected void closeJedis(Jedis jedis) {
        getRedisService().getJedisPool().returnResource(jedis);
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
            log.warn("Invalid Redis lock : " + lockString);
            return null;
        }
        Calendar created = Calendar.getInstance();
        created.setTimeInMillis(Long.parseLong(split[1]));
        return new Lock(split[0], created);
    }

    @Override
    public Lock getLock(String id) {
        Jedis jedis = getJedis();
        try {
            String lockString = jedis.get(prefix + id);
            return lockFromString(lockString);
        } finally {
            closeJedis(jedis);
        }
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        Jedis jedis = getJedis();
        try {
            String lockString = (String) jedis.evalsha(scriptSetSha,
                    Arrays.asList(prefix + id),
                    Arrays.asList(stringFromLock(lock)));
            return lockFromString(lockString); // existing lock
        } finally {
            closeJedis(jedis);
        }
    }

    @Override
    public Lock removeLock(String id, String owner) {
        Jedis jedis = getJedis();
        try {
            String lockString = (String) jedis.evalsha(scriptRemoveSha,
                    Arrays.asList(prefix + id),
                    Arrays.asList(owner == null ? "" : owner));
            Lock lock = lockFromString(lockString);
            if (lock != null && owner != null && !owner.equals(lock.getOwner())) {
                lock = new Lock(lock, true); // failed removal
            }
            return lock;
        } finally {
            closeJedis(jedis);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void clearCaches() {
    }

}
