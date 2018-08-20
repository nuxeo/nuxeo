/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.work.NuxeoBlockingQueue;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.WorkQueuing;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Implementation of a {@link WorkQueuing} storing {@link Work} instances in Redis.
 *
 * @since 5.8
 */
public class RedisWorkQueuing implements WorkQueuing {

    private static final Log log = LogFactory.getLog(RedisWorkQueuing.class);

    protected static final String UTF_8 = "UTF-8";

    /**
     * Global hash of Work instance id -> serialoized Work instance.
     */
    protected static final String KEY_DATA = "data";

    /**
     * Global hash of Work instance id -> Work state. The completed state ( {@value #STATE_COMPLETED_B}) is followed by
     * a completion time in milliseconds.
     */
    protected static final String KEY_STATE = "state";

    /**
     * Per-queue list of suspended Work instance ids.
     */
    protected static final String KEY_SUSPENDED_PREFIX = "prev";

    protected static final byte[] KEY_SUSPENDED = KEY_SUSPENDED_PREFIX.getBytes();

    /**
     * Per-queue list of scheduled Work instance ids.
     */
    protected static final String KEY_QUEUE_PREFIX = "queue";

    protected static final byte[] KEY_QUEUE = KEY_QUEUE_PREFIX.getBytes();

    /**
     * Per-queue set of scheduled Work instance ids.
     */
    protected static final String KEY_SCHEDULED_PREFIX = "sched";

    protected static final byte[] KEY_SCHEDULED = KEY_SCHEDULED_PREFIX.getBytes();

    /**
     * Per-queue set of running Work instance ids.
     */
    protected static final String KEY_RUNNING_PREFIX = "run";

    protected static final byte[] KEY_RUNNING = KEY_RUNNING_PREFIX.getBytes();

    /**
     * Per-queue set of counters.
     */
    protected static final String KEY_COMPLETED_PREFIX = "done";

    protected static final byte[] KEY_COMPLETED = KEY_COMPLETED_PREFIX.getBytes();

    protected static final String KEY_CANCELED_PREFIX = "cancel";

    protected static final byte[] KEY_CANCELED = KEY_CANCELED_PREFIX.getBytes();

    protected static final String KEY_COUNT_PREFIX = "count";

    protected static final byte STATE_SCHEDULED_B = 'Q';

    protected static final byte STATE_RUNNING_B = 'R';

    protected static final byte STATE_RUNNING_C = 'C';

    protected static final byte[] STATE_SCHEDULED = new byte[] { STATE_SCHEDULED_B };

    protected static final byte[] STATE_RUNNING = new byte[] { STATE_RUNNING_B };

    protected static final byte[] STATE_UNKNOWN = new byte[0];

    protected Listener listener;

    protected final Map<String, NuxeoBlockingQueue> allQueued = new HashMap<>();

    protected String redisNamespace;

    // lua scripts
    protected byte[] initWorkQueueSha;

    protected byte[] metricsWorkQueueSha;

    protected byte[] schedulingWorkSha;

    protected byte[] popWorkSha;

    protected byte[] runningWorkSha;

    protected byte[] cancelledScheduledWorkSha;

    protected byte[] completedWorkSha;

    protected byte[] cancelledRunningWorkSha;

    public RedisWorkQueuing(Listener listener) {
        this.listener = listener;
        loadConfig();
    }

    void loadConfig() {
        RedisAdmin admin = Framework.getService(RedisAdmin.class);
        redisNamespace = admin.namespace("work");
        try {
            initWorkQueueSha = admin.load("org.nuxeo.ecm.core.redis", "init-work-queue").getBytes();
            metricsWorkQueueSha = admin.load("org.nuxeo.ecm.core.redis", "metrics-work-queue").getBytes();
            schedulingWorkSha = admin.load("org.nuxeo.ecm.core.redis", "scheduling-work").getBytes();
            popWorkSha = admin.load("org.nuxeo.ecm.core.redis", "pop-work").getBytes();
            runningWorkSha = admin.load("org.nuxeo.ecm.core.redis", "running-work").getBytes();
            cancelledScheduledWorkSha = admin.load("org.nuxeo.ecm.core.redis", "cancelled-scheduled-work").getBytes();
            completedWorkSha = admin.load("org.nuxeo.ecm.core.redis", "completed-work").getBytes();
            cancelledRunningWorkSha = admin.load("org.nuxeo.ecm.core.redis", "cancelled-running-work").getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load LUA scripts", e);
        }
    }

