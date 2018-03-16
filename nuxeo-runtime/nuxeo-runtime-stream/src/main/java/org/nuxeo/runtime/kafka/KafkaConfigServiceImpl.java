/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class KafkaConfigServiceImpl extends DefaultComponent implements KafkaConfigService {
    public static final String KAFKA_CONFIG_XP = "kafkaConfig";

    public static final int APPLICATION_STARTED_ORDER = -600;

    private static final Log log = LogFactory.getLog(KafkaConfigServiceImpl.class);

    protected static final String DEFAULT_BOOTSTRAP_SERVERS = "DEFAULT_TEST";

    protected final Map<String, KafkaConfigDescriptor> configs = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(KAFKA_CONFIG_XP)) {
            KafkaConfigDescriptor descriptor = (KafkaConfigDescriptor) contribution;
            configs.put(descriptor.name, descriptor);
            log.info(String.format("Register Kafka contribution: %s", descriptor.name));
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        // since there is no dependencies, let's start before main nuxeo core services
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        log.debug("Deactivating service");
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        log.debug("Activating service");
    }

    @Override
    public Set<String> listConfigNames() {
        return configs.keySet();
    }

    @Deprecated
    @Override
    public String getZkServers(String configName) {
        checkConfigName(configName);
        return configs.get(configName).zkServers;
    }

    protected void checkConfigName(String configName) {
        if (!configs.containsKey(configName)) {
            throw new IllegalArgumentException("Unknown configuration name: " + configName);
        }
    }

    @Override
    public Properties getProducerProperties(String configName) {
        checkConfigName(configName);
        Properties ret = configs.get(configName).getProducerProperties();
        if (DEFAULT_BOOTSTRAP_SERVERS.equals(ret.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))) {
            ret.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        }
        return ret;
    }

    @Override
    public Properties getConsumerProperties(String configName) {
        checkConfigName(configName);
        Properties ret = configs.get(configName).getConsumerProperties();
        if (DEFAULT_BOOTSTRAP_SERVERS.equals(ret.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))) {
            ret.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        }
        return ret;
    }

    @Override
    public String getTopicPrefix(String configName) {
        checkConfigName(configName);
        return configs.get(configName).getTopicPrefix();
    }
}
