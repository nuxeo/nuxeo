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

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract implementation of {@link PubSubProvider}.
 * <p>
 * This deals with subscribers registration and dispatch.
 *
 * @since 9.1
 */
public abstract class AbstractPubSubProvider implements PubSubProvider {

    private final Log log = LogFactory.getLog(AbstractPubSubProvider.class);

    protected String namespace;

    /** List of subscribers for each topic. */
    protected Map<String, List<BiConsumer<String, byte[]>>> subscribers;

    @Override
    public void initialize(Map<String, String> options, Map<String, List<BiConsumer<String, byte[]>>> subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public void close() {
        // DO NOT subscribers.clear(), we do not own this map
    }

    public void localPublish(String topic, byte[] message) {
        if (subscribers == null) {
            // not yet initialized
            return;
        }
        List<BiConsumer<String, byte[]>> subs = subscribers.get(topic);
        if (subs != null) {
            for (BiConsumer<String, byte[]> subscriber : subs) {
                try {
                    subscriber.accept(topic, message);
                } catch (RuntimeException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw e;
                    }
                    // don't break everything if a subscriber is ill-behaved
                    log.error("Exception in subscriber for topic: " + topic, e);
                }
            }
        }
    }

}
