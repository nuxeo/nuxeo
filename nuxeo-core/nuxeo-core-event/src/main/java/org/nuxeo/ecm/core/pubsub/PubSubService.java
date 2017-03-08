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
package org.nuxeo.ecm.core.pubsub;

import java.util.function.BiConsumer;

/**
 * Publish/Subscribe Service.
 * <p>
 * This service allows cross-instance notifications through simple messages sent to topics.
 *
 * @since 9.1
 */
public interface PubSubService {

    /**
     * Publishes a message to the given topic.
     *
     * @param topic the topic
     * @param message the message
     */
    void publish(String topic, byte[] message);

    /**
     * Registers a subscriber for the given topic.
     * <p>
     * The subscriber must deal with the message without delay and return immediately, usually by storing it in a
     * thread-safe datastructure.
     *
     * @param topic the topic
     * @param subscriber the subscriber, who will receive the topic and a {@code byte[]} message
     */
    void registerSubscriber(String topic, BiConsumer<String, byte[]> subscriber);

    /**
     * Unregisters a subscriber for the given topic.
     *
     * @param topic the topic
     * @param subscriber the subscriber
     */
    void unregisterSubscriber(String topic, BiConsumer<String, byte[]> subscriber);

}
