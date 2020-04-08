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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogConfig;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.AbstractLogManager;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;

/**
 * @since 9.3
 */
public class KafkaLogManager extends AbstractLogManager {
    public static final String DISABLE_SUBSCRIBE_PROP = "subscribe.disable";

    protected final List<KafkaLogConfig> configs;

    protected final KafkaLogConfig defaultConfig;

    protected final Map<KafkaLogConfig, KafkaUtils> kUtils = new HashMap<>();

    /**
     * @since 10.2
     */
    public KafkaLogManager(String prefix, Properties producerProperties, Properties consumerProperties) {
        this(Collections.singletonList(new KafkaLogConfig(true, Collections.emptyList(), prefix, null,
                producerProperties, consumerProperties)));
    }

    /**
     * @since 11.1
     */
    public KafkaLogManager(List<KafkaLogConfig> kafkaConfigs) {
        if (kafkaConfigs == null && kafkaConfigs.isEmpty()) {
            throw new IllegalArgumentException("config required");
        }
        this.configs = kafkaConfigs;
        this.defaultConfig = findDefaultConfig();
        configs.forEach(config -> kUtils.put(config, new KafkaUtils(config.getAdminProperties())));
    }

    protected KafkaLogConfig findDefaultConfig() {
        List<KafkaLogConfig> defaultConfigs = configs.stream()
                                                     .filter(LogConfig::isDefault)
                                                     .collect(Collectors.toList());
        // use the last default config
        if (defaultConfigs.isEmpty()) {
            return configs.get(configs.size() - 1);
        }
        return defaultConfigs.get(defaultConfigs.size() - 1);
    }

    protected KafkaLogConfig getConfig(Name name) {
        return configs.stream().filter(config -> config.match(name)).findFirst().orElse(defaultConfig);
    }

    protected KafkaLogConfig getConfig(Name name, Name group) {
        return configs.stream().filter(config -> config.match(name, group)).findFirst().orElse(defaultConfig);
    }

    @Override
    public void create(Name name, int size) {
        KafkaLogConfig config = getConfig(name);
        kUtils.get(config).createTopic(config.getResolver().getId(name), size, config.getReplicatorFactor());
    }

    @Override
    protected int getSize(Name name) {
        KafkaLogConfig config = getConfig(name);
        return kUtils.get(config).partitions(config.getResolver().getId(name));
    }

    @Override
    public boolean exists(Name name) {
        KafkaLogConfig config = getConfig(name);
        return kUtils.get(config).topicExists(config.getResolver().getId(name));
    }

    @Override
    public <M extends Externalizable> CloseableLogAppender<M> createAppender(Name name, Codec<M> codec) {
        KafkaLogConfig config = getConfig(name);
        return KafkaLogAppender.open(codec, config.getResolver(), name, config.getProducerProperties(),
                config.getConsumerProperties());
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doCreateTailer(Collection<LogPartition> partitions, Name group,
            Codec<M> codec) {
        partitions.forEach(this::checkValidPartition);
        if (partitions.isEmpty()) {
            return KafkaLogTailer.createAndAssign(codec, defaultConfig.getResolver(), partitions, group,
                    (Properties) defaultConfig.getConsumerProperties().clone());
        }
        KafkaLogConfig config = getConfig(partitions.iterator().next().name());
        return KafkaLogTailer.createAndAssign(codec, config.getResolver(), partitions, group,
                (Properties) config.getConsumerProperties().clone());
    }

    protected void checkValidPartition(LogPartition partition) {
        KafkaLogConfig config = getConfig(partition.name());
        int partitions = kUtils.get(config).getNumberOfPartitions(config.getResolver().getId(partition.name()));
        if (partition.partition() >= partitions) {
            throw new IllegalArgumentException("Partition out of bound " + partition + " max: " + partitions);
        }
    }

    @Override
    public void close() {
        super.close();
        configs.forEach(config -> kUtils.get(config).close());
    }

    @Override
    public boolean supportSubscribe() {
        return !defaultConfig.getDisableSubscribe();
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doSubscribe(Name group, Collection<Name> names,
            RebalanceListener listener, Codec<M> codec) {
        KafkaLogConfig config = getConfig(names.iterator().next(), group);
        return KafkaLogTailer.createAndSubscribe(codec, config.getResolver(), names, group,
                (Properties) config.getConsumerProperties().clone(), listener);
    }

    @Override
    public List<LogLag> getLagPerPartition(Name name, Name group) {
        KafkaLogConfig config = getConfig(name, group);
        Properties props = (Properties) config.getConsumerProperties().clone();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getResolver().getId(group));
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, config.getResolver().getId(group) + "-lag");
        // Prevents to create multiple consumers with the same client/group ids
        synchronized(KafkaLogManager.class) {
            try (KafkaConsumer<String, Bytes> consumer = new KafkaConsumer<>(props)) {
                List<TopicPartition> topicPartitions = consumer.partitionsFor(config.getResolver().getId(name))
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
    public List<Name> listAll() {
        Set<String> allTopics = kUtils.get(defaultConfig).listTopics();
        Set<Name> names = new HashSet<>(allTopics.size());
        for (String topic : allTopics) {
            for (KafkaLogConfig config : configs) {
                if (topic.startsWith(config.getResolver().getPrefix())) {
                    names.add(config.getResolver().getName(topic));
                }
            }
        }
        return new ArrayList<>(names);
    }

    @Override
    public String toString() {
        // TODO: filter displayed props
        return "KafkaLogManager{" + "configs=" + configs + ", defaultConfig=" + defaultConfig + ", defaultResolver"
                + defaultConfig.getResolver()
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
    public List<Name> listConsumerGroups(Name name) {
        KafkaLogConfig config = getConfig(name);
        String topic = config.getResolver().getId(name);
        if (!kUtils.get(config).topicExists(topic)) {
            throw new IllegalArgumentException("Unknown Log: " + name);
        }
        return kUtils.get(config)
                     .listConsumers(topic)
                     .stream()
                     .filter(group -> group.startsWith(config.getResolver().getPrefix()))
                     .map(config.getResolver()::getName)
                     .collect(Collectors.toList());
    }

}
