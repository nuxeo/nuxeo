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
package org.nuxeo.lib.stream.log.kafka;

import static java.util.stream.Collectors.toMap;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.Externalizable;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.utils.Bytes;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;

/**
 * @since 9.3
 */
public class KafkaLogTailer<M extends Externalizable> implements LogTailer<M>, ConsumerRebalanceListener {
    private static final Log log = LogFactory.getLog(KafkaLogTailer.class);

    protected final String group;

    protected final Map<TopicPartition, Long> lastOffsets = new HashMap<>();

    protected final Map<TopicPartition, Long> lastCommittedOffsets = new HashMap<>();

    protected final Queue<ConsumerRecord<String, Bytes>> records = new LinkedList<>();

    protected final KafkaNamespace ns;

    protected final Codec<M> codec;

    protected final Codec<M> decodeCodec;

    protected KafkaConsumer<String, Bytes> consumer;

    protected String id;

    protected Collection<TopicPartition> topicPartitions;

    protected Collection<LogPartition> partitions;

    // keep track of all tailers on the same namespace index even from different mq
    protected boolean closed;

    protected Collection<String> names;

    protected RebalanceListener listener;

    protected boolean isRebalanced;

    protected boolean consumerMoved;

    protected static final AtomicInteger CONSUMER_CLIENT_ID_SEQUENCE = new AtomicInteger(1);

