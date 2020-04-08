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

import java.util.Properties;
import java.util.Set;

/**
 * Service to collect kafka configurations.
 *
 * @since 9.3
 */
public interface KafkaConfigService {

    /**
     * List the name of the registered Kafka configuration.
     */
    Set<String> listConfigNames();

    /**
     * Returns the Kafka producer properties for a configuration.
     */
    Properties getProducerProperties(String configName);

    /**
     * Returns the Kafka consumer properties for a configuration.
     */
    Properties getConsumerProperties(String configName);

    /**
     * Returns the topic prefix to use for a configuration.
     */
    String getTopicPrefix(String configName);

    /**
     * Returns the Kafka admin properties for a configuration.
     *
     * @since 11.1
     */
    Properties getAdminProperties(String kafkaConfig);
}
