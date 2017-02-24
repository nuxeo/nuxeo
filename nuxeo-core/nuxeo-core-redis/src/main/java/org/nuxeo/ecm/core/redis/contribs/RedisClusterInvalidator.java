package org.nuxeo.ecm.core.redis.contribs;/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */

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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

    protected static final String STARTED_FIELD = "started";

    protected static final String LAST_INVAL_FIELD = "lastInvalSent";

    protected String nodeId;

    protected String repositoryName;

    protected RedisExecutor redisExecutor;

    protected Invalidations receivedInvals;

    protected Thread subscriberThread;

    protected String namespace;

    protected String startedDateTime;

    private static final Log log = LogFactory.getLog(RedisClusterInvalidator.class);

    private CountDownLatch subscribeLatch;

    private String registerSha;
    private String sendSha;

    @Override
    public void initialize(String nodeId, RepositoryImpl repository) {
        this.nodeId = nodeId;
        this.repositoryName = repository.getName();
        redisExecutor = Framework.getLocalService(RedisExecutor.class);
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        namespace = redisAdmin.namespace(PREFIX, repositoryName);
        try {
            registerSha = redisAdmin.load("org.nuxeo.ecm.core.redis", "register-node-inval");
            sendSha = redisAdmin.load("org.nuxeo.ecm.core.redis", "send-inval");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            if (!subscribeLatch.await(TIMEOUT_SUBSCRIBE_SECOND, TimeUnit.SECONDS)) {
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
        redisExecutor.subscribe(new JedisPubSub() {
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
    }

    protected String getChannelName() {
        return namespace + INVALIDATION_CHANNEL;
    }

    protected void registerNode() {
        final String startedDateTime = getCurrentDateTime();
        final List<String> keys = Arrays.asList(getNodeKey());
        final List<String> args = Arrays.asList(STARTED_FIELD, startedDateTime,
                Integer.valueOf(TIMEOUT_REGISTER_SECOND).toString());
        log.debug("Registering node: " + nodeId);

        redisExecutor.evalsha(registerSha, keys, args);
        if (log.isInfoEnabled()) {
            log.info("Node registered: " + nodeId);
        }
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
        final String startedDateTime = getCurrentDateTime();
        RedisInvalidations rInvals = new RedisInvalidations(nodeId, invals);
        if (log.isTraceEnabled()) {
            log.trace("Sending invalidations: " + rInvals);
        }
        final List<String> keys = Arrays.asList(getChannelName(), getNodeKey());
        final List<String> args;
        try {
            args = Arrays.asList(rInvals.serialize(), STARTED_FIELD, startedDateTime,
                    LAST_INVAL_FIELD, getCurrentDateTime(), Integer.valueOf(TIMEOUT_REGISTER_SECOND).toString());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        redisExecutor.evalsha(sendSha, keys, args);
        log.trace("invals sent");
    }

    protected String getCurrentDateTime() {
        return LocalDateTime.now().toString();
    }
}
