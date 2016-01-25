/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.redis.contribs;

import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.storage.sql.ClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.runtime.api.Framework;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of {@link ClusterInvalidator}.
 *
 * Use a single channel pubsub to send invalidations.
 * Use an HSET to register nodes, only for debug purpose so far.
 *
 * @since 7.4
 */
public class RedisClusterInvalidator implements ClusterInvalidator {

    protected static final String PREFIX = "inval";

    // PubSub channel: nuxeo:inval:<repositoryName>:channel
    protected static final String INVALIDATION_CHANNEL = "channel";

    // Node HSET key: nuxeo:inval:<repositoryName>:nodes:<nodeId>
    protected static final String CLUSTER_NODES_KEY = "nodes";

    // Keep info about a cluster node for one day
    protected static final int TIMEOUT_REGISTER_SECOND = 24 * 3600;

    // Max delay to wait for a channel subscription
    protected static final long TIMEOUT_SUBSCRIBE_SECOND = 10;

    protected static final String STARTED_KEY = "started";

    protected static final String LAST_INVAL_KEY = "lastInvalSent";

    protected String nodeId;

    protected String repositoryName;

    protected RedisExecutor redisExecutor;

    protected Invalidations receivedInvals;

    protected Thread subscriberThread;

    protected String namespace;

    protected String startedDateTime;

    private static final Log log = LogFactory.getLog(RedisClusterInvalidator.class);

    private CountDownLatch subscribeLatch;

    @Override
    public void initialize(String nodeId, RepositoryImpl repository) {
        this.nodeId = nodeId;
        this.repositoryName = repository.getName();
        redisExecutor = Framework.getLocalService(RedisExecutor.class);
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        namespace = redisAdmin.namespace(PREFIX, repositoryName);
        receivedInvals = new Invalidations();
        createSubscriberThread();
        registerNode();
    }

    protected void createSubscriberThread() {
        subscribeLatch = new CountDownLatch(1);
        String name = "RedisClusterInvalidatorSubscriber:" + repositoryName + ":" + nodeId;
        subscriberThread = new Thread(this::subscribeToInvalidationChannel, name);
        subscriberThread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught error on thread " + t.getName(), e));
        subscriberThread.setPriority(Thread.NORM_PRIORITY);
        subscriberThread.start();
        try {
            if (! subscribeLatch.await(TIMEOUT_SUBSCRIBE_SECOND, TimeUnit.SECONDS)) {
                log.error("Redis channel subscripion timeout after " + TIMEOUT_SUBSCRIBE_SECOND +
                        "s, continuing but this node may not receive cluster invalidations");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected void subscribeToInvalidationChannel() {
        log.info("Subscribing to channel: " + getChannelName());
        redisExecutor.execute(jedis -> {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    super.onSubscribe(channel, subscribedChannels);
                    if (subscribeLatch != null) {
                        subscribeLatch.countDown();
                    }
                    log.debug("Subscribed to channel: " + getChannelName());
                }

                @Override
                public void onMessage(String channel, String message) {
                    try {
                        RedisInvalidations rInvals = new RedisInvalidations(nodeId, message);
                        if (log.isTraceEnabled()) {
                            log.trace("Receive invalidations: " + rInvals);
                        }
                        Invalidations invals = rInvals.getInvalidations();
                        synchronized (RedisClusterInvalidator.this) {
                            receivedInvals.add(invals);
                        }
                    } catch (IllegalArgumentException e) {
                        log.error("Fail to read message: " + message, e);
                    }
                }
            }, getChannelName());
            return null;
        });
    }

    protected String getChannelName() {
        return namespace + INVALIDATION_CHANNEL;
    }

    protected void registerNode() {
        startedDateTime = getCurrentDateTime();
        log.info("Registering node: " + nodeId);
        redisExecutor.execute(jedis -> {
            String key = getNodeKey();
            Pipeline pipe = jedis.pipelined();
            pipe.hset(key, "started", startedDateTime);
            // Use an expiration so we can access info after a shutdown
            pipe.expire(key, TIMEOUT_REGISTER_SECOND);
            pipe.sync();
            return null;
        });
    }

    protected String getNodeKey() {
        return namespace + CLUSTER_NODES_KEY + ":" + nodeId;
    }

    @Override
    public void close() {
        log.debug("Closing");
        unsubscribeToInvalidationChannel();
        // The Jedis pool is already closed when the repository is shutdowned
        receivedInvals.clear();
    }

    protected void unsubscribeToInvalidationChannel() {
        subscriberThread.interrupt();
    }


    @Override
    public Invalidations receiveInvalidations() {
        Invalidations newInvals = new Invalidations();
        Invalidations ret;
        synchronized (this) {
            ret = receivedInvals;
            receivedInvals = newInvals;
        }
        return ret;
    }

    @Override
    public void sendInvalidations(Invalidations invals) {
        redisExecutor.execute(jedis -> {
            RedisInvalidations rInvals = new RedisInvalidations(nodeId, invals);
            if (log.isTraceEnabled()) {
                log.trace("Sending invalidations: " + rInvals);
            }
            String key = getNodeKey();

            try {
                Pipeline pipe = jedis.pipelined();
                pipe.publish(getChannelName(), rInvals.serialize());
                pipe.hset(key, STARTED_KEY, startedDateTime);
                pipe.hset(key, LAST_INVAL_KEY, getCurrentDateTime());
                pipe.expire(key, TIMEOUT_REGISTER_SECOND);
                pipe.sync();
                return null;
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        });
    }

    protected String getCurrentDateTime() {
        return LocalDateTime.now().toString();
    }
}
