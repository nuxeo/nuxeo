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
package org.nuxeo.runtime.pubsub;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.pubsub")
public class TestMemPubSubProvider {

    @Inject
    protected PubSubService pubSubService;

    protected List<String> messages = new CopyOnWriteArrayList<>();

    protected volatile CountDownLatch messageReceivedLatch;

    @Before
    public void setUp() {
        messages.clear();
    }

    @Test
    public void testNoSubscriber() throws Exception {
        // nothing to receive it but we can still send something into the void
        pubSubService.publish("testtopic", "foo".getBytes(UTF_8));
    }

    @Test
    public void testPublish() throws Exception {
        messageReceivedLatch = new CountDownLatch(1);
        BiConsumer<String, byte[]> subscriber = this::subscriber;
        pubSubService.registerSubscriber("testtopic", subscriber);
        pubSubService.publish("testtopic", "foo".getBytes(UTF_8));
        if (!messageReceivedLatch.await(5, TimeUnit.SECONDS)) {
            fail("message not received in 5s");
        }
        assertEquals(Collections.singletonList("testtopic=foo"), messages);

        // with subscriber unregistered it receives nothing anymore
        pubSubService.unregisterSubscriber("testtopic", subscriber);
        messages.clear();
        pubSubService.publish("testtopic", "bar".getBytes(UTF_8));
        Thread.sleep(500);
        assertEquals(Collections.emptyList(), messages);
    }

    public void subscriber(String topic, byte[] message) {
        String msg = new String(message, UTF_8);
        messages.add(topic + "=" + msg);
        messageReceivedLatch.countDown();
    }

}
