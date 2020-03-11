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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Encapsulates message sending and receiving through the {@link PubSubService}.
 * <p>
 * All nodes that use the same topic will receive the same messages. The discriminator is used to distinguish nodes
 * between one another, and to avoid that a node receives the messages it send itself.
 * <p>
 * An actual implementation must implement the method {@link #deserialize} (usually by delegating to a static method in
 * the {@link T} message class), and the {@link #receivedMessage} callback.
 * <p>
 * The public API is {@link #sendMessage}, and the {@link #receivedMessage} callback.
 *
 * @since 9.3
 */
public abstract class AbstractPubSubBroker<T extends SerializableMessage> {

    private static final Log log = LogFactory.getLog(AbstractPubSubBroker.class);

    private static final String UTF_8 = "UTF-8";

    protected String topic;

    protected byte[] discriminatorBytes;

    /** Deserializes an {@link InputStream} into a message, or {@code null}. */
    public abstract T deserialize(InputStream in) throws IOException;

    /**
     * Initializes the broker.
     *
     * @param topic the topic
     * @param discriminator the discriminator
     */
    public void initialize(String topic, String discriminator) {
        this.topic = topic;
        try {
            discriminatorBytes = discriminator.getBytes(UTF_8);
        } catch (IOException e) { // cannot happen
            throw new IllegalArgumentException(e);
        }
        for (byte b : discriminatorBytes) {
            if (b == DISCRIMINATOR_SEP) {
                throw new IllegalArgumentException("Invalid discriminator, must not contains separator '"
                        + (char) DISCRIMINATOR_SEP + "': " + discriminator);
            }
        }
        PubSubService pubSubService = Framework.getService(PubSubService.class);
        pubSubService.registerSubscriber(topic, this::subscriber);
    }

    /**
     * Closes this broker and releases resources.
     */
    public void close() {
        PubSubService pubSubService = Framework.getService(PubSubService.class);
        pubSubService.unregisterSubscriber(topic, this::subscriber);
    }

    protected static final byte DISCRIMINATOR_SEP = ':';

    /**
     * Sends a message to other nodes.
     */
    public void sendMessage(T message) {
        if (log.isTraceEnabled()) {
            log.trace("Sending message: " + message);
        }
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        try {
            baout.write(discriminatorBytes);
        } catch (IOException e) {
            // cannot happen, ByteArrayOutputStream.write doesn't throw
            return;
        }
        baout.write(DISCRIMINATOR_SEP);
        try {
            message.serialize(baout);
        } catch (IOException e) {
            log.error("Failed to serialize message", e);
            // don't crash for this
            return;
        }
        byte[] bytes = baout.toByteArray();
        PubSubService pubSubService = Framework.getService(PubSubService.class);
        pubSubService.publish(topic, bytes);
    }

    /**
     * PubSubService subscriber, called from a separate thread.
     */
    protected void subscriber(String topic, byte[] bytes) {
        int start = scanDiscriminator(bytes);
        if (start == -1) {
            // same discriminator or invalid message
            return;
        }
        InputStream bain = new ByteArrayInputStream(bytes, start, bytes.length - start);
        T message;
        try {
            message = deserialize(bain);
        } catch (IOException e) {
            log.error("Failed to deserialize message", e);
            // don't crash for this
            return;
        }
        if (message == null) {
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("Received message: " + message);
        }
        receivedMessage(message);
    }

    /**
     * Callback implementing the delivery of a message from another node.
     *
     * @param message the received message
     */
    public abstract void receivedMessage(T message);

    /**
     * Scans for the discriminator and returns the payload start offset.
     *
     * @return payload start offset, or -1 if the discriminator is local or if the message is invalid
     */
    protected int scanDiscriminator(byte[] message) {
        if (message == null) {
            return -1;
        }
        int start = -1;
        boolean differ = false;
        for (int i = 0; i < message.length; i++) {
            byte b = message[i];
            if (b == DISCRIMINATOR_SEP) {
                differ = differ || discriminatorBytes.length > i;
                start = i + 1;
                break;
            }
            if (!differ) {
                if (i == discriminatorBytes.length) {
                    // discriminator is a prefix of the received one
                    differ = true;
                } else if (b != discriminatorBytes[i]) {
                    // difference
                    differ = true;
                }
            }
        }
        if (!differ) {
            // same discriminator
            return -1;
        }
        return start; // may be -1 if separator was never found (invalid message)
    }

}
