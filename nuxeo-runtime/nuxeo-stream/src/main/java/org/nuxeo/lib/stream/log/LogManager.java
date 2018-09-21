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
package org.nuxeo.lib.stream.log;

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.Externalizable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.nuxeo.lib.stream.codec.Codec;

/**
 * Manage Log and give access to Appenders and Tailers. Closing the LogManager will also close all its appenders and
 * tailers.
 *
 * @since 9.3
 */
@SuppressWarnings("unchecked")
public interface LogManager extends AutoCloseable {

    /**
     * Returns {@code true} if a Log with this {@code name} exists.
     */
    boolean exists(String name);

    /**
     * Creates a new Log with {@code size} partitions if the Log does not exists. Returns true it the Log has been
     * created.
     */
    boolean createIfNotExists(String name, int size);

    /**
     * Tries to delete a Log. Returns true if successfully deleted, might not be possible depending on the
     * implementation.
     */
    boolean delete(String name);

    /**
     * Returns the number of partition of a Log.
     *
     * @since 10.2
     */
    int size(String name);

    /**
     * Gets an appender for the Log named {@code name}, uses {@code codec} to encode records. An appender is thread
     * safe.
     *
     * @since 10.2
     */
    <M extends Externalizable> LogAppender<M> getAppender(String name, Codec<M> codec);

    /**
     * Gets an appender for the Log named {@code name}, uses an already defined codec or the legacy encoding to encode
     * records. An appender is thread safe.
     */
    default <M extends Externalizable> LogAppender<M> getAppender(String name) {
        return getAppender(name, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign multiple {@code partitions}. Uses {@code codec} to
     * decode records. Note that {@code partitions} can be from different Logs. A tailer is NOT thread safe.
     *
     * @since 10.2
     */
    <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions,
            Codec<M> codec);

    /**
     * Creates a tailer for a consumer {@code group} and assign multiple {@code partitions}. Note that
     * {@code partitions} can be from different Logs. Reads records using the legacy decoder. A tailer is NOT thread
     * safe.
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions) {
        return createTailer(group, partitions, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign a single {@code partition}. Reads records using the
     * legacy decoder. A tailer is NOT thread safe.
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition) {
        return createTailer(group, partition, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign all {@code partitions} of the Log. Reads records using
     * the legacy decoder. A tailer is NOT thread safe.
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, String name) {
        return createTailer(group, name, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign a single {@code partition}. Use an explicit codec to
     * decode records. A tailer is NOT thread safe.
     *
     * @since 10.2
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition, Codec<M> codec) {
        return createTailer(group, Collections.singletonList(partition), codec);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assigns all {@code partitions} of the Log. Uses {@code codec}
     * to decode records. A tailer is NOT thread safe.
     *
     * @since 10.2
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, String name, Codec<M> codec) {
        int partitions = size(name);
        if (partitions <= 0) {
            System.out.println(this);
            throw new IllegalArgumentException("Log name: " + name + " not found");
        }
        return createTailer(group,
                IntStream.range(0, partitions).boxed().map(partition -> new LogPartition(name, partition)).collect(
                        Collectors.toList()),
                codec);
    }

    /**
     * Returns {@code true} if the Log {@link #subscribe} method is supported.
     */
    boolean supportSubscribe();

    /**
     * Creates a tailer for a consumer {@code group} and subscribe to multiple Logs. The partitions assignment is done
     * dynamically depending on the number of subscribers. The partitions can change during tailers life, this is called
     * a rebalancing. A listener can be used to be notified on assignment changes. Uses {@code codec} to decode records.
     * <p/>
     * A tailer is NOT thread safe.
     * <p/>
     * You should not mix {@link #createTailer} and {@code subscribe} usage using the same {@code group}.
     */
    <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec);

    default <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener) {
        return subscribe(group, names, listener, NO_CODEC);
    }

    /**
     * Returns the lag between consumer {@code group} and the producers for each partition. The result list is ordered,
     * for instance index 0 is lag for partition 0.
     */
    List<LogLag> getLagPerPartition(String name, String group);

    /**
     * Returns the lag between consumer {@code group} and producers for a Log.
     */
    default LogLag getLag(String name, String group) {
        return LogLag.of(getLagPerPartition(name, group));
    }

    /**
     * Returns the lag with latency. Timestamps used to compute the latencies are extracted from the records. This
     * requires to read one record per partition so it costs more than {@link #getLagPerPartition(String, String)}.
     * <br/>
     * Two functions need to be provided to extract the timestamp and a key from a record.
     *
     * @since 10.2
     */
    <M extends Externalizable> List<Latency> getLatencyPerPartition(String name, String group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor);

    /**
     * Returns the latency between consumer {@code group} and producers for a Log.
     *
     * @since 10.2
     */
    default <M extends Externalizable> Latency getLatency(String name, String group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return Latency.of(getLatencyPerPartition(name, group, codec, timestampExtractor, keyExtractor));
    }

    /**
     * Returns the lag with latency. Timestamps used to compute the latencies are extracted from the records. This
     * requires to read one record per partition so it costs more than {@link #getLagPerPartition(String, String)}.
     * <br/>
     * Two functions need to be provided to extract the timestamp and a key from a record.
     *
     * @since 10.1
     * @deprecated 10.2 use {@link #getLatencyPerPartition(String, String, Codec, Function, Function)} instead.
     */
    @Deprecated
    default <M extends Externalizable> List<Latency> getLatencyPerPartition(String name, String group,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return getLatencyPerPartition(name, group, NO_CODEC, timestampExtractor, keyExtractor);
    }

    /**
     * Returns the latency between consumer {@code group} and producers for a Log.
     *
     * @since 10.1
     * @deprecated 10.2 use {@link #getLatency(String, String, Codec, Function, Function)} instead.
     */
    @Deprecated
    default <M extends Externalizable> Latency getLatency(String name, String group,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return getLatency(name, group, NO_CODEC, timestampExtractor, keyExtractor);
    }

    /**
     * Returns all the Log names.
     */
    List<String> listAll();

    /**
     * List the consumer groups for a Log.<br/>
     * Note that for Kafka it returns only consumers that use the subscribe API.
     */
    List<String> listConsumerGroups(String name);

    @Override
    void close();
}
