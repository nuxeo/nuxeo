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

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.Externalizable;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.NameResolver;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;

/**
 * Apache Kafka implementation of Log.
 *
 * @since 9.3
 */
public class KafkaLogAppender<M extends Externalizable> implements CloseableLogAppender<M> {
    private static final Log log = LogFactory.getLog(KafkaLogAppender.class);

    protected final String topic;

    protected final Properties consumerProps;

    protected final Properties producerProps;

    protected final int size;

    // keep track of created tailers to make sure they are closed
    protected final ConcurrentLinkedQueue<KafkaLogTailer<M>> tailers = new ConcurrentLinkedQueue<>();

    protected final Name name;

    protected final Codec<M> codec;

    protected final Codec<M> encodingCodec;

    protected final NameResolver resolver;

    protected KafkaProducer<String, Bytes> producer;

    protected boolean closed;

    protected static final AtomicInteger PRODUCER_CLIENT_ID_SEQUENCE = new AtomicInteger(1);

    private KafkaLogAppender(Codec<M> codec, NameResolver resolver, Name name, Properties producerProperties,
            Properties consumerProperties) {
        Objects.requireNonNull(codec);
        this.codec = codec;
        this.resolver = resolver;
        if (NO_CODEC.equals(codec)) {
            this.encodingCodec = new SerializableCodec<>();
        } else {
            this.encodingCodec = codec;
        }
        this.name = name;
        this.topic = resolver.getId(name);
        this.producerProps = producerProperties;
        this.consumerProps = consumerProperties;
        producerProps.setProperty(ProducerConfig.CLIENT_ID_CONFIG,
                resolver.getId(this.name) + "-" + PRODUCER_CLIENT_ID_SEQUENCE.getAndIncrement());
        this.producer = new KafkaProducer<>(this.producerProps);
        this.size = producer.partitionsFor(topic).size();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Created appender: %s on topic: %s with %d partitions", this.name, topic, size));
        }
    }

    public static <M extends Externalizable> KafkaLogAppender<M> open(Codec<M> codec, NameResolver resolver, Name name,
            Properties producerProperties, Properties consumerProperties) {
        return new KafkaLogAppender<>(codec, resolver, name, producerProperties, consumerProperties);
    }

    @Override
    public Name name() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public LogOffset append(String key, M message) {
        Objects.requireNonNull(key);
        int partition = (key.hashCode() & 0x7fffffff) % size;
        return append(partition, key, message);
    }

    @Override
    public LogOffset append(int partition, M message) {
        String key = String.valueOf(partition);
        return append(partition, key, message);
    }

    public LogOffset append(int partition, String key, M message) {
        Bytes value = Bytes.wrap(encodingCodec.encode(message));
        ProducerRecord<String, Bytes> record = new ProducerRecord<>(topic, partition, key, value);
        Future<RecordMetadata> future = producer.send(record);
        RecordMetadata result;
        try {
            result = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException("Unable to send record: " + record, e);
        } catch (ExecutionException e) {
            throw new StreamRuntimeException("Unable to send record: " + record, e);
        }
        LogOffset ret = new LogOffsetImpl(name, partition, result.offset());
        if (log.isDebugEnabled()) {
            int len = record.value().get().length;
            log.debug(String.format("Append to %s-%02d:+%d, len: %d, key: %s, value: %s", name, partition, ret.offset(),
                    len, key, message));
        }
        return ret;
    }

    @Override
    public boolean waitFor(LogOffset offset, Name group, Duration timeout) throws InterruptedException {
        boolean ret = false;
        if (!name.equals(offset.partition().name())) {
            throw new IllegalArgumentException(name + " can not wait for an offset with a different Log: " + offset);
        }
        TopicPartition topicPartition = new TopicPartition(topic, offset.partition().partition());
        try {
            ret = isProcessed(group, topicPartition, offset.offset());
            if (ret) {
                return true;
            }
            long timeoutMs = timeout.toMillis();
            long deadline = System.currentTimeMillis() + timeoutMs;
            long delay = Math.min(100, timeoutMs);
            while (!ret && System.currentTimeMillis() < deadline) {
                Thread.sleep(delay);
                ret = isProcessed(group, topicPartition, offset.offset());
            }
            return ret;
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("waitFor " + offset + "/" + group + " returns: " + ret);
            }
        }
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public String toString() {
        return "KafkaLogAppender{" + "name='" + name + '\'' + ", size=" + size + ", closed=" + closed
                + ", codec=" + codec + '}';
    }

    @Override
    public Codec<M> getCodec() {
        return codec;
    }

    protected boolean isProcessed(Name group, TopicPartition topicPartition, long offset) {
        // TODO: find a better way, this is expensive to create a consumer each time
        // but this is needed, an open consumer is not properly updated
        Properties props = (Properties) consumerProps.clone();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, resolver.getId(group));
        try (KafkaConsumer<String, Bytes> consumer = new KafkaConsumer<>(props)) {
            consumer.assign(Collections.singletonList(topicPartition));
            long last = consumer.position(topicPartition);
            boolean ret = last > 0 && last > offset;
            if (log.isDebugEnabled()) {
                log.debug("isProcessed " + topicPartition.topic() + ":" + topicPartition.partition() + "/" + group
                        + ":+" + offset + "? " + ret + ", current position: " + last);
            }
            return ret;
        }
    }

    @Override
    public void close() {
        log.debug("Closing appender: " + name);
        tailers.stream().filter(Objects::nonNull).forEach(tailer -> {
            try {
                tailer.close();
            } catch (Exception e) {
                log.error("Failed to close tailer: " + tailer);
            }
        });
        tailers.clear();
        if (producer != null) {
            producer.close();
            producer = null;
        }
        closed = true;
    }
}
