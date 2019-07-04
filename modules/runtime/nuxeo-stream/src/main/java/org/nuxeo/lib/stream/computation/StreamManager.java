/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.computation;

import java.util.List;

import org.nuxeo.lib.stream.log.LogOffset;

/**
 * Gives access to StreamProcessor and appender for source provider.
 *
 * @since 11.1
 */
public interface StreamManager {
    /**
     * Registers a processor and initializes the underlying streams, this is needed before creating a processor or
     * appending record in source streams.
     */
    void register(String processorName, Topology topology, Settings settings);

    /**
     * Creates a registered processor without starting it.
     */
    StreamProcessor createStreamProcessor(String processorName);

    /**
     * Registers and creates a processor without starting it.
     */
    default StreamProcessor registerAndCreateProcessor(String processorName, Topology topology, Settings settings) {
        register(processorName, topology, settings);
        return createStreamProcessor(processorName);
    }

    /**
     * Registers some source Streams without any processors.
     */
    void register(List<String> stream, Settings settings);

    /**
     * Appends a record to a processor's source stream.
     */
    LogOffset append(String stream, Record record);
}
