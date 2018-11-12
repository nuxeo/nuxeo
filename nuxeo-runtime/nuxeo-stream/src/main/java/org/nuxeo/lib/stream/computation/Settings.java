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

/**
 * Settings defines stream's partitions and computation's concurrency and policy.
 *
 * @since 9.3
 */
public class Settings {
    protected final int defaultConcurrency;

    protected final int defaultPartitions;

    protected final Codec<Record> defaultCodec;

    protected final ComputationPolicy defaultPolicy;

    protected final Map<String, Integer> concurrencies = new HashMap<>();

    protected final Map<String, Integer> partitions = new HashMap<>();

    protected final Map<String, Codec<Record>> codecs = new HashMap<>();

    protected final Map<String, ComputationPolicy> policies = new HashMap<>();

    /**
     * Default concurrency and partition to use if not specified explicitly.
     */
    public Settings(int defaultConcurrency, int defaultPartitions) {
        this(defaultConcurrency, defaultPartitions, null, null);
    }

    /**
     * Default concurrency and partition to use if not specified explicitly.
     */
    public Settings(int defaultConcurrency, int defaultPartitions, Codec<Record> defaultCodec) {
        this(defaultConcurrency, defaultPartitions, defaultCodec, null);
    }

    public Settings(int defaultConcurrency, int defaultPartitions, ComputationPolicy defaultPolicy) {
        this(defaultConcurrency, defaultPartitions, null, defaultPolicy);
    }

    @SuppressWarnings("unchecked")
    public Settings(int defaultConcurrency, int defaultPartitions, Codec<Record> defaultCodec,
            ComputationPolicy defaultPolicy) {
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
    }

    /**
     * Sets the computation thread pool size.
     */
    public Settings setConcurrency(String computationName, int concurrency) {
        concurrencies.put(computationName, concurrency);
        return this;
    }

    public int getConcurrency(String computationName) {
        return concurrencies.getOrDefault(computationName, defaultConcurrency);
    }

    /**
     * Sets the number of partitions for a stream.
     */
    public Settings setPartitions(String streamName, int partitions) {
        this.partitions.put(streamName, partitions);
        return this;
    }

    public int getPartitions(String streamName) {
        return partitions.getOrDefault(streamName, defaultPartitions);
    }

    /**
     * Sets the codec for a stream.
     *
     * @since 10.2
     */
    public Settings setCodec(String streamName, Codec<Record> codec) {
        Objects.requireNonNull(codec);
        codecs.put(streamName, codec);
        return this;
    }

    /**
     * Gets the codec for a stream.
     *
     * @since 10.2
     */
    public Codec<Record> getCodec(String streamName) {
        return codecs.getOrDefault(streamName, defaultCodec);
    }

    /**
     * Sets the policy for a computation, when using default as computationName this sets the default policy for all
     * computations in the processor.
     *
     * @since 10.3
     */
    public Settings setPolicy(String computationName, ComputationPolicy policy) {
        if (policy == null) {
            policies.remove(computationName);
        } else {
            policies.put(computationName, policy);
        }
        return this;
    }

    /**
     * Gets the policy for a computation.
     *
     * @since 10.3
     */
    public ComputationPolicy getPolicy(String computationName) {
        return policies.getOrDefault(computationName, defaultPolicy);
    }

}
