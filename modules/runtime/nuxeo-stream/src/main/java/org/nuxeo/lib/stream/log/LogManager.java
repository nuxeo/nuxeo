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
     *
     * @since 11.1
     */
    boolean exists(Name name);

    /**
     * @deprecated since 11.1 use {@link #exists(Name)} instead
     */
    @Deprecated(since = "11.1")
    default boolean exists(String name) {
        return exists(Name.ofUrn(name));
    }

    /**
     * Creates a new Log with {@code size} partitions if the Log does not exists. Returns true it the Log has been
     * created.
     *
     * @since 11.1
     */
    boolean createIfNotExists(Name name, int size);

    /**
     * @deprecated since 11.1 use {@link #createIfNotExists(Name, int)} instead
     */
    @Deprecated(since = "11.1")
    default boolean createIfNotExists(String name, int size) {
        return createIfNotExists(Name.ofUrn(name), size);
    }

    /**
     * Tries to delete a Log. Returns true if successfully deleted, might not be possible depending on the
     * implementation.
     *
     * @since 11.1
     */
    boolean delete(Name name);

    /**
     * @deprecated since 11.1 use {@link #delete(Name)} instead
     */
    @Deprecated(since = "11.1")
    default boolean delete(String name) {
        return delete(Name.ofUrn(name));
    }

    /**
     * Returns the number of partition of a Log.
     *
     * @since 11.1
     */
    int size(Name name);

    /**
     * Returns the number of partition of a Log.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #size(Name)} instead
     */
    @Deprecated(since = "11.1")
    default int size(String name) {
        return size(Name.ofUrn(name));
    }

    /**
     * Gets an appender for the Log named {@code name}, uses {@code codec} to encode records. An appender is thread
     * safe.
     *
     * @since 10.2
     */
    <M extends Externalizable> LogAppender<M> getAppender(Name name, Codec<M> codec);

    /**
     * @deprecated since 11.1 use {@link #getAppender(Name, Codec)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogAppender<M> getAppender(String name, Codec<M> codec) {
        return getAppender(Name.ofUrn(name), codec);
    }

    /**
     * Gets an appender for the Log named {@code name}, uses an already defined codec or the legacy encoding to encode
     * records. An appender is thread safe.
     *
     * @since 11.1
     */
    default <M extends Externalizable> LogAppender<M> getAppender(Name name) {
        return getAppender(name, NO_CODEC);
    }

    /**
     * Gets an appender for the Log named {@code name}, uses an already defined codec or the legacy encoding to encode
     * records. An appender is thread safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #getAppender(Name)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogAppender<M> getAppender(String name) {
        return getAppender(name, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign multiple {@code partitions}. Uses {@code codec} to
     * decode records. Note that {@code partitions} can be from different Logs. A tailer is NOT thread safe.
     *
     * @since 11.1
     */
    <M extends Externalizable> LogTailer<M> createTailer(Name group, Collection<LogPartition> partitions,
            Codec<M> codec);

    /**
     * Creates a tailer for a consumer {@code group} and assign multiple {@code partitions}. Uses {@code codec} to
     * decode records. Note that {@code partitions} can be from different Logs. A tailer is NOT thread safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #createTailer(Name, Name, Codec)} (Name)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions,
            Codec<M> codec) {
        return createTailer(Name.ofUrn(group), partitions, codec);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign multiple {@code partitions}. Note that
     * {@code partitions} can be from different Logs. Reads records using the legacy decoder. A tailer is NOT thread
     * safe.
     *
     * @since 11.1
     */
    default <M extends Externalizable> LogTailer<M> createTailer(Name group, Collection<LogPartition> partitions) {
        return createTailer(group, partitions, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign multiple {@code partitions}. Note that
     * {@code partitions} can be from different Logs. Reads records using the legacy decoder. A tailer is NOT thread
     * safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #createTailer(Name, Collection) (Name)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions) {
        return createTailer(Name.ofUrn(group), partitions);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign a single {@code partition}. Reads records using the
     * legacy decoder. A tailer is NOT thread safe.
     *
     * @since 11.1
     */
    default <M extends Externalizable> LogTailer<M> createTailer(Name group, LogPartition partition) {
        return createTailer(group, partition, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign a single {@code partition}. Reads records using the
     * legacy decoder. A tailer is NOT thread safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #createTailer(Name, Collection) (Name)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition) {
        return createTailer(Name.ofUrn(group), partition);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign all {@code partitions} of the Log. Reads records using
     * the legacy decoder. A tailer is NOT thread safe.
     *
     * @since 11.1
     */
    default <M extends Externalizable> LogTailer<M> createTailer(Name group, Name name) {
        return createTailer(group, name, NO_CODEC);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign all {@code partitions} of the Log. Reads records using
     * the legacy decoder. A tailer is NOT thread safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #createTailer(Name, Name)} (Name)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> createTailer(String group, String name) {
        return createTailer(Name.ofUrn(group), Name.ofUrn(name));
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign a single {@code partition}. Use an explicit codec to
     * decode records. A tailer is NOT thread safe.
     *
     * @since 11.1
     */
    default <M extends Externalizable> LogTailer<M> createTailer(Name group, LogPartition partition, Codec<M> codec) {
        return createTailer(group, Collections.singletonList(partition), codec);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assign a single {@code partition}. Use an explicit codec to
     * decode records. A tailer is NOT thread safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #createTailer(Name, LogPartition, Codec)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition, Codec<M> codec) {
        return createTailer(Name.ofUrn(group), Collections.singletonList(partition), codec);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assigns all {@code partitions} of the Log. Uses {@code codec}
     * to decode records. A tailer is NOT thread safe.
     *
     * @since 11.1
     */
    default <M extends Externalizable> LogTailer<M> createTailer(Name group, Name name, Codec<M> codec) {
        int partitions = size(name);
        if (partitions <= 0) {
            throw new IllegalArgumentException("Log name: " + name + " not found");
        }
        return createTailer(group,
                IntStream.range(0, partitions)
                         .boxed()
                         .map(partition -> new LogPartition(name, partition))
                         .collect(Collectors.toList()),
                codec);
    }

    /**
     * Creates a tailer for a consumer {@code group} and assigns all {@code partitions} of the Log. Uses {@code codec}
     * to decode records. A tailer is NOT thread safe.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #createTailer(Name, Name, Codec)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> createTailer(String group, String name, Codec<M> codec) {
        return createTailer(Name.ofUrn(group), Name.ofUrn(name), codec);
    }

    /**
     * Returns {@code true} if the Log {@link #subscribe} method is supported.
     * Now deprecated because some implementations support subscribe only on specific Log names.
     *
     * @deprecated since 2021.34 use {@link #supportSubscribe(Name)} instead
     */
    boolean supportSubscribe();

    /**
     * Returns {@code true} if the {@link #subscribe} method is supported for the specific Log.
     *
     * @since 2021.34
     */
    boolean supportSubscribe(Name stream);

    /**
     * Creates a tailer for a consumer {@code group} and subscribe to multiple Logs. The partitions assignment is done
     * dynamically depending on the number of subscribers. The partitions can change during tailers life, this is called
     * a rebalancing. A listener can be used to be notified on assignment changes. Uses {@code codec} to decode records.
     *
     * @since 11.1
     * @implSpec You should not mix {@link #createTailer} and {@code subscribe} usage using the same {@code group}.
     * @implNote A tailer is NOT thread safe.
     */
    <M extends Externalizable> LogTailer<M> subscribe(Name group, Collection<Name> names,
            RebalanceListener listener, Codec<M> codec);

    /**
     * Creates a tailer for a consumer {@code group} and subscribe to multiple Logs. The partitions assignment is done
     * dynamically depending on the number of subscribers. The partitions can change during tailers life, this is called
     * a rebalancing. A listener can be used to be notified on assignment changes. Uses {@code codec} to decode records.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #subscribe(Name, Collection, RebalanceListener, Codec)} instead
     * @implSpec You should not mix {@link #createTailer} and {@code subscribe} usage using the same {@code group}.
     * @implNote A tailer is NOT thread safe.
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec) {
        return subscribe(Name.ofUrn(group), names.stream().map(Name::ofUrn).collect(Collectors.toList()), listener,
                codec);
    }

    // @since 11.1
    default <M extends Externalizable> LogTailer<M> subscribe(Name group, Collection<Name> names,
            RebalanceListener listener) {
        return subscribe(group, names, listener, NO_CODEC);
    }

    /**
     * @since 10.2
     * @deprecated since 11.1 use {@link #subscribe(Name, Collection, RebalanceListener)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener) {
        return subscribe(group, names, listener, NO_CODEC);
    }

    /**
     * Returns the lag between consumer {@code group} and the producers for each partition. The result list is ordered,
     * for instance index 0 is lag for partition 0.
     *
     * @since 11.1
     */
    List<LogLag> getLagPerPartition(Name name, Name group);

    /**
     * Returns the lag between consumer {@code group} and the producers for each partition. The result list is ordered,
     * for instance index 0 is lag for partition 0.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #getLagPerPartition(Name, Name)} instead
     */
    @Deprecated(since = "11.1")
    default List<LogLag> getLagPerPartition(String name, String group) {
        return getLagPerPartition(Name.ofUrn(name), Name.ofUrn(group));
    }

    /**
     * Returns the lag between consumer {@code group} and producers for a Log.
     *
     * @since 11.1
     */
    default LogLag getLag(Name name, Name group) {
        return LogLag.of(getLagPerPartition(name, group));
    }

    /**
     * Returns the lag between consumer {@code group} and producers for a Log.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #getLag(Name, Name)} instead
     */
    @Deprecated(since = "11.1")
    default LogLag getLag(String name, String group) {
        return getLag(Name.ofUrn(name), Name.ofUrn(group));
    }

    /**
     * Returns the lag with latency. Timestamps used to compute the latencies are extracted from the records. This
     * requires to read one record per partition so it costs more than {@link #getLagPerPartition(Name, Name)}.
     * <p>
     * Two functions need to be provided to extract the timestamp and a key from a record.
     *
     * @since 11.1
     */
    <M extends Externalizable> List<Latency> getLatencyPerPartition(Name name, Name group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor);

    /**
     * Returns the lag with latency. Timestamps used to compute the latencies are extracted from the records. This
     * requires to read one record per partition so it costs more than {@link #getLagPerPartition(Name, Name)}.
     * <p>
     * Two functions need to be provided to extract the timestamp and a key from a record.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #getLatencyPerPartition(Name, Name, Codec, Function, Function)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> List<Latency> getLatencyPerPartition(String name, String group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return getLatencyPerPartition(Name.ofUrn(name), Name.ofUrn(group), codec, timestampExtractor, keyExtractor);
    }

    /**
     * Returns the latency between consumer {@code group} and producers for a Log.
     *
     * @since 11.1
     */
    default <M extends Externalizable> Latency getLatency(Name name, Name group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return Latency.of(getLatencyPerPartition(name, group, codec, timestampExtractor, keyExtractor));
    }

    /**
     * Returns the latency between consumer {@code group} and producers for a Log.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #getLatencyPerPartition(Name, Name, Codec, Function, Function)} instead
     */
    @Deprecated(since = "11.1")
    default <M extends Externalizable> Latency getLatency(String name, String group, Codec<M> codec,
                                                          Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return Latency.of(getLatencyPerPartition(name, group, codec, timestampExtractor, keyExtractor));
    }

    /**
     * Returns all the Log names.
     *
     * @deprecated since 11.1 use {@link #listAllNames()} instead
     */
    @Deprecated(since = "11.1")
    default List<String> listAll() {
        return listAllNames().stream().map(Name::getUrn).collect(Collectors.toList());
    }

    /**
     * Returns all the Log names.
     *
     * @since 11.1
     */
    List<Name> listAllNames();

    /**
     * List the consumer groups for a Log.
     *
     * @since 11.1
     * @implNote Note that for Kafka it returns only consumers that use the subscribe API.
     */
    List<Name> listConsumerGroups(Name name);

    /**
     * List the consumer groups for a Log.
     *
     * @since 10.2
     * @deprecated since 11.1 use {@link #listConsumerGroups(Name)} instead
     * @implNote Note that for Kafka it returns only consumers that use the subscribe API.
     */
    @Deprecated(since = "11.1")
    default List<String> listConsumerGroups(String name) {
        return listConsumerGroups(Name.ofUrn(name)).stream().map(Name::getUrn).collect(Collectors.toList());
    }

    @Override
    void close();

    /**
     * Delete all records of a stream by moving the first offsets to end of each partition.
     * Exposed for testing purpose, might not be implemented on all implementation.
     *
     * @since 2021.43
     */
    default void deleteRecords(Name name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Remove all existing consumers and their committed positions.
     * Exposed for testing purpose, might not be implemented by all implementation.
     *
     * @since 2021.43
     */
    default void deleteConsumers() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
