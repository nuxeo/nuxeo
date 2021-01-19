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
package org.nuxeo.runtime.kafka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationHolder;
import org.nuxeo.launcher.config.backingservices.BackingChecker;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;

/**
 * @since 11.3
 */
public class KafkaChecker implements BackingChecker {

    private static final Logger log = LogManager.getLogger(KafkaChecker.class);

    private static final String KAFKA_ENABLED_PROP = "kafka.enabled";

    private static final String CONFIG_NAME = "kafka-config.xml";

    @Override
    public boolean accepts(ConfigurationHolder configHolder) {
        // not using Boolean.parseValue on purpose, only 'true' must trigger the checker
        if (!"true".equals(configHolder.getProperty(KAFKA_ENABLED_PROP))) {
            log.debug("Checker skipped because Kafka is disabled");
            return false;
        }
        return true;
    }

    @Override
    public void check(ConfigurationHolder configHolder) throws ConfigurationException {
        KafkaConfigDescriptor config = getDescriptor(configHolder, CONFIG_NAME, KafkaConfigDescriptor.class);
        try (KafkaLogManager manager = new KafkaLogManager(config.topicPrefix, config.producerProperties.properties,
                config.consumerProperties.properties)) {
            manager.exists(Name.ofUrn("input/null"));
        } catch (Exception e) {
            throw new ConfigurationException("Unable to reach Kafka using: " + config.producerProperties.properties, e);
        }
    }
}