    protected KafkaLogTailer(Codec<M> codec, KafkaNamespace ns, String group, Properties consumerProps) {
        this.codec = codec;
        if (NO_CODEC.equals(codec)) {
            this.decodeCodec = new SerializableCodec<>();
        } else {
            this.decodeCodec = codec;
        }
        Objects.requireNonNull(group);
        this.ns = ns;
        this.group = group;
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, ns.getKafkaGroup(group));
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, group + "-" + CONSUMER_CLIENT_ID_SEQUENCE.getAndIncrement());
        this.consumer = new KafkaConsumer<>(consumerProps);
    }

    @SuppressWarnings("squid:S2095")
    public static <M extends Externalizable> KafkaLogTailer<M> createAndAssign(Codec<M> codec, KafkaNamespace ns,
            Collection<LogPartition> partitions, String group, Properties consumerProps) {
        KafkaLogTailer<M> ret = new KafkaLogTailer<>(codec, ns, group, consumerProps);
        ret.id = buildId(ret.group, partitions);
        ret.partitions = partitions;
        ret.topicPartitions = partitions.stream()
                                        .map(partition -> new TopicPartition(ns.getTopicName(partition.name()),
                                                partition.partition()))
                                        .collect(Collectors.toList());
        ret.consumer.assign(ret.topicPartitions);
        log.debug(String.format("Created tailer with assignments: %s using prefix: %s", ret.id, ns));
        return ret;
    }

    @SuppressWarnings("squid:S2095")
    public static <M extends Externalizable> KafkaLogTailer<M> createAndSubscribe(Codec<M> codec, KafkaNamespace ns,
            Collection<String> names, String group, Properties consumerProps, RebalanceListener listener) {
        KafkaLogTailer<M> ret = new KafkaLogTailer<>(codec, ns, group, consumerProps);
        ret.id = buildSubscribeId(ret.group, names);
        ret.names = names;
        Collection<String> topics = names.stream().map(ns::getTopicName).collect(Collectors.toList());
        ret.listener = listener;
        ret.consumer.subscribe(topics, ret);
        ret.partitions = Collections.emptyList();
        ret.topicPartitions = Collections.emptyList();
        log.debug(String.format("Created tailer with subscription: %s using prefix: %s", ret.id, ns));
        return ret;
    }

    protected static String buildId(String group, Collection<LogPartition> partitions) {
        return group + ":" + partitions.stream().map(LogPartition::toString).collect(Collectors.joining("|"));
    }

    protected static String buildSubscribeId(String group, Collection<String> names) {
        return group + ":" + String.join("|", names);
    }

    @Override
    public LogRecord<M> read(Duration timeout) throws InterruptedException {
        if (closed) {
            throw new IllegalStateException("The tailer has been closed.");
        }
        if (records.isEmpty()) {
            int items = poll(timeout);
            if (isRebalanced) {
                isRebalanced = false;
                log.debug("Rebalance happens during poll, raising exception");
                throw new RebalanceException("Partitions has been rebalanced");
            }
            if (items == 0) {
                if (log.isTraceEnabled()) {
                    log.trace("No data " + id + " after " + timeout.toMillis() + " ms");
                }
                return null;
            }
        }
        ConsumerRecord<String, Bytes> record = records.poll();
        lastOffsets.put(new TopicPartition(record.topic(), record.partition()), record.offset());
        M value = decodeCodec.decode(record.value().get());
        LogPartition partition = LogPartition.of(ns.getLogName(record.topic()), record.partition());
        LogOffset offset = new LogOffsetImpl(partition, record.offset());
        consumerMoved = false;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Read from %s/%s, key: %s, value: %s", offset, group, record.key(), value));
        }
        return new LogRecord<>(value, offset);
    }

    protected int poll(Duration timeout) throws InterruptedException {
        records.clear();
        try {
            for (ConsumerRecord<String, Bytes> record : consumer.poll(timeout)) {
                if (log.isDebugEnabled() && records.isEmpty()) {
                    log.debug("Poll first record: " + ns.getLogName(record.topic()) + ":" + record.partition() + ":+"
                            + record.offset());
                }
                records.add(record);
            }
        } catch (org.apache.kafka.common.errors.InterruptException e) {
            // the thread is already interrupted
            throw new InterruptedException(e.getMessage());
        } catch (WakeupException e) {
            log.debug("Receiving wakeup from another thread to close the tailer");
            close();
            throw new IllegalStateException("poll interrupted because tailer has been closed");
        }
        if (log.isDebugEnabled()) {
            String msg = "Polling " + id + " returns " + records.size() + " records";
            if (records.isEmpty()) {
                log.trace(msg);
            } else {
                log.debug(msg);
            }
        }
        return records.size();
    }

    @Override
    public void toEnd() {
        log.debug("toEnd: " + id);
        lastOffsets.clear();
        records.clear();
        consumer.seekToEnd(Collections.emptyList());
        consumerMoved = true;
    }

    @Override
    public void toStart() {
        log.debug("toStart: " + id);
        lastOffsets.clear();
        records.clear();
        consumer.seekToBeginning(Collections.emptyList());
        consumerMoved = true;
    }

    @Override
    public void toLastCommitted() {
        if (log.isDebugEnabled()) {
            log.debug("toLastCommitted tailer: " + id);
        }
        String msg = consumer.assignment()
                             .stream()
                             .map(tp -> String.format("%s-%02d:+%d", ns.getLogName(tp.topic()), tp.partition(),
                                     toLastCommitted(tp)))
                             .collect(Collectors.joining("|"));
        if (msg.length() > 0 && log.isInfoEnabled()) {
            log.info("toLastCommitted offsets: " + group + ":" + msg);
        }
        lastOffsets.clear();
        records.clear();
        consumerMoved = false;
    }

    protected long toLastCommitted(TopicPartition topicPartition) {
        Long offset = lastCommittedOffsets.get(topicPartition);
        if (offset == null) {
            OffsetAndMetadata offsetMeta = consumer.committed(topicPartition);
            if (offsetMeta != null) {
                offset = offsetMeta.offset();
            }
        }
        if (offset != null) {
            consumer.seek(topicPartition, offset);
        } else {
            consumer.seekToBeginning(Collections.singletonList(topicPartition));
            offset = consumer.position(topicPartition);
        }
        lastCommittedOffsets.put(topicPartition, offset);
        if (log.isDebugEnabled()) {
            log.debug(String.format(" toLastCommitted: %s-%02d:+%d", ns.getLogName(topicPartition.topic()),
                    topicPartition.partition(), offset));
        }
        return offset;
    }

    @Override
    public void seek(LogOffset offset) {
        log.debug("Seek to: " + offset.offset() + " from tailer: " + id);
        TopicPartition topicPartition = new TopicPartition(ns.getTopicName(offset.partition().name()),
                offset.partition().partition());
        consumer.seek(topicPartition, offset.offset());
        lastOffsets.remove(topicPartition);
        int partition = topicPartition.partition();
        records.removeIf(rec -> rec.partition() == partition);
        consumerMoved = true;
    }

    @Override
    public void reset() {
        // we just commit the first offset
        log.info("Reset committed offsets for all assigned partitions: " + topicPartitions + " tailer: " + id);
        Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(topicPartitions);
        Map<TopicPartition, OffsetAndMetadata> offsetToCommit = new HashMap<>();
        beginningOffsets.forEach((tp, offset) -> offsetToCommit.put(tp, new OffsetAndMetadata(offset)));
        consumer.commitSync(offsetToCommit);
        lastCommittedOffsets.clear();
        toLastCommitted();
    }

    @Override
    public void reset(LogPartition partition) {
        log.info("Reset committed offset for partition: " + partition + " tailer: " + id);
        TopicPartition topicPartition = new TopicPartition(ns.getTopicName(partition.name()), partition.partition());
        Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(Collections.singleton(topicPartition));
        Map<TopicPartition, OffsetAndMetadata> offsetToCommit = new HashMap<>();
        beginningOffsets.forEach((tp, offset) -> offsetToCommit.put(tp, new OffsetAndMetadata(offset)));
        consumer.commitSync(offsetToCommit);
        lastCommittedOffsets.remove(topicPartition);
        seek(new LogOffsetImpl(partition, beginningOffsets.get(topicPartition)));
    }

    @Override
    public LogOffset offsetForTimestamp(LogPartition partition, long timestamp) {
        TopicPartition topicPartition = new TopicPartition(ns.getTopicName(partition.name()), partition.partition());
        Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes = consumer.offsetsForTimes(
                Collections.singletonMap(topicPartition, timestamp));
        if (offsetsForTimes.size() == 1) {
            OffsetAndTimestamp offsetAndTimestamp = offsetsForTimes.get(topicPartition);
            if (offsetAndTimestamp != null) {
                return new LogOffsetImpl(partition, offsetAndTimestamp.offset());
            }
        }
        return null;
    }

    @Override
    public void commit() {
        if (consumerMoved) {
            forceCommit();
            return;
        }
        Map<TopicPartition, OffsetAndMetadata> offsetToCommit = new HashMap<>();
        lastOffsets.forEach((tp, offset) -> offsetToCommit.put(tp, new OffsetAndMetadata(offset + 1)));
        lastOffsets.clear();
        if (offsetToCommit.isEmpty()) {
            return;
        }
        consumer.commitSync(offsetToCommit);
        offsetToCommit.forEach((topicPartition, offset) -> lastCommittedOffsets.put(topicPartition, offset.offset()));
        if (log.isDebugEnabled()) {
            String msg = offsetToCommit.entrySet()
                                       .stream()
                                       .map(entry -> String.format("%s-%02d:+%d", ns.getLogName(entry.getKey().topic()),
                                               entry.getKey().partition(), entry.getValue().offset()))
                                       .collect(Collectors.joining("|"));
            log.debug("Committed offsets  " + group + ":" + msg);
        }
    }

    /**
     * Commits the consumer at its current position regardless of lastOffsets or lastCommittedOffsets
     */
    protected void forceCommit() {
        log.info("Force commit after a move");

        Map<TopicPartition, OffsetAndMetadata> offsets = topicPartitions.stream().collect(
                toMap(tp -> tp, tp -> new OffsetAndMetadata(consumer.position(tp))));
        consumer.commitSync(offsets);
        offsets.forEach((topicPartition, offset) -> lastCommittedOffsets.put(topicPartition, offset.offset()));
        consumerMoved = false;
        lastOffsets.clear();
    }

    @Override
    public LogOffset commit(LogPartition partition) {
        TopicPartition topicPartition = new TopicPartition(ns.getTopicName(partition.name()), partition.partition());
        Long offset = lastOffsets.get(topicPartition);
        if (offset == null) {
            if (log.isDebugEnabled()) {
                log.debug("unchanged partition, nothing to commit: " + partition);
            }
            return null;
        }
        offset += 1;
        consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(offset)));
        LogOffset ret = new LogOffsetImpl(partition, offset);
        if (log.isInfoEnabled()) {
            log.info("Committed: " + offset + "/" + group);
        }
        return ret;
    }

    @Override
    public Collection<LogPartition> assignments() {
        return partitions;
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public Codec<M> getCodec() {
        return codec;
    }

    @SuppressWarnings("squid:S1181")
    @Override
    public void close() {
        if (consumer != null) {
            log.debug("Closing tailer: " + id);
            try {
                // calling wakeup enable to terminate consumer blocking on poll call
                consumer.close();
            } catch (ConcurrentModificationException e) {
                // closing from another thread raise this exception, try to wakeup the owner
                log.info("Closing tailer from another thread, send wakeup");
                consumer.wakeup();
                return;
            } catch (InterruptException | IllegalStateException e) {
                // this happens if the consumer has already been closed or if it is closed from another
                // thread.
                log.warn("Discard error while closing consumer: ", e);
            } catch (Throwable t) {
                log.error("interrupted", t);
            }
            consumer = null;
        }
        closed = true;
    }

    @Override
    public String toString() {
        return "KafkaLogTailer{" + "ns='" + ns + '\'' + ", id=" + id + ", closed=" + closed + ", codec=" + codec + '}';
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        Collection<LogPartition> revoked = partitions.stream()
                                                     .map(tp -> LogPartition.of(ns.getLogName(tp.topic()),
                                                             tp.partition()))
                                                     .collect(Collectors.toList());
        log.info(String.format("Rebalance revoked: %s", revoked));
        id += "-revoked";
        if (listener != null) {
            listener.onPartitionsRevoked(revoked);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> newPartitions) {
        partitions = newPartitions.stream()
                                  .map(tp -> LogPartition.of(ns.getLogName(tp.topic()), tp.partition()))
                                  .collect(Collectors.toList());
        topicPartitions = newPartitions;
        id = buildId(group, partitions);
        lastCommittedOffsets.clear();
        lastOffsets.clear();
        records.clear();
        isRebalanced = true;
        log.info(String.format("Rebalance assigned: %s", partitions));
        if (listener != null) {
            listener.onPartitionsAssigned(partitions);
        }
    }

}
