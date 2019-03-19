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
import org.nuxeo.lib.stream.computation.internals.RecordFilterChainImpl;

/**
 * Settings defines stream's partitions and computation's concurrency.
 *
 * @since 9.3
 */
public class Settings {
    // streams
    protected final int defaultPartitions;

    protected final Map<String, Integer> partitions = new HashMap<>();

    protected final RecordFilterChain defaultFilter;

    protected final Map<String, RecordFilterChain> filters = new HashMap<>();

    // computations
    protected final int defaultConcurrency;

    protected final Map<String, Integer> concurrencies = new HashMap<>();

    /**
     * Default concurrency and partition to use if not specified explicitly
     */
    public Settings(int defaultConcurrency, int defaultPartitions) {
        this(defaultConcurrency, defaultPartitions, null);
    }

    @SuppressWarnings("unchecked")
    public Settings(int defaultConcurrency, int defaultPartitions, RecordFilterChain defaultFilter) {
        this.defaultConcurrency = defaultConcurrency;
        this.defaultPartitions = defaultPartitions;
        if (defaultFilter == null) {
            this.defaultFilter = RecordFilterChainImpl.NONE;
        } else {
            this.defaultFilter = defaultFilter;
        }
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

     /**
     * Add a filter
     *
     * @since 11.1
     */
    public Settings addFilter(String streamName, RecordFilter filter) {
        if (filter == null) {
            filters.remove(streamName);
        } else {
            RecordFilterChain chain = filters.computeIfAbsent(streamName, k -> new RecordFilterChainImpl());
            chain.addFilter(filter);
        }
        return this;
    }

    /**
     * Gets the filter chain for a stream.
     *
     * @since 11.1
     */
    public RecordFilterChain getFilterChain(String streamName) {
        return filters.getOrDefault(streamName, defaultFilter);
    }

}
