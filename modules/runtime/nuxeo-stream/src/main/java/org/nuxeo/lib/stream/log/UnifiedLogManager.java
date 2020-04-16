/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogConfig;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogConfig;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;

/**
 * @since 11.1
 */
public class UnifiedLogManager implements LogManager {

    protected final List<LogConfig> configs;

    protected LogManager cqManager;

    protected LogManager kafkaManager;

    protected LogManager defaultManager;

    protected LogConfig defaultConfig;

    protected Map<LogConfig, LogManager> managers = new HashMap<>();

    public UnifiedLogManager(List<LogConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("No LogConfig provided");
        }
        this.configs = configs;
        createCQLogManager();
        createKafkaLogManager();
        findDefaultLogManger();
    }

    protected void createCQLogManager() {
        List<ChronicleLogConfig> cqConfigs = configs.stream()
                                                    .filter(config -> config instanceof ChronicleLogConfig)
                                                    .map(config -> (ChronicleLogConfig) config)
                                                    .collect(Collectors.toList());
        if (!cqConfigs.isEmpty()) {
            cqManager = new ChronicleLogManager(cqConfigs);
            cqConfigs.forEach(config -> managers.put(config, cqManager));
        }
    }

    protected void createKafkaLogManager() {
        List<KafkaLogConfig> kafkaConfigs = configs.stream()
                                                   .filter(config -> config instanceof KafkaLogConfig)
                                                   .map(config -> (KafkaLogConfig) config)
                                                   .collect(Collectors.toList());
        if (!kafkaConfigs.isEmpty()) {
            kafkaManager = new KafkaLogManager(kafkaConfigs);
            kafkaConfigs.forEach(config -> managers.put(config, kafkaManager));
        }
    }

    protected void findDefaultLogManger() {
        List<LogConfig> defaultConfigs = configs.stream().filter(LogConfig::isDefault).collect(Collectors.toList());
        // use the last default config
        if (defaultConfigs.isEmpty()) {
            defaultConfig = configs.get(configs.size() - 1);
        } else {
            defaultConfig = defaultConfigs.get(defaultConfigs.size() - 1);
        }
        if (defaultConfig instanceof ChronicleLogConfig) {
            defaultManager = cqManager;
        } else {
            defaultManager = kafkaManager;
        }
    }

    protected LogManager getManager(Name name) {
        return managers.get(configs.stream()
                                   .filter(config -> config.match(name))
                                   .findFirst()
                                   .orElse(defaultConfig));
    }

    protected LogManager getManager(Name name, Name group) {
        return managers.get(
                configs.stream()
                       .filter(config -> config.match(name, group))
                       .findFirst()
                       .orElse(defaultConfig));
    }

    @Override
    public boolean exists(Name name) {
        return getManager(name).exists(name);
    }

    @Override
    public boolean createIfNotExists(Name name, int size) {
        return getManager(name).createIfNotExists(name, size);
    }

    @Override
    public boolean delete(Name name) {
        return getManager(name).delete(name);
    }

    @Override
    public int size(Name name) {
        return getManager(name).size(name);
    }

    @Override
    public <M extends Externalizable> LogAppender<M> getAppender(Name name, Codec<M> codec) {
        return getManager(name).getAppender(name, codec);
    }

    @Override
    public <M extends Externalizable> LogTailer<M> createTailer(Name group, Collection<LogPartition> partitions,
            Codec<M> codec) {
        if (partitions.isEmpty()) {
            return defaultManager.createTailer(group, partitions, codec);
        }
        Name name = partitions.iterator().next().name();
        return getManager(name, group).createTailer(group, partitions, codec);
    }

    @Override
    public boolean supportSubscribe() {
        return defaultManager.supportSubscribe();
    }

    @Override
    public <M extends Externalizable> LogTailer<M> subscribe(Name group, Collection<Name> names,
            RebalanceListener listener, Codec<M> codec) {
        Name name = names.iterator().next();
        return getManager(name, group).subscribe(group, names, listener, codec);
    }

    @Override
    public List<LogLag> getLagPerPartition(Name name, Name group) {
        return getManager(name, group).getLagPerPartition(name, group);
    }

    @Override
    public <M extends Externalizable> List<Latency> getLatencyPerPartition(Name name, Name group, Codec<M> codec,
            Function<M, Long> timestampExtractor, Function<M, String> keyExtractor) {
        return getManager(name, group).getLatencyPerPartition(name, group, codec, timestampExtractor, keyExtractor);
    }

    @Override
    public List<Name> listAll() {
        List<Name> names = new ArrayList<>();
        if (kafkaManager != null) {
            names.addAll(kafkaManager.listAll());
        }
        if (cqManager != null) {
            names.addAll(cqManager.listAll());
        }
        return names;
    }

    @Override
    public List<Name> listConsumerGroups(Name name) {
        return getManager(name).listConsumerGroups(name);
    }

    @Override
    public void close() {
        if (kafkaManager != null) {
            kafkaManager.close();
        }
        if (cqManager != null) {
            cqManager.close();
        }
    }
}
