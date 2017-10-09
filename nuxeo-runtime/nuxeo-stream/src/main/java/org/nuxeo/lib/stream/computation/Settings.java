/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Settings defines stream's partitions and computation's concurrency.
 *
 * @since 9.3
 */
public class Settings {
    protected final int defaultConcurrency;

    protected final int defaultPartitions;

    protected final Map<String, Integer> concurrencies = new HashMap<>();

    protected final Map<String, Integer> partitions = new HashMap<>();

    /**
     * Default concurrency and partition to use if not specified explicitly
     */
    public Settings(int defaultConcurrency, int defaultPartitions) {
        this.defaultConcurrency = defaultConcurrency;
        this.defaultPartitions = defaultPartitions;
    }

    /**
     * Set the computation thread pool size.
     */
    public Settings setConcurrency(String computationName, int concurrency) {
        concurrencies.put(computationName, concurrency);
        return this;
    }

    public int getConcurrency(String computationName) {
        return concurrencies.getOrDefault(computationName, defaultConcurrency);
    }

    /**
     * Set the number of partitions for a stream.
     */
    public Settings setPartitions(String streamName, int partitions) {
        this.partitions.put(streamName, partitions);
        return this;
    }

    public int getPartitions(String streamName) {
        return partitions.getOrDefault(streamName, defaultPartitions);
    }

}
