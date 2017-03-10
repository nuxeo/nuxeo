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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Encapsulates invalidations management through the {@link PubSubService}.
 * <p>
 * All nodes that use the same topic will share the same invalidations.
 * <p>
 * The discriminator is used to distinguish nodes between one another, and to avoid that a node receives the
 * invalidations it send itself.
 *
 * @since 9.1
 */
public abstract class AbstractPubSubInvalidator<T extends SerializableInvalidations> {

    private static final Log log = LogFactory.getLog(AbstractPubSubInvalidator.class);

    private static final String UTF_8 = "UTF-8";

    protected String topic;

    protected byte[] discriminatorBytes;

    protected volatile T bufferedInvalidations;

    /** Constructs new empty invalidations, of type {@link T}. */
    public abstract T newInvalidations();

    /** Deserializes an {@link InputStream} into invalidations, or {@code null}. */
    public abstract T deserialize(InputStream in) throws IOException;

    /**
     * Initializes the invalidator.
     *
     * @param topic the topic
     * @param discriminator the discriminator
     */
    public void initialize(String topic, String discriminator) {
        this.topic = topic;
        try {
            discriminatorBytes = discriminator.getBytes(UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        for (byte b : discriminatorBytes) {
            if (b == DISCRIMINATOR_SEP) {
                throw new IllegalArgumentException("Invalid discriminator, must not contains separator '"
                        + (char) DISCRIMINATOR_SEP + "': " + discriminator);
            }
        }
        bufferedInvalidations = newInvalidations();
        PubSubService pubSubService = Framework.getService(PubSubService.class);
        pubSubService.registerSubscriber(topic, this::subscriber);
    }

    /**
     * Closes this invalidator and releases resources.
     */
    public void close() {
        PubSubService pubSubService = Framework.getService(PubSubService.class);
        pubSubService.unregisterSubscriber(topic, this::subscriber);
        // not null to avoid crashing subscriber thread still in flight
        bufferedInvalidations = newInvalidations();
    }

    protected static final byte DISCRIMINATOR_SEP = ':';

    /**
     * Sends invalidations to other nodes.
     */
    public void sendInvalidations(T invalidations) {
        if (log.isTraceEnabled()) {
            log.trace("Sending invalidations: " + invalidations);
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
            invalidations.serialize(baout);
        } catch (IOException e) {
            log.error("Failed to serialize invalidations", e);
            // don't crash for this
            return;
        }
        byte[] message = baout.toByteArray();
        PubSubService pubSubService = Framework.getService(PubSubService.class);
        pubSubService.publish(topic, message);
    }

    /**
     * PubSubService subscriber, called from a separate thread.
     */
    protected void subscriber(String topic, byte[] message) {
        int start = scanDiscriminator(message);
        if (start == -1) {
            // same discriminator or invalid message
            return;
        }
        InputStream bain = new ByteArrayInputStream(message, start, message.length - start);
        T invalidations;
        try {
            invalidations = deserialize(bain);
        } catch (IOException e) {
            log.error("Failed to deserialize invalidations", e);
            // don't crash for this
            return;
        }
        if (invalidations == null || invalidations.isEmpty()) {
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("Receiving invalidations: " + invalidations);
        }
        synchronized (this) {
            bufferedInvalidations.add(invalidations);
        }
    }

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

    /**
     * Receives invalidations from other nodes.
     */
    public T receiveInvalidations() {
        T newInvalidations = newInvalidations();
        T invalidations;
        synchronized (this) {
            invalidations = bufferedInvalidations;
            bufferedInvalidations = newInvalidations;
        }
        if (log.isTraceEnabled()) {
            log.trace("Received invalidations: " + invalidations);
        }
        return invalidations;
    }

}
