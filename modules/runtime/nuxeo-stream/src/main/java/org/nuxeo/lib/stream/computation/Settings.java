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

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.internals.RecordFilterChainImpl;
import org.nuxeo.lib.stream.log.Name;

/**
 * Settings defines stream's partitions and computation's concurrency and policy.
 *
 * @since 9.3
 */
public class Settings {
    // streams
    protected final int defaultPartitions;

    protected final Map<Name, Integer> partitions = new HashMap<>();

    protected final Codec<Record> defaultCodec;

    protected final Map<Name, Codec<Record>> codecs = new HashMap<>();

    protected final RecordFilterChain defaultFilter;

    protected final Map<Name, RecordFilterChain> filters = new HashMap<>();

    protected final Map<Name, Boolean> externals = new HashMap<>();

    protected final boolean defaultExternal;

    // computations
    protected final int defaultConcurrency;

    protected final Map<Name, Integer> concurrencies = new HashMap<>();

    protected final ComputationPolicy defaultPolicy;

    protected final Map<Name, ComputationPolicy> policies = new HashMap<>();

    /**
     * Default concurrency and partition to use if not specified explicitly.
     */
    public Settings(int defaultConcurrency, int defaultPartitions) {
        this(defaultConcurrency, defaultPartitions, null, null, null);
    }

    /**
     * Default concurrency and partition to use if not specified explicitly.
     */
    public Settings(int defaultConcurrency, int defaultPartitions, Codec<Record> defaultCodec) {
        this(defaultConcurrency, defaultPartitions, defaultCodec, null, null);
    }

    public Settings(int defaultConcurrency, int defaultPartitions, ComputationPolicy defaultPolicy) {
        this(defaultConcurrency, defaultPartitions, null, defaultPolicy, null);
    }

    public Settings(int defaultConcurrency, int defaultPartitions, Codec<Record> defaultCodec,
            ComputationPolicy defaultPolicy) {
        this(defaultConcurrency, defaultPartitions, defaultCodec, defaultPolicy, null);
    }

    public Settings(int defaultConcurrency, int defaultPartitions, Codec<Record> defaultCodec,
            ComputationPolicy defaultPolicy, RecordFilterChain defaultFilter) {
        this(defaultConcurrency, defaultPartitions, defaultCodec, defaultPolicy, defaultFilter, false);
    }

    @SuppressWarnings("unchecked")
    public Settings(int defaultConcurrency, int defaultPartitions, Codec<Record> defaultCodec,
            ComputationPolicy defaultPolicy, RecordFilterChain defaultFilter, boolean defaultExternal) {
        this.defaultConcurrency = defaultConcurrency;
        this.defaultPartitions = defaultPartitions;
        if (defaultCodec == null) {
            this.defaultCodec = NO_CODEC;
        } else {
            this.defaultCodec = defaultCodec;
        }
        if (defaultPolicy == null) {
            this.defaultPolicy = ComputationPolicy.NONE;
        } else {
            this.defaultPolicy = defaultPolicy;
        }
        if (defaultFilter == null) {
            this.defaultFilter = RecordFilterChainImpl.NONE;
        } else {
            this.defaultFilter = defaultFilter;
        }
        this.defaultExternal = defaultExternal;
    }

    /**
     * Sets the computation thread pool size.
     */
    public Settings setConcurrency(String computationName, int concurrency) {
        return setConcurrency(Name.ofUrn(computationName), concurrency);
    }

    public Settings setConcurrency(Name computationName, int concurrency) {
        concurrencies.put(computationName, concurrency);
        return this;
    }

    public int getConcurrency(String computationName) {
        return getConcurrency(Name.ofUrn(computationName));
    }

    public int getConcurrency(Name computationName) {
        return concurrencies.getOrDefault(computationName, defaultConcurrency);
    }

    /**
     * Sets the number of partitions for a stream.
     */
    public Settings setPartitions(Name streamName, int partitions) {
        this.partitions.put(streamName, partitions);
        return this;
    }

    public Settings setPartitions(String streamName, int partitions) {
        return setPartitions(Name.ofUrn(streamName), partitions);
    }

    public int getPartitions(Name streamName) {
        return partitions.getOrDefault(streamName, defaultPartitions);
    }

    public int getPartitions(String streamName) {
        return getPartitions(Name.ofUrn(streamName));
    }

    // @since 11.1
    public Settings setExternal(Name streamName, boolean external) {
        this.externals.put(streamName, external);
        return this;
    }

    // @since 11.1
    public boolean isExternal(Name streamName) {
        return externals.getOrDefault(streamName, defaultExternal);
    }

    /**
     * Sets the codec for a stream.
     *
     * @since 10.2
     */
    public Settings setCodec(Name streamName, Codec<Record> codec) {
        Objects.requireNonNull(codec);
        codecs.put(streamName, codec);
        return this;
    }

    public Settings setCodec(String streamName, Codec<Record> codec) {
        return setCodec(Name.ofUrn(streamName), codec);
    }

    /**
     * Gets the codec for a stream.
     *
     * @since 10.2
     */
    public Codec<Record> getCodec(String streamName) {
        return getCodec(Name.ofUrn(streamName));
    }

    public Codec<Record> getCodec(Name streamName) {
        return codecs.getOrDefault(streamName, defaultCodec);
    }

    /**
     * Sets the policy for a computation, when using default as computationName this sets the default policy for all
     * computations in the processor.
     *
     * @since 10.3
     */
    public Settings setPolicy(Name computationName, ComputationPolicy policy) {
        if (policy == null) {
            policies.remove(computationName);
        } else {
            policies.put(computationName, policy);
        }
        return this;
    }

    public Settings setPolicy(String computationName, ComputationPolicy policy) {
        return setPolicy(Name.ofUrn(computationName), policy);
    }

    /**
     * Gets the policy for a computation.
     *
     * @since 10.3
     */
    public ComputationPolicy getPolicy(Name computationName) {
        return policies.getOrDefault(computationName, defaultPolicy);
    }

    public ComputationPolicy getPolicy(String computationName) {
        return policies.getOrDefault(Name.ofUrn(computationName), defaultPolicy);
    }

    /**
     * Add a filter
     *
     * @since 11.1
     */
    public Settings addFilter(Name streamName, RecordFilter filter) {
        if (filter == null) {
            filters.remove(streamName);
        } else {
            RecordFilterChain chain = filters.computeIfAbsent(streamName, k -> new RecordFilterChainImpl());
            chain.addFilter(filter);
        }
        return this;
    }

    public Settings addFilter(String streamName, RecordFilter filter) {
        return addFilter(Name.ofUrn(streamName), filter);
    }

    /**
     * Gets the filter chain for a stream.
     *
     * @since 11.1
     */
    public RecordFilterChain getFilterChain(Name streamName) {
        return filters.getOrDefault(streamName, defaultFilter);
    }

    public RecordFilterChain getFilterChain(String streamName) {
        return filters.getOrDefault(Name.ofUrn(streamName), defaultFilter);
    }

}
