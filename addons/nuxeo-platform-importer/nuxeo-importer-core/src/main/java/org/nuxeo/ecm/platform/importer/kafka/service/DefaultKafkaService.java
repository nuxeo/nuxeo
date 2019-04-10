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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public interface DefaultKafkaService {

    Properties getProducerProperties();

    void setProducerProperties(Properties producerProperties);

    Properties getConsumerProperties();

    void setConsumerProperties(Properties consumerProperties);

    List<String> allTopics();

    void addTopic(String topic);

    boolean removeTopic(String topic);

    List<String> propagateTopics(int partitions, short replication, int timeout) throws IOException;
}
