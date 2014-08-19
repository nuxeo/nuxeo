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
package org.nuxeo.ecm.core.work.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.redis.RedisService;
import org.nuxeo.ecm.core.work.WorkManagerImpl;
import org.nuxeo.ecm.core.work.WorkQueueDescriptorRegistry;
import org.nuxeo.ecm.core.work.WorkQueuing;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Implementation of a {@link WorkQueuing} storing {@link Work} instances in
 * Redis.
 *
 * @since 5.8
 */
public class RedisWorkQueuing implements WorkQueuing {

    private static final Log log = LogFactory.getLog(RedisWorkQueuing.class);

    /**
     * Prefix for keys, added after the globally configured prefix of the
     * {@link RedisService}.
     */
    public static final String PREFIX = "work:";

    protected static final String UTF_8 = "UTF-8";

    /**
     * Global hash of Work instance id -> serialized Work instance.
     */
    protected static final String KEY_DATA = "data";

    /**
     * Global hash of Work instance id -> Work state. The completed state (
     * {@value #STATE_COMPLETED_B}) is followed by a completion time in
     * milliseconds.
     */
    protected static final String KEY_STATE = "state";

    /**
     * Per-queue list of suspended Work instance ids.
     */
    protected static final String KEY_SUSPENDED_PREFIX = "prev:";

    /**
     * Per-queue list of scheduled Work instance ids.
     */
    protected static final String KEY_SCHEDULED_PREFIX = "queue:";

    /**
     * Per-queue set of running Work instance ids.
     */
    protected static final String KEY_RUNNING_PREFIX = "run:";

    /**
     * Per-queue set of completed Work instance ids.
     */
    protected static final String KEY_COMPLETED_PREFIX = "done:";

    protected static final byte STATE_SCHEDULED_B = 'Q';

    protected static final byte STATE_CANCELED_B = 'X';

    protected static final byte STATE_RUNNING_B = 'R';

    protected static final byte STATE_COMPLETED_B = 'C';

    protected static final byte[] STATE_SCHEDULED = new byte[] { STATE_SCHEDULED_B };

    protected static final byte[] STATE_CANCELED = new byte[] { STATE_CANCELED_B };

    protected static final byte[] STATE_RUNNING = new byte[] { STATE_RUNNING_B };

    protected static final byte[] STATE_COMPLETED = new byte[] { STATE_COMPLETED_B };

    protected final WorkManagerImpl mgr;

    // @GuardedBy("this")
    protected Map<String, BlockingQueue<Runnable>> allScheduled = new HashMap<String, BlockingQueue<Runnable>>();

    protected RedisService redisService;

    protected String redisPrefix;

    public RedisWorkQueuing(WorkManagerImpl mgr, WorkQueueDescriptorRegistry workQueueDescriptors) {
        this.mgr = mgr;
    }

