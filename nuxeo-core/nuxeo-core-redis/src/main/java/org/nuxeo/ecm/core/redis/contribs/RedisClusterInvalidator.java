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

    protected static final int TIMEOUT_REGISTER_SECOND = 24 * 3600;

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
        String name = "RedisClusterInvalidatorSubscriber:" + repositoryName + ":" + nodeId;
        subscriberThread = new Thread(this::subscribeToInvalidationChannel, name);
        subscriberThread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught error on thread " + t.getName(), e));
        subscriberThread.setPriority(Thread.NORM_PRIORITY);
        subscriberThread.start();
    }

    protected void subscribeToInvalidationChannel() {
        log.info("Subscribe to channel: " + getChannelName());
        redisExecutor.execute(jedis -> {
            jedis.subscribe(new JedisPubSub() {
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
