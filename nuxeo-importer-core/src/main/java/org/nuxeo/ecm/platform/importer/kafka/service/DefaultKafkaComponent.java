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
package org.nuxeo.ecm.platform.importer.kafka.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DefaultKafkaComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(DefaultKafkaComponent.class);
    public static final String KAFKA_CONFIGURATION_XP = "kafkaConfiguration";
    protected DefaultKafkaService kafkaService;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(KAFKA_CONFIGURATION_XP)) {
            DefaultKafkaConfigurationDescriptor descriptor = (DefaultKafkaConfigurationDescriptor) contribution;
            Properties producerProps = descriptor.getProducerProperties();
            if (producerProps == null) {
                producerProps = new Properties();
            }
            kafkaService.setProducerProperties(producerProps);

            Properties consumerProps = descriptor.getConsumerProperties();
            if (consumerProps == null) {
                consumerProps = new Properties();
            }
            kafkaService.setConsumerProperties(consumerProps);

            List<String> topics = descriptor.getTopics();
            if (topics == null) {
                topics = new ArrayList<>();
            }
            topics.stream()
                    .filter(s -> !kafkaService.allTopics().contains(s))
                    .forEach(s -> kafkaService.addTopic(s));

            log.info(String.format("Kafka service started, topics %s propagated", topics));
        }
    }

    @Override
    public void activate(ComponentContext context) {
        kafkaService = new DefaultKafkaServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        kafkaService = null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DefaultKafkaService.class)) {
            return adapter.cast(kafkaService);
        }
        return super.getAdapter(adapter);
    }
}