    @Override
    public void init() {
        try {
            for (String queueId : getSuspendedQueueIds()) {
                int n = scheduleSuspendedWork(queueId);
                log.info("Re-scheduling " + n
                        + " work instances suspended from queue: " + queueId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized BlockingQueue<Runnable> getScheduledQueue(String queueId) {
        BlockingQueue<Runnable> scheduled = allScheduled.get(queueId);
        if (scheduled == null) {
            allScheduled.put(queueId, scheduled = newBlockingQueue(queueId));
        }
        return scheduled;
    }

    @Override
    public void workRunning(String queueId, Work work) {
        try {
            workSetRunning(queueId, work);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void workCompleted(String queueId, Work work) {
        try {
            workSetCompleted(queueId, work);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BlockingQueue<Runnable> newBlockingQueue(String queueId) {
        return new RedisBlockingQueue(queueId, this);
    }

    @Override
    public List<Work> listWork(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return listScheduled(queueId);
        case RUNNING:
            return listRunning(queueId);
        case COMPLETED:
            return listCompleted(queueId);
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public List<String> listWorkIds(String queueId, State state) {
        if (state == null) {
            return listNonCompletedIds(queueId);
        }
        switch (state) {
        case SCHEDULED:
            return listScheduledIds(queueId);
        case RUNNING:
            return listRunningIds(queueId);
        case COMPLETED:
            return listCompletedIds(queueId);
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    protected List<Work> listScheduled(String queueId) {
        try {
            return listWorkList(scheduledKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Work> listRunning(String queueId) {
        try {
            return listWorkSet(runningKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Work> listCompleted(String queueId) {
        try {
            return listWorkSet(completedKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> listScheduledIds(String queueId) {
        try {
            return listWorkIdsList(scheduledKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> listRunningIds(String queueId) {
        try {
            return listWorkIdsSet(runningKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> listNonCompletedIds(String queueId) {
        List<String> list = listScheduledIds(queueId);
        list.addAll(listRunningIds(queueId));
        return list;
    }

    protected List<String> listCompletedIds(String queueId) {
        try {
            return listWorkIdsSet(completedKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getQueueSize(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return getScheduledSize(queueId);
        case RUNNING:
            return getRunningSize(queueId);
        case COMPLETED:
            return getCompletedSize(queueId);
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    protected int getScheduledSize(String queueId) {
        try {
            return getScheduledQueueSize(queueId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getRunningSize(String queueId) {
        try {
            return getRunningQueueSize(queueId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getCompletedSize(String queueId) {
        try {
            return getCompletedQueueSize(queueId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Work find(String workId, State state) {
        if (isWorkInState(workId, state)) {
            return getWork(bytes(workId));
        }
        return null;
    }

    @Override
    public boolean isWorkInState(String workId, State state) {
        State s = getWorkState(workId);
        if (state == null) {
            return s == State.SCHEDULED || s == State.RUNNING;
        }
        return s == state;
    }

    @Override
    public Work removeScheduled(String queueId, String workId) {
        try {
            return removeScheduledWork(queueId, workId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public State getWorkState(String workId) {
        try {
            return getWorkStateInfo(workId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int setSuspending(String queueId) {
        try {
            int n = suspendScheduledWork(queueId);
            log.info("Suspending " + n + " work instances from queue: "
                    + queueId);
            return n;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearCompletedWork(String queueId, long completionTime) {
        try {
            if (completionTime <= 0) {
                removeAllCompletedWork(queueId);
            } else {
                removeCompletedWork(queueId, completionTime);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * ******************** Redis Interface ********************
     */

    protected RedisService getRedisService() {
        if (redisService == null) {
            redisService = Framework.getLocalService(RedisService.class);
            redisPrefix = redisService.getPrefix() + PREFIX;
        }
        return redisService;
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

    protected static String string(byte[] bytes) {
        try {
            return new String(bytes, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] bytes(String string) {
        try {
            return string.getBytes(UTF_8);
        } catch (IOException e) {
            // cannot happen for UTF-8
            throw new RuntimeException(e);
        }
    }

    protected byte[] keyBytes(String prefix, String queueId) {
        return bytes(redisPrefix + prefix + queueId);
    }

    protected byte[] keyBytes(String prefix) {
        return bytes(redisPrefix + prefix);
    }

    protected byte[] suspendedKey(String queueId) {
        return keyBytes(KEY_SUSPENDED_PREFIX, queueId);
    }

    protected byte[] scheduledKey(String queueId) {
        return keyBytes(KEY_SCHEDULED_PREFIX, queueId);
    }

    protected byte[] runningKey(String queueId) {
        return keyBytes(KEY_RUNNING_PREFIX, queueId);
    }

    protected byte[] completedKey(String queueId) {
        return keyBytes(KEY_COMPLETED_PREFIX, queueId);
    }

    protected byte[] stateKey() {
        return keyBytes(KEY_STATE);
    }

    protected byte[] dataKey() {
        return keyBytes(KEY_DATA);
    }

    protected byte[] serializeWork(Work work) throws IOException {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baout);
        out.writeObject(work);
        out.flush();
        out.close();
        return baout.toByteArray();
    }

    protected Work deserializeWork(byte[] workBytes) throws IOException {
        if (workBytes == null) {
            return null;
        }
        InputStream bain = new ByteArrayInputStream(workBytes);
        ObjectInputStream in = new ObjectInputStream(bain);
        try {
            return (Work) in.readObject();
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getScheduledQueueSize(String queueId) throws IOException {
        Jedis jedis = getJedis();
        try {
            Long len = jedis.llen(scheduledKey(queueId));
            return len.intValue();
        } finally {
            closeJedis(jedis);
        }
    }

    protected int getRunningQueueSize(String queueId) throws IOException {
        Jedis jedis = getJedis();
        try {
            Long len = jedis.scard(runningKey(queueId));
            return len.intValue();
        } finally {
            closeJedis(jedis);
        }
    }

    protected int getCompletedQueueSize(String queueId) throws IOException {
        Jedis jedis = getJedis();
        try {
            Long len = jedis.scard(completedKey(queueId));
            return len.intValue();
        } finally {
            closeJedis(jedis);
        }
    }

    protected void storeWork(byte[] workIdBytes, byte[] workBytes, Jedis jedis) {
        // store data
        jedis.hset(dataKey(), workIdBytes, workBytes);
        // set state to scheduled
        jedis.hset(stateKey(), workIdBytes, STATE_SCHEDULED);
    }

    protected void removeWork(byte[] workIdBytes, Jedis jedis) {
        // remove state
        jedis.hdel(stateKey(), workIdBytes);
        // remove data
        jedis.hdel(dataKey(), workIdBytes);
    }

    /**
     * Persists a work instance and adds it to the scheduled queue.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @throws IOException
     */
    public void addScheduledWork(String queueId, Work work) throws IOException {
        log.debug("Add scheduled " + work);
        byte[] workIdBytes = bytes(work.getId());

        // serialize Work
        byte[] workBytes = serializeWork(work);

        Jedis jedis = getJedis();
        try {
            storeWork(workIdBytes, workBytes, jedis);
            // put key in scheduled queue
            jedis.lpush(scheduledKey(queueId), workIdBytes);
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Finds which queues have suspended work.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    protected Set<String> getSuspendedQueueIds() throws IOException {
        return getQueueIds(KEY_SUSPENDED_PREFIX);
    }

    protected Set<String> getScheduledQueueIds() {
        try {
            return getQueueIds(KEY_SCHEDULED_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Set<String> getRunningQueueIds() {
        try {
            return getQueueIds(KEY_RUNNING_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getCompletedQueueIds() {
        try {
            return getQueueIds(KEY_COMPLETED_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds which queues have work for a given state prefix.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    protected Set<String> getQueueIds(String prefix) throws IOException {
        Jedis jedis = getJedis();
        try {
            int offset = keyBytes(prefix).length;
            Set<byte[]> keys = jedis.keys(keyBytes(prefix, "*"));
            Set<String> queueIds = new HashSet<String>(keys.size());
            for (byte[] bytes : keys) {
                String queueId = new String(bytes, offset, bytes.length
                        - offset, UTF_8);
                queueIds.add(queueId);
            }
            return queueIds;
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Resumes all suspended work instances by moving them to the scheduled
     * queue.
     *
     * @param queueId the queue id
     * @return the number of work instances scheduled
     * @since 5.8
     */
    public int scheduleSuspendedWork(String queueId) throws IOException {
        Jedis jedis = getJedis();
        try {
            for (int n = 0;; n++) {
                byte[] workIdBytes = jedis.rpoplpush(suspendedKey(queueId),
                        scheduledKey(queueId));
                if (workIdBytes == null) {
                    return n;
                }
            }
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Suspends all scheduled work instances by moving them to the suspended
     * queue.
     *
     * @param queueId the queue id
     * @return the number of work instances suspended
     * @since 5.8
     */
    public int suspendScheduledWork(String queueId) throws IOException {
        Jedis jedis = getJedis();
        try {
            for (int n = 0;; n++) {
                byte[] workIdBytes = jedis.rpoplpush(scheduledKey(queueId),
                        suspendedKey(queueId));
                if (workIdBytes == null) {
                    return n;
                }
            }
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Switches a work to state running.
     *
     * @param queueId the queue id
     * @param workId the work id
     */
    protected void workSetRunning(String queueId, Work work) throws IOException {
        byte[] workIdBytes = bytes(work.getId());
        Jedis jedis = getJedis();
        try {
            // put key in running set
            jedis.sadd(runningKey(queueId), workIdBytes);
            // set state to running
            jedis.hset(stateKey(), workIdBytes, STATE_RUNNING);
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Switches a work to state completed, and saves its new state.
     *
     * @param queueId
     * @param id
     * @throws IOException
     */
    protected void workSetCompleted(String queueId, Work work)
            throws IOException {
        byte[] workIdBytes = bytes(work.getId());
        byte[] workBytes = serializeWork(work);
        Jedis jedis = getJedis();
        try {
            // store (updated) content in hash
            jedis.hset(dataKey(), workIdBytes, workBytes);
            // remove key from running set
            jedis.srem(runningKey(queueId), workIdBytes);
            // put key in completed set
            jedis.sadd(completedKey(queueId), workIdBytes);
            // set state to completed
            byte[] completedBytes = bytes(((char) STATE_COMPLETED_B)
                    + String.valueOf(work.getCompletionTime()));
            jedis.hset(stateKey(), workIdBytes, completedBytes);
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Gets the work state.
     *
     * @param workId the work id
     * @return the state, or {@code null} if not found
     */
    protected State getWorkStateInfo(String workId) throws IOException {
        byte[] workIdBytes = bytes(workId);
        Jedis jedis = getJedis();
        try {
            // get state
            byte[] bytes = jedis.hget(stateKey(), workIdBytes);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            switch (bytes[0]) {
            case STATE_SCHEDULED_B:
                return State.SCHEDULED;
            case STATE_CANCELED_B:
                return State.CANCELED;
            case STATE_RUNNING_B:
                return State.RUNNING;
            case STATE_COMPLETED_B:
                return State.COMPLETED;
            default:
                log.error("Unknown work state: " + new String(bytes, UTF_8)
                        + ", work: " + workId);
                return null;
            }
        } finally {
            closeJedis(jedis);
        }
    }

    protected List<String> listWorkIdsList(byte[] queueBytes) throws IOException {
        Jedis jedis = getJedis();
        try {
            List<byte[]> keys = jedis.lrange(queueBytes, 0, -1);
            List<String> list = new ArrayList<String>(keys.size());
            for (byte[] workIdBytes : keys) {
                list.add(string(workIdBytes));
            }
            return list;
        } finally {
            closeJedis(jedis);
        }
    }

    protected List<String> listWorkIdsSet(byte[] queueBytes) throws IOException {
        Jedis jedis = getJedis();
        try {
            Set<byte[]> keys = jedis.smembers(queueBytes);
            List<String> list = new ArrayList<String>(keys.size());
            for (byte[] workIdBytes : keys) {
                list.add(string(workIdBytes));
            }
            return list;
        } finally {
            closeJedis(jedis);
        }
    }

    protected List<Work> listWorkList(byte[] queueBytes) throws IOException {
        Jedis jedis = getJedis();
        try {
            List<byte[]> keys = jedis.lrange(queueBytes, 0, -1);
            List<Work> list = new ArrayList<Work>(keys.size());
            for (byte[] workIdBytes : keys) {
                // get data
                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                Work work = deserializeWork(workBytes);
                list.add(work);
            }
            return list;
        } finally {
            closeJedis(jedis);
        }
    }

    protected List<Work> listWorkSet(byte[] queueBytes) throws IOException {
        Jedis jedis = getJedis();
        try {
            Set<byte[]> keys = jedis.smembers(queueBytes);
            List<Work> list = new ArrayList<Work>(keys.size());
            for (byte[] workIdBytes : keys) {
                // get data
                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                Work work = deserializeWork(workBytes);
                list.add(work);
            }
            return list;
        } finally {
            closeJedis(jedis);
        }
    }

    protected Work getWork(byte[] workIdBytes) {
        try {
            return getWorkData(workIdBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Work getWorkData(byte[] workIdBytes) throws IOException {
        Jedis jedis = getJedis();
        try {
            byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
            return deserializeWork(workBytes);
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Removes first work from scheduled queue.
     *
     * @param queueId the queue id
     * @return the work, or {@code null} if the scheduled queue is empty
     */
    protected Work removeScheduledWork(String queueId) throws IOException {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return null;
        }
        try {
            // pop from queue
            byte[] workIdBytes = jedis.rpop(scheduledKey(queueId));
            if (workIdBytes == null) {
                return null;
            }
            // get data
            byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
            return deserializeWork(workBytes);
        } finally {
            closeJedis(jedis);
        }
    }

    /**
     * Removes a given work from scheduled queue and set state to completed.
     */
    protected Work removeScheduledWork(String queueId, String workId)
            throws IOException {
        byte[] workIdBytes = bytes(workId);
        Jedis jedis = getJedis();
        try {
            // remove from queue
            Long n = jedis.lrem(scheduledKey(queueId), 0, workIdBytes);
            if (n == null || n.intValue() == 0) {
                return null;
            }
            // set state to completed at current time
            byte[] completedBytes = bytes(String.valueOf(System.currentTimeMillis()));
            jedis.hset(stateKey(), workIdBytes, completedBytes);
            // get data
            byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
            return deserializeWork(workBytes);

        } finally {
            closeJedis(jedis);
        }
    }

    protected void removeAllCompletedWork(String queueId) throws IOException {
        Jedis jedis = getJedis();
        try {
            for (;;) {
                byte[] workIdBytes = jedis.spop(completedKey(queueId));
                if (workIdBytes == null) {
                    return;
                }
                removeWork(workIdBytes, jedis);
            }
        } finally {
            closeJedis(jedis);
        }
    }

    protected void removeCompletedWork(String queueId, long completionTime)
            throws IOException {
        Jedis jedis = getJedis();
        try {
            Set<byte[]> keys = jedis.smembers(completedKey(queueId));
            for (byte[] workIdBytes : keys) {
                // state is a completion time
                byte[] bytes = jedis.hget(stateKey(), workIdBytes);
                if (bytes == null || bytes.length == 0
                        || bytes[0] != STATE_COMPLETED_B) {
                    continue;
                }
                long t = Long.parseLong(new String(bytes, 1, bytes.length - 1,
                        UTF_8));
                if (t < completionTime) {
                    jedis.srem(completedKey(queueId), workIdBytes);
                    removeWork(workIdBytes, jedis);
                }
            }
        } finally {
            closeJedis(jedis);
        }
    }

}
