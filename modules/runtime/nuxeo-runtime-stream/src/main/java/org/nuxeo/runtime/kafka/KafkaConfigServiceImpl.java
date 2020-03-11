/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.runtime.kafka;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

public class KafkaConfigServiceImpl extends DefaultComponent implements KafkaConfigService {

    public static final String XP_KAFKA_CONFIG = "kafkaConfig";

    public static final int APPLICATION_STARTED_ORDER = -600;

    protected static final String DEFAULT_BOOTSTRAP_SERVERS = "DEFAULT_TEST";

    @Override
    public int getApplicationStartedOrder() {
        // since there is no dependencies, let's start before main nuxeo core services
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public Set<String> listConfigNames() {
        return getDescriptors(XP_KAFKA_CONFIG).stream()
                                              .map(Descriptor::getId)
                                              .collect(Collectors.toSet());
    }

    @Deprecated
    @Override
    public String getZkServers(String configName) {
        return getDescriptor(configName).zkServers;
    }

    @Override
    public Properties getProducerProperties(String configName) {
        Properties ret = getDescriptor(configName).producerProperties.properties;
        if (DEFAULT_BOOTSTRAP_SERVERS.equals(ret.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))) {
            ret.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        }
        return ret;
    }

    @Override
    public Properties getConsumerProperties(String configName) {
        Properties ret = getDescriptor(configName).consumerProperties.properties;
        if (DEFAULT_BOOTSTRAP_SERVERS.equals(ret.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))) {
            ret.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        }
        return ret;
    }

    @Override
    public String getTopicPrefix(String configName) {
        KafkaConfigDescriptor config = getDescriptor(configName);
        String ret = config.topicPrefix == null ? "" : config.topicPrefix;
        if (config.randomPrefix) {
            ret += System.currentTimeMillis() + "-";
        }
        return ret;
    }

    protected KafkaConfigDescriptor getDescriptor(String configName) {
        KafkaConfigDescriptor descriptor = getDescriptor(XP_KAFKA_CONFIG, configName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown configuration name: " + configName);
        }
        return descriptor;
    }

}