    @Override
    public NuxeoBlockingQueue init(WorkQueueDescriptor config) {
        evalSha(initWorkQueueSha, keys(config.id), Collections.emptyList());
        RedisBlockingQueue queue = new RedisBlockingQueue(config.id, this);
        allQueued.put(config.id, queue);
        return queue;
    }

    @Override
    public NuxeoBlockingQueue getQueue(String queueId) {
        return allQueued.get(queueId);
    }

    @Override
    public void workSchedule(String queueId, Work work) {
        getQueue(queueId).offer(new WorkHolder(work));
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
    public void workCanceled(String queueId, Work work) {
        try {
            workSetCancelledScheduled(queueId, work);
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

    @Override
    public void workReschedule(String queueId, Work work) {
        try {
            workSetReschedule(queueId, work);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Work> listWork(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return listScheduled(queueId);
        case RUNNING:
            return listRunning(queueId);
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
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    protected List<Work> listScheduled(String queueId) {
        try {
            return listWorkList(queuedKey(queueId));
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

    protected List<String> listScheduledIds(String queueId) {
        try {
            return listWorkIdsList(queuedKey(queueId));
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

    @Override
    public long count(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return metrics(queueId).scheduled.longValue();
        case RUNNING:
            return metrics(queueId).running.longValue();
        default:
            throw new IllegalArgumentException(String.valueOf(state));
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
    public void removeScheduled(String queueId, String workId) {
        try {
            removeScheduledWork(queueId, workId);
        } catch (IOException cause) {
            throw new RuntimeException("Cannot remove scheduled work " + workId + " from " + queueId, cause);
        }
    }

    @Override
    public State getWorkState(String workId) {
        return getWorkStateInfo(workId);
    }

    @Override
    public void setActive(String queueId, boolean value) {
        WorkQueueMetrics metrics = getQueue(queueId).setActive(value);
        if (value) {
            listener.queueActivated(metrics);
        } else {
            listener.queueDeactivated(metrics);
        }
    }

    /*
     * ******************** Redis Interface ********************
     */

    protected String string(byte[] bytes) {
        try {
            return new String(bytes, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Should not happen, cannot decode string in UTF-8", e);
        }
    }

    protected byte[] bytes(String string) {
        try {
            return string.getBytes(UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Should not happen, cannot encode string in UTF-8", e);
        }
    }

    protected byte[] bytes(Work.State state) {
        switch (state) {
        case SCHEDULED:
            return STATE_SCHEDULED;
        case RUNNING:
            return STATE_RUNNING;
        default:
            return STATE_UNKNOWN;
        }
    }

    protected static String key(String... names) {
        return String.join(":", names);
    }

    protected byte[] keyBytes(String prefix, String queueId) {
        return keyBytes(key(prefix, queueId));
    }

    protected byte[] keyBytes(String prefix) {
        return bytes(redisNamespace.concat(prefix));
    }

    protected byte[] workId(Work work) {
        return workId(work.getId());
    }

    protected byte[] workId(String id) {
        return bytes(id);
    }

    protected byte[] suspendedKey(String queueId) {
        return keyBytes(key(KEY_SUSPENDED_PREFIX, queueId));
    }

    protected byte[] queuedKey(String queueId) {
        return keyBytes(key(KEY_QUEUE_PREFIX, queueId));
    }

    protected byte[] countKey(String queueId) {
        return keyBytes(key(KEY_COUNT_PREFIX, queueId));
    }

    protected byte[] runningKey(String queueId) {
        return keyBytes(key(KEY_RUNNING_PREFIX, queueId));
    }

    protected byte[] scheduledKey(String queueId) {
        return keyBytes(key(KEY_SCHEDULED_PREFIX, queueId));
    }

    protected byte[] completedKey(String queueId) {
        return keyBytes(key(KEY_COMPLETED_PREFIX, queueId));
    }

    protected byte[] canceledKey(String queueId) {
        return keyBytes(key(KEY_CANCELED_PREFIX, queueId));
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

    protected Work deserializeWork(byte[] workBytes) {
        if (workBytes == null) {
            return null;
        }
        InputStream bain = new ByteArrayInputStream(workBytes);
        try (ObjectInputStream in = new ObjectInputStream(bain)) {
            return (Work) in.readObject();
        } catch (RuntimeException cause) {
            throw cause;
        } catch (IOException | ClassNotFoundException cause) {
            throw new RuntimeException("Cannot deserialize work", cause);
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
        return getQueueIds(KEY_QUEUE_PREFIX);
    }

    protected Set<String> getRunningQueueIds() {
        return getQueueIds(KEY_RUNNING_PREFIX);
    }

    /**
     * Finds which queues have work for a given state prefix.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    protected Set<String> getQueueIds(final String queuePrefix) {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<Set<String>>() {
            @Override
            public Set<String> call(Jedis jedis) {
                int offset = keyBytes(queuePrefix).length;
                Set<byte[]> keys = jedis.keys(keyBytes(key(queuePrefix, "*")));
                Set<String> queueIds = new HashSet<String>(keys.size());
                for (byte[] bytes : keys) {
                    try {
                        String queueId = new String(bytes, offset, bytes.length - offset, UTF_8);
                        queueIds.add(queueId);
                    } catch (IOException e) {
                        throw new NuxeoException(e);
                    }
                }
                return queueIds;
            }

        });
    }

    /**
     * Resumes all suspended work instances by moving them to the scheduled queue.
     *
     * @param queueId the queue id
     * @return the number of work instances scheduled
     * @since 5.8
     */
    public int scheduleSuspendedWork(final String queueId) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<Integer>() {
            @Override
            public Integer call(Jedis jedis) {
                for (int n = 0;; n++) {
                    byte[] workIdBytes = jedis.rpoplpush(suspendedKey(queueId), queuedKey(queueId));
                    if (workIdBytes == null) {
                        return Integer.valueOf(n);
                    }
                }
            }

        }).intValue();
    }

    /**
     * Suspends all scheduled work instances by moving them to the suspended queue.
     *
     * @param queueId the queue id
     * @return the number of work instances suspended
     * @since 5.8
     */
    public int suspendScheduledWork(final String queueId) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<Integer>() {

            @Override
            public Integer call(Jedis jedis) {
                for (int n = 0;; n++) {
                    byte[] workIdBytes = jedis.rpoplpush(queuedKey(queueId), suspendedKey(queueId));
                    if (workIdBytes == null) {
                        return n;
                    }
                }
            }
        }).intValue();
    }

    @Override
    public WorkQueueMetrics metrics(String queueId) {
        return metrics(queueId, evalSha(metricsWorkQueueSha, keys(queueId), Collections.emptyList()));
    }

    WorkQueueMetrics metrics(String queueId, Number[] counters) {
        return new WorkQueueMetrics(queueId, counters[0], counters[1], counters[2], counters[3]);
    }

    /**
     * Persists a work instance and adds it to the scheduled queue.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @throws IOException
     */
    public void workSetScheduled(final String queueId, Work work) throws IOException {
        listener.queueChanged(work, metrics(queueId, evalSha(schedulingWorkSha, keys(queueId), args(work, true))));
    }

    /**
     * Switches a work to state completed, and saves its new state.
     */
    protected void workSetCancelledScheduled(final String queueId, final Work work) throws IOException {
        listener.queueChanged(work,
                metrics(queueId, evalSha(cancelledScheduledWorkSha, keys(queueId), args(work, true))));
    }

    /**
     * Switches a work to state running.
     *
     * @param queueId the queue id
     * @param work the work
     */
    protected void workSetRunning(final String queueId, Work work) throws IOException {
        listener.queueChanged(work, metrics(queueId, evalSha(runningWorkSha, keys(queueId), args(work, true))));
    }

    /**
     * Switches a work to state completed, and saves its new state.
     */
    protected void workSetCompleted(final String queueId, final Work work) throws IOException {
        listener.queueChanged(work, metrics(queueId, evalSha(completedWorkSha, keys(queueId), args(work, false))));
    }

    /**
     * Switches a work to state canceled, and saves its new state.
     */
    protected void workSetReschedule(final String queueId, final Work work) throws IOException {
        listener.queueChanged(work,
                metrics(queueId, evalSha(cancelledRunningWorkSha, keys(queueId), args(work, true))));
    }

    protected List<byte[]> keys(String queueid) {
        return Arrays.asList(dataKey(), stateKey(), countKey(queueid), scheduledKey(queueid), queuedKey(queueid),
                runningKey(queueid), completedKey(queueid), canceledKey(queueid));
    }

    protected List<byte[]> args(String workId) throws IOException {
        return Arrays.asList(workId(workId));
    }

    protected List<byte[]> args(Work work, boolean serialize) throws IOException {
        List<byte[]> args = Arrays.asList(workId(work), bytes(work.getWorkInstanceState()));
        if (serialize) {
            args = new ArrayList<>(args);
            args.add(serializeWork(work));
        }
        return args;
    }

    /**
     * Gets the work state.
     *
     * @param workId the work id
     * @return the state, or {@code null} if not found
     */
    protected State getWorkStateInfo(final String workId) {
        final byte[] workIdBytes = bytes(workId);
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<State>() {
            @Override
            public State call(Jedis jedis) {
                // get state
                byte[] bytes = jedis.hget(stateKey(), workIdBytes);
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                switch (bytes[0]) {
                case STATE_SCHEDULED_B:
                    return State.SCHEDULED;
                case STATE_RUNNING_B:
                    return State.RUNNING;
                default:
                    String msg;
                    try {
                        msg = new String(bytes, UTF_8);
                    } catch (UnsupportedEncodingException e) {
                        msg = Arrays.toString(bytes);
                    }
                    log.error("Unknown work state: " + msg + ", work: " + workId);
                    return null;
                }
            }
        });
    }

    protected List<String> listWorkIdsList(final byte[] queueBytes) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<List<String>>() {

            @Override
            public List<String> call(Jedis jedis) {
                List<byte[]> keys = jedis.lrange(queueBytes, 0, -1);
                List<String> list = new ArrayList<String>(keys.size());
                for (byte[] workIdBytes : keys) {
                    list.add(string(workIdBytes));
                }
                return list;
            }

        });
    }

    protected List<String> listWorkIdsSet(final byte[] queueBytes) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<List<String>>() {

            @Override
            public List<String> call(Jedis jedis) {

                Set<byte[]> keys = jedis.smembers(queueBytes);
                List<String> list = new ArrayList<String>(keys.size());
                for (byte[] workIdBytes : keys) {
                    list.add(string(workIdBytes));
                }
                return list;
            }

        });

    }

    protected List<Work> listWorkList(final byte[] queueBytes) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<List<Work>>() {
            @Override
            public List<Work> call(Jedis jedis) {
                List<byte[]> keys = jedis.lrange(queueBytes, 0, -1);
                List<Work> list = new ArrayList<Work>(keys.size());
                for (byte[] workIdBytes : keys) {
                    // get data
                    byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                    Work work = deserializeWork(workBytes);
                    list.add(work);
                }
                return list;
            }
        });
    }

    protected List<Work> listWorkSet(final byte[] queueBytes) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<List<Work>>() {
            @Override
            public List<Work> call(Jedis jedis) {
                Set<byte[]> keys = jedis.smembers(queueBytes);
                List<Work> list = new ArrayList<Work>(keys.size());
                for (byte[] workIdBytes : keys) {
                    // get data
                    byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                    Work work = deserializeWork(workBytes);
                    list.add(work);
                }
                return list;
            }
        });
    }

    protected Work getWork(byte[] workIdBytes) {
        try {
            return getWorkData(workIdBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Work getWorkData(final byte[] workIdBytes) throws IOException {
        return Framework.getService(RedisExecutor.class).execute(new RedisCallable<Work>() {

            @Override
            public Work call(Jedis jedis) {
                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                return deserializeWork(workBytes);
            }

        });
    }

    /**
     * Removes first work from work queue.
     *
     * @param queueId the queue id
     * @return the work, or {@code null} if the scheduled queue is empty
     */
    protected Work getWorkFromQueue(final String queueId) throws IOException {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        List<byte[]> keys = keys(queueId);
        List<byte[]> args = Collections.singletonList(STATE_RUNNING);
        List<?> result = (List<?>) redisExecutor.evalsha(popWorkSha, keys, args);
        if (result == null) {
            return null;
        }

        List<Number> numbers = (List<Number>) result.get(0);
        WorkQueueMetrics metrics = metrics(queueId, coerceNullToZero(numbers));
        Object bytes = result.get(1);
        if (bytes instanceof String) {
            bytes = bytes((String) bytes);
        }
        Work work = deserializeWork((byte[]) bytes);

        listener.queueChanged(work, metrics);

        return work;
    }

    /**
     * Removes a given work from queue, move the work from scheduled to completed set.
     *
     * @throws IOException
     */
    protected void removeScheduledWork(final String queueId, final String workId) throws IOException {
        evalSha(cancelledScheduledWorkSha, keys(queueId), args(workId));
    }

    Number[] evalSha(byte[] sha, List<byte[]> keys, List<byte[]> args) throws JedisException {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        List<Number> numbers = (List<Number>) redisExecutor.evalsha(sha, keys, args);
        return coerceNullToZero(numbers);
    }

    protected static Number[] coerceNullToZero(List<Number> numbers) {
        return coerceNullToZero(numbers.toArray(new Number[numbers.size()]));
    }

    protected static Number[] coerceNullToZero(Number[] counters) {
        for (int i = 0; i < counters.length; ++i) {
            if (counters[i] == null) {
                counters[i] = 0;
            }
        }
        return counters;
    }

    @Override
    public void listen(Listener listener) {
        this.listener = listener;

    }

    @Override
    public boolean supportsProcessingDisabling() {
        return true;
    }

}
