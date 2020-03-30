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

import java.io.Externalizable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.AbstractLogManager;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;

/**
 * @since 9.3
 */
public class KafkaLogManager extends AbstractLogManager {
    public static final String DISABLE_SUBSCRIBE_PROP = "subscribe.disable";

    public static final String DEFAULT_REPLICATION_FACTOR_PROP = "default.replication.factor";

    protected final KafkaUtils kUtils;

    protected final Properties producerProperties;

    protected final Properties consumerProperties;

    protected final Properties adminProperties;

    protected final String prefix;

    protected final short defaultReplicationFactor;

    protected final boolean disableSubscribe;

    protected final KafkaNamespace ns;

    /**
     * @deprecated since 10.2, zookeeper is not needed anymore, you need to remove the zkServers parameter.
     */
    @Deprecated
    public KafkaLogManager(String zkServers, String prefix, Properties producerProperties,
            Properties consumerProperties) {
        this(prefix, producerProperties, consumerProperties);
    }

    /**
     * @since 10.2
     */
    public KafkaLogManager(String prefix, Properties producerProperties, Properties consumerProperties) {
        this.prefix = (prefix != null) ? prefix : "";
        this.ns = new KafkaNamespace(this.prefix);
        disableSubscribe = Boolean.valueOf(consumerProperties.getProperty(DISABLE_SUBSCRIBE_PROP, "false"));
        defaultReplicationFactor = Short.parseShort(
                producerProperties.getProperty(DEFAULT_REPLICATION_FACTOR_PROP, "1"));
        this.producerProperties = normalizeProducerProperties(producerProperties);
        this.consumerProperties = normalizeConsumerProperties(consumerProperties);
        this.adminProperties = createAdminProperties(producerProperties);
        this.kUtils = new KafkaUtils(adminProperties);
    }

    @Override
    public void create(String name, int size) {
        kUtils.createTopic(ns.getTopicName(name), size, defaultReplicationFactor);
    }

    @Override
    protected int getSize(String name) {
        return kUtils.partitions(ns.getTopicName(name));
    }

    @Override
    public boolean exists(String name) {
        return kUtils.topicExists(ns.getTopicName(name));
    }

    @Override
    public <M extends Externalizable> CloseableLogAppender<M> createAppender(String name, Codec<M> codec) {
        return KafkaLogAppender.open(codec, ns, name, producerProperties, consumerProperties);
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doCreateTailer(Collection<LogPartition> partitions, String group,
            Codec<M> codec) {
        partitions.forEach(this::checkValidPartition);
        return KafkaLogTailer.createAndAssign(codec, ns, partitions, group, (Properties) consumerProperties.clone());
    }

    protected void checkValidPartition(LogPartition partition) {
        int partitions = kUtils.getNumberOfPartitions(ns.getTopicName(partition.name()));
        if (partition.partition() >= partitions) {
            throw new IllegalArgumentException("Partition out of bound " + partition + " max: " + partitions);
        }
    }

    public Properties getProducerProperties() {
        return producerProperties;
    }

    public Properties getConsumerProperties() {
        return consumerProperties;
    }

    public Properties getAdminProperties() {
        return adminProperties;
    }

    @Override
    public void close() {
        super.close();
        if (kUtils != null) {
            kUtils.close();
        }
    }

    @Override
    public boolean supportSubscribe() {
        return !disableSubscribe;
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doSubscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec) {
        return KafkaLogTailer.createAndSubscribe(codec, ns, names, group, (Properties) consumerProperties.clone(),
                listener);
    }

    protected Properties normalizeProducerProperties(Properties producerProperties) {
        Properties ret;
        if (producerProperties != null) {
            ret = (Properties) producerProperties.clone();
        } else {
            ret = new Properties();
        }
        ret.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        ret.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.BytesSerializer");
        ret.remove(DEFAULT_REPLICATION_FACTOR_PROP);
        return ret;
    }

    protected Properties normalizeConsumerProperties(Properties consumerProperties) {
        Properties ret;
        if (consumerProperties != null) {
            ret = (Properties) consumerProperties.clone();
        } else {
            ret = new Properties();
        }
        ret.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        ret.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.BytesDeserializer");
        ret.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ret.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ret.remove(DISABLE_SUBSCRIBE_PROP);
        return ret;
    }

    protected Properties createAdminProperties(Properties producerProperties) {
        Properties ret = new Properties();
        for (Map.Entry<Object, Object> prop : producerProperties.entrySet()) {
            switch (prop.getKey().toString()) {
            case ProducerConfig.ACKS_CONFIG:
            case ProducerConfig.BATCH_SIZE_CONFIG:
            case ProducerConfig.BUFFER_MEMORY_CONFIG:
            case ProducerConfig.COMPRESSION_TYPE_CONFIG:
            case ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG:
            case ProducerConfig.LINGER_MS_CONFIG:
            case ProducerConfig.MAX_BLOCK_MS_CONFIG:
            case ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG:
            case ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG:
            case DEFAULT_REPLICATION_FACTOR_PROP:
                // Skip non admin config properties to avoid warning on unused properties
                break;
            default:
                ret.put(prop.getKey(), prop.getValue());
            }
        }
        return ret;
    }

    @Override
    public List<LogLag> getLagPerPartition(String name, String group) {
        Properties props = (Properties) consumerProperties.clone();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, prefix + group);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "lag");
        // Prevents to create multiple consumers with the same client/group ids
        synchronized(KafkaLogManager.class) {
            try (KafkaConsumer<String, Bytes> consumer = new KafkaConsumer<>(props)) {
                List<TopicPartition> topicPartitions = consumer.partitionsFor(ns.getTopicName(name))
                        .stream()
                        .map(meta -> new TopicPartition(meta.topic(),
                                meta.partition()))
                        .collect(Collectors.toList());
                LogLag[] ret = new LogLag[topicPartitions.size()];
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
                for (TopicPartition topicPartition : topicPartitions) {
                    long committedOffset = 0L;
                    OffsetAndMetadata committed = consumer.committed(topicPartition);
                    if (committed != null) {
                        committedOffset = committed.offset();
                    }
                    Long endOffset = endOffsets.get(topicPartition);
                    if (endOffset == null) {
                        endOffset = 0L;
                    }
                    ret[topicPartition.partition()] = new LogLag(committedOffset, endOffset);
                }
                return Arrays.asList(ret);
            }
        }
    }

    @Override
    public List<String> listAll() {
        return kUtils.listTopics()
                     .stream()
                     .filter(name -> name.startsWith(prefix))
                     .map(ns::getLogName)
                     .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "KafkaLogManager{" + "producerProperties=" + filterDisplayedProperties(producerProperties)
                + ", consumerProperties=" + filterDisplayedProperties(consumerProperties) + ", prefix='" + prefix + '\''
                + '}';
    }

    protected String filterDisplayedProperties(Properties properties) {
        String ret = properties.toString();
        if (ret.indexOf("password") < 0) {
            return ret;
        }
        return ret.replaceAll("password=.[^\\\"\\;\\,\\ ]*", "password=****");
    }

    @Override
    public List<String> listConsumerGroups(String name) {
        String topic = ns.getTopicName(name);
        if (!exists(name)) {
            throw new IllegalArgumentException("Unknown Log: " + name);
        }
        return kUtils.listConsumers(topic)
                     .stream()
                     .filter(group -> group.startsWith(prefix))
                     .map(ns::getGroup)
                     .collect(Collectors.toList());
    }

}
