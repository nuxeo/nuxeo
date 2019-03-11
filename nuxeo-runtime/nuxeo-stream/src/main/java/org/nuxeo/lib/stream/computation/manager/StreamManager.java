/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */

package org.nuxeo.lib.stream.computation.manager;

import java.io.Externalizable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.appender.StreamAppender;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffsetStorage;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;

/**
 * @since 11.1
 */
public class StreamManager implements LogManager {

    protected Function<Record, String> identityExtractor;

    protected LogOffsetStorage storage;

    protected LogManager delegate;

    public StreamManager(LogManager manager, LogOffsetStorage storage, Function<Record, String> identityExtractor) {
        this.identityExtractor = identityExtractor;
        this.delegate = manager;
        this.storage = storage;
        if (identityExtractor == null) {
            identityExtractor = (record) -> record.getKey();
        }
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean createIfNotExists(String name, int size) {
        return delegate.createIfNotExists(name, size);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions) {
        return delegate.createTailer(group, partitions);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions,
            Codec<M> codec) {
        return delegate.createTailer(group, partitions, codec);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition) {
        return delegate.createTailer(group, partition);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition, Codec<M> codec) {
        return delegate.createTailer(group, partition, codec);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, String name) {
        return delegate.createTailer(group, name);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, String name, Codec<M> codec) {
        return delegate.createTailer(group, name, codec);
    }

    @Override
    public boolean delete(String name) {
        return delegate.delete(name);
    }

    @Override
    public boolean exists(String name) {
        return delegate.exists(name);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <M extends Externalizable> StreamAppender getAppender(String name, Codec<M> codec) {
        return new StreamAppender(delegate.getAppender(name, codec), storage, identityExtractor);
    }

    @Override
    public LogLag getLag(String name, String group) {
        return delegate.getLag(name, group);
    }

    @Override
    public List<LogLag> getLagPerPartition(String name, String group) {
        return delegate.getLagPerPartition(name, group);
    }

    @Override
    public <M extends Externalizable> Latency getLatency(String name, String group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return delegate.getLatency(name, group, codec, timestampExtractor, keyExtractor);
    }

    @Override
    public <M extends Externalizable> Latency getLatency(String name, String group,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return delegate.getLatency(name, group, timestampExtractor, keyExtractor);
    }

    @Override
    public <M extends Externalizable> List<Latency> getLatencyPerPartition(String name, String group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return delegate.getLatencyPerPartition(name, group, codec, timestampExtractor, keyExtractor);
    }

    @Override
    public <M extends Externalizable> List<Latency> getLatencyPerPartition(String name, String group,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return delegate.getLatencyPerPartition(name, group, timestampExtractor, keyExtractor);
    }

    @Override
    public List<String> listAll() {
        return delegate.listAll();
    }

    @Override
    public List<String> listConsumerGroups(String name) {
        return delegate.listConsumerGroups(name);
    }

    @Override
    public int size(String name) {
        return delegate.size(name);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener) {
        return delegate.subscribe(group, names, listener);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec) {
        return delegate.subscribe(group, names, listener, codec);
    }

    @Override
    public boolean supportSubscribe() {
        return delegate.supportSubscribe();
    }

}
