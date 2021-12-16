/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.common.concurrent.ThreadFactories.newThreadFactory;
import static redis.clients.jedis.Protocol.Keyword.MESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PMESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.PUNSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.SUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.UNSUBSCRIBE;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.pubsub.AbstractPubSubProvider;
import org.nuxeo.runtime.pubsub.PubSubProvider;

import redis.clients.jedis.Client;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.SafeEncoder;

/**
 * Redis implementation of {@link PubSubProvider}.
 *
 * @since 9.1
 */
public class RedisPubSubProvider extends AbstractPubSubProvider {

    // package-private to avoid synthetic accessor for nested class
    static final Log log = LogFactory.getLog(RedisPubSubProvider.class);

    /** Maximum delay to wait for a channel subscription on startup. */
    public static final long TIMEOUT_SUBSCRIBE_SECONDS = 5;

    protected static final String THREAD_NAME = "Nuxeo-PubSub-Redis";

    protected static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool(newThreadFactory(THREAD_NAME));

    protected Dispatcher dispatcher;

    protected Thread thread;

    @Override
    public void initialize(Map<String, String> options, Map<String, List<BiConsumer<String, byte[]>>> subscribers) {
        super.initialize(options, subscribers);
        log.debug("Initializing");
        namespace = Framework.getService(RedisAdmin.class).namespace();
        dispatcher = new Dispatcher(namespace + "*");
        thread = new Thread(dispatcher::run, THREAD_NAME);
        thread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught error on thread " + t.getName(), e));
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(true);
        thread.start();
        if (!dispatcher.awaitSubscribed(TIMEOUT_SUBSCRIBE_SECONDS, TimeUnit.SECONDS)) {
            thread.interrupt();
            throw new NuxeoException(
                    "Failed to subscribe to Redis pubsub after " + TIMEOUT_SUBSCRIBE_SECONDS + "s");
        }
        log.debug("Initialized");
    }

    @Override
    public void close() {
        log.debug("Closing");
        if (dispatcher != null) {
            thread.interrupt();
            thread = null;
            dispatcher.close();
            dispatcher = null;
        }
        log.debug("Closed");
    }

    /**
     * Subscribes to the provided Redis channel pattern and dispatches received messages. Method {@code #run} must be
     * called in a new thread.
     */
    public class Dispatcher extends JedisPubSub {

        // we look this up during construction in the main thread,
        // because service lookup is unavailable from alternative threads during startup
        protected RedisExecutor redisExecutor;

        protected final String pattern;

        protected final CountDownLatch subscribedLatch;

        protected volatile boolean stop;

        public Dispatcher(String pattern) {
            redisExecutor = Framework.getService(RedisExecutor.class);
            this.pattern = pattern;
            this.subscribedLatch = new CountDownLatch(1);
        }

        /**
         * To be called from the main thread to wait for subscription to be effective.
         */
        public boolean awaitSubscribed(long timeout, TimeUnit unit) {
            try {
                return subscribedLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NuxeoException(e);
            }
        }

        /**
         * To be called from a new thread to do the actual Redis subscription and to dispatch messages.
         */
        public void run() {
            log.debug("Subscribing to: " + pattern);
            // we can't do service lookup during startup here because we're in a separate thread
            RedisExecutor redisExecutor = this.redisExecutor;
            this.redisExecutor = null;
            redisExecutor.psubscribe(this, pattern);
        }

        /**
         * To be called from the main thread to stop the subscription.
         */
        public void close() {
            stop = true;
            // send an empty message so that the dispatcher thread can be woken up and stop
            publish("", new byte[0]);
        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            subscribedLatch.countDown();
            if (log.isDebugEnabled()) {
                log.debug("Subscribed to: " + pattern);
            }
        }

        public void onMessage(String channel, byte[] message) {
            if (message == null) {
                message = new byte[0];
            }
            if (log.isTraceEnabled()) {
                log.trace("Message received from channel: " + channel + " (" + message.length + " bytes)");
            }
            String topic = channel.substring(namespace.length());
            // localPublish needs to be called in a different thread,
            // so that if a subscriber calls Redis it doesn't reuse our current Redis connection
            // which can only be used for subscribe/unsubscribe/ping commands.
            final byte[] finalMessage = message;
            THREAD_POOL.execute(() -> localPublish(topic, finalMessage));
        }

        public void onPMessage(String pattern, String channel, byte[] message) {
            onMessage(channel, message);
        }

        @Override
        public void proceed(Client client, String... channels) {
            client.subscribe(channels);
            flush(client);
            processBinary(client);
        }

        @Override
        public void proceedWithPatterns(Client client, String... patterns) {
            client.psubscribe(patterns);
            flush(client);
            processBinary(client);
        }

        // stupid Jedis has a protected flush method
        protected void flush(Client client) {
            try {
                Method m = redis.clients.jedis.Connection.class.getDeclaredMethod("flush");
                m.setAccessible(true);
                m.invoke(client);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
        }

        // patched process() to pass the raw binary message to onMessage and onPMessage
        protected void processBinary(Client client) {
            for (;;) {
                List<Object> reply = client.getRawObjectMultiBulkReply();
                if (stop) {
                    return;
                }
                Object type = reply.get(0);
                if (!(type instanceof byte[])) {
                    throw new JedisException("Unknown message type: " + type);
                }
                byte[] btype = (byte[]) type;
                if (Arrays.equals(MESSAGE.raw, btype)) {
                    byte[] bchannel = (byte[]) reply.get(1);
                    byte[] bmesg = (byte[]) reply.get(2);
                    onMessage(toString(bchannel), bmesg);
                } else if (Arrays.equals(PMESSAGE.raw, btype)) {
                    byte[] bpattern = (byte[]) reply.get(1);
                    byte[] bchannel = (byte[]) reply.get(2);
                    byte[] bmesg = (byte[]) reply.get(3);
                    onPMessage(toString(bpattern), toString(bchannel), bmesg);
                } else if (Arrays.equals(SUBSCRIBE.raw, btype)) {
                    byte[] bchannel = (byte[]) reply.get(1);
                    onSubscribe(toString(bchannel), 0);
                } else if (Arrays.equals(PSUBSCRIBE.raw, btype)) {
                    byte[] bpattern = (byte[]) reply.get(1);
                    onPSubscribe(toString(bpattern), 0);
                } else if (Arrays.equals(UNSUBSCRIBE.raw, btype)) {
                    byte[] bchannel = (byte[]) reply.get(1);
                    onUnsubscribe(toString(bchannel), 0);
                } else if (Arrays.equals(PUNSUBSCRIBE.raw, btype)) {
                    byte[] bpattern = (byte[]) reply.get(1);
                    onPUnsubscribe(toString(bpattern), 0);
                } else {
                    throw new JedisException("Unknown message: " + toString(btype));
                }
            }
        }

        protected String toString(byte[] bytes) {
            return bytes == null ? null : SafeEncoder.encode(bytes);
        }

    }

    // ===== PubSubService =====

    @Override
    public void publish(String topic, byte[] message) {
        String channel = namespace + topic;
        byte[] bchannel = SafeEncoder.encode(channel);
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        if (redisExecutor != null) {
            redisExecutor.execute(jedis -> jedis.publish(bchannel, message));
        }
    }

}
