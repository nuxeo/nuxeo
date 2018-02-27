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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.Externalizable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manage Log and give access to Appenders and Tailers. Closing the LogManager will also close all its appenders and
 * tailers.
 *
 * @since 9.3
 */
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
     * Try to delete a Log. Returns true if successfully deleted, might not be possible depending on the implementation.
     */
    boolean delete(String name);

    /**
     * Get an appender for the Log named {@code name}. An appender is thread safe.
     */
    <M extends Externalizable> LogAppender<M> getAppender(String name);

    /**
     * Create a tailer for a consumer {@code group} and assign multiple {@code partitions}. Note that {@code partitions}
     * can be from different Logs. A tailer is NOT thread safe.
     */
    <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions);

    /**
     * Create a tailer for a consumer {@code group} and assign a single {@code partition}. A tailer is NOT thread safe.
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition) {
        return createTailer(group, Collections.singletonList(partition));
    }

    /**
     * Create a tailer for a consumer {@code group} and assign all {@code partitions} of the Log. A tailer is NOT thread
     * safe.
     */
    default <M extends Externalizable> LogTailer<M> createTailer(String group, String name) {
        int size = getAppender(name).size();
        return createTailer(group,
                IntStream.range(0, size).boxed().map(partition -> new LogPartition(name, partition)).collect(
                        Collectors.toList()));
    }

    /**
     * Returns {@code true} if the Log {@link #subscribe} method is supported.
     */
    boolean supportSubscribe();

    /**
     * Create a tailer for a consumer {@code group} and subscribe to multiple Logs. The partitions assignment is done
     * dynamically depending on the number of subscribers. The partitions can change during tailers life, this is called
     * a rebalancing. A listener can be used to be notified on assignment changes.
     * <p/>
     * A tailer is NOT thread safe.
     * <p/>
     * You should not mix {@link #createTailer} and {@code subscribe} usage using the same {@code group}.
     */
    <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener);

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
     * Returns all the Log names.
     */
    List<String> listAll();

    /**
     * List the consumer groups for a Log.
     */
    List<String> listConsumerGroups(String name);

    @Override
    void close();
}
