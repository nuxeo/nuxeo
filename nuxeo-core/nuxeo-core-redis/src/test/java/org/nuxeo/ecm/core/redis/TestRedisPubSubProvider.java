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
package org.nuxeo.ecm.core.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.redis.RedisFeature.Mode;
import org.nuxeo.ecm.core.redis.contribs.RedisPubSubProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.pubsub.PubSubService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestRedisPubSubProvider extends NXRuntimeTestCase {

    protected PubSubService pubSubService;

    protected List<String> messages = new CopyOnWriteArrayList<>();

    protected volatile CountDownLatch messageReceivedLatch;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        RedisFeature redisFeature = RedisFeature.setUpFeature(this);
        assumeTrue("Requires a true Redis server with pubsub support", redisFeature.getMode() == Mode.server);
        // we must check for a real Redis server and not the embedded one BEFORE initializing
        // the Redis pubsub contribution, because the embedded server doesn't support pubsub
        deployBundle("org.nuxeo.runtime.pubsub");
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.redis.tests", "test-redis-pubsub-contrib.xml");
        pubSubService = Framework.getService(PubSubService.class);
    }

    @Test
    public void testPublish() throws Exception {
        messageReceivedLatch = new CountDownLatch(1);
        BiConsumer<String, byte[]> subscriber = this::subscriber;
        pubSubService.registerSubscriber("testtopic", subscriber);
        pubSubService.publish("testtopic", "foo".getBytes());
        if (!messageReceivedLatch.await(5, TimeUnit.SECONDS)) {
            fail("message not received in 5s");
        }
        assertEquals(Arrays.asList("testtopic=foo/redis"), messages);

        // with subscriber unregistered it receives nothing anymore
        pubSubService.unregisterSubscriber("testtopic", subscriber);
        messages.clear();
        pubSubService.publish("testtopic", "bar".getBytes());
        Thread.sleep(500);
        assertEquals(Collections.emptyList(), messages);
    }

    public void subscriber(String topic, byte[] message) {
        String msg = new String(message);

        // check that we're called from the Redis implementation
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (int i = 2; i < 6; i++) { // only first levels
            String className = stackTraceElements[i].getClassName();
            if (className.contains(RedisPubSubProvider.class.getName())) {
                msg += "/redis"; // yes, we're coming from the Redis implementation
                break;
            }
        }

        messages.add(topic + "=" + msg);
        messageReceivedLatch.countDown();
    }

}
