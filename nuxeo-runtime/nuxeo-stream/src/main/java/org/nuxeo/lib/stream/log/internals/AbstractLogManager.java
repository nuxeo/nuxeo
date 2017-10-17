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
package org.nuxeo.lib.stream.log.internals;

import java.io.Externalizable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;

public abstract class AbstractLogManager implements LogManager {
    protected final Map<String, LogAppender> appenders = new ConcurrentHashMap<>();

    protected final Map<LogPartitionGroup, LogTailer> tailersAssignments = new ConcurrentHashMap<>();

    protected final Set<LogTailer> tailers = Collections.newSetFromMap(new ConcurrentHashMap<LogTailer, Boolean>());

    protected abstract void create(String name, int size);

    protected abstract <M extends Externalizable> LogAppender<M> createAppender(String name);

    protected abstract <M extends Externalizable> LogTailer<M> acquireTailer(Collection<LogPartition> partitions,
            String group);

    protected abstract <M extends Externalizable> LogTailer<M> doSubscribe(String group, Collection<String> names,
            RebalanceListener listener);

    @Override
    public synchronized boolean createIfNotExists(String name, int size) {
        if (!exists(name)) {
            create(name, size);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(String name) {
        return false;
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions) {
        partitions.forEach(partition -> checkTailerForPartition(group, partition));
        LogTailer<M> ret = acquireTailer(partitions, group);
        partitions.forEach(partition -> tailersAssignments.put(new LogPartitionGroup(group, partition), ret));
        tailers.add(ret);
        return ret;
    }

    @Override
    public boolean supportSubscribe() {
        return false;
    }

    @Override
    public <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener) {
        LogTailer<M> ret = doSubscribe(group, names, listener);
        tailers.add(ret);
        return ret;
    }

    protected void checkTailerForPartition(String group, LogPartition partition) {
        LogPartitionGroup key = new LogPartitionGroup(group, partition);
        LogTailer ret = tailersAssignments.get(key);
        if (ret != null && !ret.closed()) {
            throw new IllegalArgumentException(
                    "Tailer for this partition already created: " + partition + ", group: " + group);
        }
        if (!exists(partition.name())) {
            throw new IllegalArgumentException("Tailer with unknown Log name: " + partition.name());
        }
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, LogPartition partition) {
        return createTailer(group, Collections.singletonList(partition));
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <M extends Externalizable> LogAppender<M> getAppender(String name) {
        if (!appenders.containsKey(name) || appenders.get(name).closed()) {
            if (exists(name)) {
                LogAppender<M> appender = createAppender(name);
                appenders.put(name, appender);
            } else {
                throw new IllegalArgumentException("unknown Log name: " + name);
            }
        }
        return (LogAppender<M>) appenders.get(name);
    }

    @Override
    public void close() {
        appenders.values().stream().filter(Objects::nonNull).forEach(LogAppender::close);
        appenders.clear();
        tailers.stream().filter(Objects::nonNull).forEach(LogTailer::close);
        tailers.clear();
        tailersAssignments.clear();
    }
}
