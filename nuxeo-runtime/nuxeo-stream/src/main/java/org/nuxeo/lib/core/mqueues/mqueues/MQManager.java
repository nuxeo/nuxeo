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
package org.nuxeo.lib.core.mqueues.mqueues;


import java.io.Externalizable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Manage MQueue and give access to appender and tailers.
 *
 * Closing the MQManager will also close all its appenders and tailers.
 *
 * @since 9.2
 */
public interface MQManager extends AutoCloseable {

    /**
     * Returns {@code true} if a MQueue with this {@code name} exists.
     */
    boolean exists(String name);

    /**
     * Creates a new MQueue with {@code size} partitions if the MQueue does not exists.
     * Returns true it the MQueue has been created.
     */
    boolean createIfNotExists(String name, int size);

    /**
     * Try to delete a MQueue.
     * Returns true if successfully deleted, might not be possible depending on the implementation.
     */
    boolean delete(String name);

    /**
     * Get an appender for the MQueue named {@code name}.
     * An appender is thread safe.
     */
    <M extends Externalizable> MQAppender<M> getAppender(String name);

    /**
     * Create a tailer for a consumer {@code group} and assign a single {@code partition}.
     * A tailer is NOT thread safe.
     */
    <M extends Externalizable> MQTailer<M> createTailer(String group, MQPartition partition);

    /**
     * Create a tailer for a consumer {@code group} and assign multiple {@code partitions}.
     * Note that {@code partitions} can be from different MQueues.
     *
     * A tailer is NOT thread safe.
     */
    <M extends Externalizable> MQTailer<M> createTailer(String group, Collection<MQPartition> partitions);

    /**
     * Create a tailer for a consumer {@code group} and assign all {@code partitions} of a MQueue.
     * A tailer is NOT thread safe.
     *
     * @since 9.3
     */
    default <M extends Externalizable> MQTailer<M> createTailer(String group, String name) {
        int size = getAppender(name).size();
        return createTailer(group,
                IntStream.range(0, size).boxed().map(partition -> new MQPartition(name, partition))
                        .collect(Collectors.toList()));
    }

    /**
     * Returns {@code true} if the MQueue {@link #subscribe} method is supported.
     */
    boolean supportSubscribe();

    /**
     * Create a tailer for a consumer {@code group} and subscribe to multiple MQueues.
     * The partitions assignment is done dynamically depending on the number of subscribers.
     * The partitions can change during tailers life, this is called a rebalancing.
     * A listener can be used to be notified on assignment changes.
     * <p/>
     * A tailer is NOT thread safe.
     * <p/>
     * You should not mix {@link #createTailer} and {@code subscribe} usage using the same {@code group}.
     */
    <M extends Externalizable> MQTailer<M> subscribe(String group, Collection<String> names, MQRebalanceListener listener);

    /**
     * Returns the lag between consumer {@code group} and the producers for each partition.
     * The result list is ordered, for instance index 0 is lag for partition 0.
     *
     * @since 9.3
     */
    List<MQLag> getLagPerPartition(String name, String group);

    /**
     * Returns the lag between consumer {@code group} and producers for a MQueue.
     */
    default MQLag getLag(String name, String group) {
        final long[] end = {0};
        final long[] pos = {Long.MAX_VALUE};
        final long[] lag = {0};
        final long[] endMessages = {0};
        getLagPerPartition(name, group).forEach(item -> {
            if (item.lowerOffset() > 0) {
                pos[0] = min(pos[0], item.lowerOffset());
            }
            end[0] = max(end[0], item.upperOffset());
            endMessages[0] += item.upper();
            lag[0] += item.lag();
        });
        return new MQLag(pos[0], end[0], lag[0], endMessages[0]);
    }

    /**
     * Returns all the MQueue names.
     *
     * @since 9.3
     */
    List<String> listAll();

    /**
     * List the consumer groups for a MQueue.
     *
     * @since 9.3
     */
    List<String> listConsumerGroups(String name);

}
