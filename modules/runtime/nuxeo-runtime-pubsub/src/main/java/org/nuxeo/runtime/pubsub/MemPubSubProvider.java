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
 * In-Memory implementation of {@link PubSubProvider}.
 *
 * @since 9.1
 */
public class MemPubSubProvider extends AbstractPubSubProvider {

    private final Log log = LogFactory.getLog(MemPubSubProvider.class);

    @Override
    public void initialize(Map<String, String> options, Map<String, List<BiConsumer<String, byte[]>>> subscribers) {
        super.initialize(options, subscribers);
        log.debug("Initialized");
    }

    @Override
    public void close() {
        log.debug("Closed");
    }

    @Override
    public void publish(String topic, byte[] message) {
        localPublish(topic, message);
    }

}
