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
package org.nuxeo.lib.stream.tests.computation;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.log.LogStreamProcessor;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.lib.stream.tests.TestKafkaUtils;
import org.nuxeo.lib.stream.tests.log.TestLogKafka;

/**
 * Kafka test using subscribe API for topic/partitions
 *
 * @since 9.3
 */
public class TestLogStreamProcessorKafka extends TestStreamProcessor {
    @Rule
    public TestName testName = new TestName();

    protected String prefix;

    @BeforeClass
    public static void assumeKafkaEnabled() {
        TestKafkaUtils.assumeKafkaEnabled();
    }

    public String getTopicPrefix(String mark) {
        return "nuxeo-test-" + System.currentTimeMillis() + "-" + testName.getMethodName() + "-";
    }

    @Override
    public LogManager getLogManager() throws Exception {
        this.prefix = getTopicPrefix(testName.getMethodName());
        return new KafkaLogManager(KafkaUtils.getZkServers(), prefix, TestLogKafka.getProducerProps(),
                getConsumerProps());
    }

    @Override
    public LogManager getSameLogManager() throws Exception {
        return new KafkaLogManager(KafkaUtils.getZkServers(), prefix, TestLogKafka.getProducerProps(),
                getConsumerProps());
    }

    protected Properties getConsumerProps() {
        return TestLogKafka.getConsumerProps();
    }

    @Override
    public StreamProcessor getStreamProcessor(LogManager logManager) {
        return new LogStreamProcessor(logManager);
    }
}
