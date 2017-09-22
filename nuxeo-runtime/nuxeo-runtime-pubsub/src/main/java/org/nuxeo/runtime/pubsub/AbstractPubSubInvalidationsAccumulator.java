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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Encapsulates invalidations management through the {@link PubSubService}.
 *
 * @since 9.3
 */
public abstract class AbstractPubSubInvalidationsAccumulator<T extends SerializableAccumulableInvalidations>
        extends AbstractPubSubBroker<T> {

    private static final Log log = LogFactory.getLog(AbstractPubSubInvalidationsAccumulator.class);

    protected volatile T bufferedInvalidations;

    /** Constructs new empty invalidations, of type {@link T}. */
    public abstract T newInvalidations();

    @Override
    public void initialize(String topic, String discriminator) {
        bufferedInvalidations = newInvalidations();
        super.initialize(topic, discriminator);
    }

    @Override
    public void close() {
        super.close();
        // not null to avoid crashing subscriber thread still in flight
        bufferedInvalidations = newInvalidations();
    }

    /**
     * Sends invalidations to other nodes.
     */
    public void sendInvalidations(T invalidations) {
        sendMessage(invalidations);
    }

    @Override
    public void receivedMessage(T invalidations) {
        if (log.isTraceEnabled()) {
            log.trace("Received invalidations: " + invalidations);
        }
        synchronized (this) {
            bufferedInvalidations.add(invalidations);
        }
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
