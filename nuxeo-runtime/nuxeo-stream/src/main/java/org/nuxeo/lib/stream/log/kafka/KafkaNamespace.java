/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * Handle a namespace for Kafka topic and group.
 *
 * @since 10.1
 */
public class KafkaNamespace {

    protected final String prefix;

    protected final int prefixLen;

    public KafkaNamespace(String prefix) {
        this.prefix = prefix;
        this.prefixLen = prefix.length();
    }

    public String getTopicName(String logName) {
        return prefix + logName;
    }

    public String getLogName(String topicName) {
        if (!topicName.startsWith(prefix)) {
            throw new IllegalArgumentException(String.format("topic %s with invalid prefix %s", topicName, prefix));
        }
        return topicName.substring(prefixLen);
    }

    public String getKafkaGroup(String group) {
        return prefix + group;
    }

    public String getGroup(String kafkaGroup) {
        if (!kafkaGroup.startsWith(prefix)) {
            throw new IllegalArgumentException(String.format("group %s with invalid prefix %s", kafkaGroup, prefix));
        }
        return kafkaGroup.substring(prefixLen);
    }

    @Override
    public String toString() {
        return "KafkaNamespace{" + "prefix='" + prefix + '\'' + '}';
    }
}
