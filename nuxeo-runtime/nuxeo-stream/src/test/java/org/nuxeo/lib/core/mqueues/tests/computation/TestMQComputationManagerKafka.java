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
package org.nuxeo.lib.core.mqueues.tests.computation;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.nuxeo.lib.core.mqueues.computation.ComputationManager;
import org.nuxeo.lib.core.mqueues.computation.mqueue.MQComputationManager;
import org.nuxeo.lib.core.mqueues.mqueues.MQManager;
import org.nuxeo.lib.core.mqueues.mqueues.kafka.KafkaMQManager;
import org.nuxeo.lib.core.mqueues.mqueues.kafka.KafkaUtils;
import org.nuxeo.lib.core.mqueues.tests.mqueues.TestMQueueKafka;

import java.util.Properties;

/**
 * Kafka test using subscribe API for topic/partitions
 * @since 9.2
 */
public class TestMQComputationManagerKafka extends TestComputationManager {
    protected String prefix;

    @BeforeClass
    public static void assumeKafkaEnabled() {
        Assume.assumeTrue(KafkaUtils.kafkaDetected());
    }

    @Rule
    public TestName testName = new TestName();

    public String getTopicPrefix(String mark) {
        return "nuxeo-test-" + System.currentTimeMillis() + "-" + testName.getMethodName() + "-";
    }

    @Override
    public MQManager getStreams() throws Exception {
        this.prefix = getTopicPrefix(testName.getMethodName());
        return new KafkaMQManager(KafkaUtils.DEFAULT_ZK_SERVER, prefix,
                TestMQueueKafka.getProducerProps(),
                getConsumerProps());
    }

    @Override
    public MQManager getSameStreams() throws Exception {
        return new KafkaMQManager(KafkaUtils.DEFAULT_ZK_SERVER, prefix,
                TestMQueueKafka.getProducerProps(),
                getConsumerProps());
    }

    protected Properties getConsumerProps() {
        return TestMQueueKafka.getConsumerProps();
    }

    @Override
    public ComputationManager getManager(MQManager mqManager) {
        return new MQComputationManager(mqManager);
    }
}
