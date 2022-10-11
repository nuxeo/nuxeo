/*
 * (C) Copyright 2022 Nuxeo.
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
 */
package org.nuxeo.lib.stream.log.mem;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.AbstractLogManager;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;

/**
 * Memory implementation of LogManager.
 */
public class MemLogManager extends AbstractLogManager {

    private static final MemLogs INSTANCE = new MemLogs();

    private MemLogs memLogs;

    public MemLogManager() {
        memLogs = INSTANCE;
    }

    public static void clear() {
        INSTANCE.clear();
    }

    @Override
    public boolean exists(Name name) {
        return memLogs.exists(name);
    }

    @Override
    public void create(Name name, int size) {
        memLogs.createLog(name, size);
    }

    @Override
    public int getSize(Name name) {
        return memLogs.getLog(name).size();
    }

    @Override
    public boolean delete(Name name) {
        return memLogs.deleteLog(name);
    }

    @Override
    public List<LogLag> getLagPerPartition(Name name, Name group) {
        MemLog log = memLogs.getLog(name);
        int size = log.size();
        List<LogLag> lags = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            MemLogPartition partition = log.getPartition(i);
            LogLag lag = LogLag.of(partition.committed(group), partition.size());
            lags.add(lag);
        }
        return lags;
    }

    @Override
    public String toString() {
        return "MemLogManager{}";
    }

    @Override
    public List<Name> listAllNames() {
        return memLogs.listAllNames();
    }

    @Override
    public List<Name> listConsumerGroups(Name name) {
        return memLogs.getLogOptional(name).map(MemLog::getGroups).orElse(List.of());
    }

    @Override
    public <M extends Externalizable> CloseableLogAppender<M> createAppender(Name name, Codec<M> codec) {
        return new MemLogAppender<M>(memLogs, name, codec);
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doCreateTailer(Collection<LogPartition> partitions, Name group,
            Codec<M> codec) {
        List<MemLogTailer<M>> tailers = partitions.stream() //
                                                  .map(p -> createMemLogTailer(p, group, codec))
                                                  .toList();
        if (tailers.size() == 1) {
            return tailers.iterator().next();
        }
        return new MemCompoundLogTailer<>(tailers, group);
    }

    private <M extends Externalizable> MemLogTailer<M> createMemLogTailer(LogPartition p, Name group, Codec<M> codec) {
        return (MemLogTailer<M>) ((MemLogAppender<M>) getAppender(p.name(), codec)).createTailer(p, group, codec);
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doSubscribe(Name group, Collection<Name> names,
            RebalanceListener listener, Codec<M> codec) {
        throw new UnsupportedOperationException("subscribe is not supported by Mem implementation");
    }

}
