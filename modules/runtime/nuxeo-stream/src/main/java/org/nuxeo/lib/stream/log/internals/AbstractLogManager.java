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

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.Externalizable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;

public abstract class AbstractLogManager implements LogManager {
    protected final Map<String, CloseableLogAppender> appenders = new ConcurrentHashMap<>();

    protected final Map<LogPartitionGroup, LogTailer> tailersAssignments = new ConcurrentHashMap<>();

    // this define a concurrent set of tailers
    protected final Set<LogTailer> tailers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected abstract void create(String name, int size);

    protected abstract int getSize(String name);

    protected abstract <M extends Externalizable> CloseableLogAppender<M> createAppender(String name, Codec<M> codec);

    protected abstract <M extends Externalizable> LogTailer<M> doCreateTailer(Collection<LogPartition> partitions,
            String group, Codec<M> codec);

    protected abstract <M extends Externalizable> LogTailer<M> doSubscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec);

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
    public int size(String name) {
        if (appenders.containsKey(name)) {
            return appenders.get(name).size();
        }
        return getSize(name);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(String group, Collection<LogPartition> partitions,
            Codec<M> codec) {
        Objects.requireNonNull(codec);
        partitions.forEach(partition -> checkInvalidAssignment(group, partition));
        Codec<M> tailerCodec = NO_CODEC.equals(codec) ? guessCodec(partitions) : codec;
        partitions.forEach(partition -> checkInvalidCodec(partition, tailerCodec));
        LogTailer<M> ret = doCreateTailer(partitions, group, tailerCodec);
        partitions.forEach(partition -> tailersAssignments.put(new LogPartitionGroup(group, partition), ret));
        tailers.add(ret);
        return ret;
    }

    @SuppressWarnings("unchecked")
    protected <M extends Externalizable> Codec<M> guessCodec(Collection<LogPartition> partitions) {
        for (LogPartition partition : partitions) {
            if (appenders.containsKey(partition.name())) {
                return (Codec<M>) getAppender(partition.name()).getCodec();
            }
        }
        return NO_CODEC;
    }

    @Override
    public boolean supportSubscribe() {
        return false;
    }

    @Override
    public <M extends Externalizable> LogTailer<M> subscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec) {
        Objects.requireNonNull(codec);
        LogTailer<M> ret = doSubscribe(group, names, listener, codec);
        tailers.add(ret);
        return ret;
    }

    protected void checkInvalidAssignment(String group, LogPartition partition) {
        LogPartitionGroup key = new LogPartitionGroup(group, partition);
        LogTailer<?> ret = tailersAssignments.get(key);
        if (ret != null && !ret.closed()) {
            throw new IllegalArgumentException(
                    "Tailer for this partition already created: " + partition + ", group: " + group);
        }
        if (!exists(partition.name())) {
            throw new IllegalArgumentException("Tailer with unknown Log name: " + partition.name());
        }
    }

    protected <M extends Externalizable> void checkInvalidCodec(LogPartition partition, Codec<M> codec) {
        if (appenders.containsKey(partition.name())) {
            getAppender(partition.name(), codec);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M extends Externalizable> LogAppender<M> getAppender(String name, Codec<M> codec) {
        LogAppender<M> ret = appenders.computeIfAbsent(name, n -> {
            if (exists(n)) {
                return createAppender(n, codec);
            }
            throw new IllegalArgumentException("Unknown Log name: " + n);
        });
        if (NO_CODEC.equals(codec) || sameCodec(ret.getCodec(), codec)) {
            return ret;
        }
        throw new IllegalArgumentException(String.format(
                "The appender for Log %s exists and expecting codec: %s, cannot use a different codec: %s", name,
                ret.getCodec(), codec));
    }

    protected <M extends Externalizable> boolean sameCodec(Codec<M> codec1, Codec<M> codec2) {
        return codec1 == codec2
                || !NO_CODEC.equals(codec1) && !NO_CODEC.equals(codec2) && codec1.getClass().isInstance(codec2);
    }

    @Override
    public <M extends Externalizable> List<Latency> getLatencyPerPartition(String name, String group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        long now = System.currentTimeMillis();
        List<LogLag> lags = getLagPerPartition(name, group);
        List<Latency> ret = new ArrayList<>(lags.size());
        int partition = 0;
        for (LogLag lag : lags) {
            if (lag.upper() == 0 || lag.lower() == 0 || lag.lag() == 0) {
                // empty partition or the group has not consumed any message or there is no lag
                ret.add(new Latency(0, now, lag, null));
                partition++;
                continue;
            }
            // the committed offset point to the next record to process, here we want the last committed offset
            // which is the previous one
            LogOffset offset = new LogOffsetImpl(name, partition, lag.lowerOffset() - 1);
            try (LogTailer<M> tailer = createTailer("tools", offset.partition(), codec)) {
                tailer.seek(offset);
                LogRecord<M> record = tailer.read(Duration.ofSeconds(1));
                if (record == null) {
                    // the beginning of the partition is not necessary offset 0 after retention policy is applied
                    ret.add(new Latency(0, now, lag, null));
                } else {
                    long timestamp = timestampExtractor.apply(record.message());
                    String key = keyExtractor.apply(record.message());
                    ret.add(new Latency(timestamp, now, lag, key));
                }
            } catch (ClassCastException e) {
                throw new IllegalStateException("Unexpected record type" + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StreamRuntimeException(e);
            }
            partition++;
        }
        return ret;
    }

    @Override
    public void close() {
        appenders.values().stream().filter(Objects::nonNull).forEach(CloseableLogAppender::close);
        appenders.clear();
        tailers.stream().filter(Objects::nonNull).forEach(LogTailer::close);
        tailers.clear();
        tailersAssignments.clear();
    }

}
