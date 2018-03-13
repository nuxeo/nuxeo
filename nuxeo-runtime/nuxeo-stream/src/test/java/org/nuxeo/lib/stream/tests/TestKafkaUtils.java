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
package org.nuxeo.lib.stream.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;

/**
 * @since 9.3
 */
public class TestKafkaUtils {
    protected static final String DEFAULT_TOPIC = "nuxeo-test-default-topic-10";

    protected static final int DEFAULT_TOPIC_PARTITION = 10;

    @BeforeClass
    public static void assumeKafkaEnabled() {
        if ("true".equals(System.getProperty("kafka"))) {
            if (!KafkaUtils.kafkaDetected()) {
                fail("Kafka profile is enable, but no Zookeeper found: " + KafkaUtils.getZkServers());
            }
        } else {
            Assume.assumeTrue("No kafka profile", false);
        }
    }

    protected void createDefaultTopicIfNeeded(KafkaUtils kutils) {
        if (!kutils.topicExists(DEFAULT_TOPIC)) {
            kutils.createTopicWithoutReplication(DEFAULT_TOPIC, DEFAULT_TOPIC_PARTITION);
        }
    }

    @Test
    public void testCreateTopic() {
        try (KafkaUtils kutils = new KafkaUtils()) {
            createDefaultTopicIfNeeded(kutils);
            assertEquals(DEFAULT_TOPIC_PARTITION, kutils.getNumberOfPartitions(DEFAULT_TOPIC));
        }
    }

    @Test
    public void testResetConsumerStates() {
        try (KafkaUtils kutils = new KafkaUtils()) {
            createDefaultTopicIfNeeded(kutils);
            kutils.resetConsumerStates(DEFAULT_TOPIC);
        }
    }

    @Test
    public void testBrokerList() {
        try (KafkaUtils kutils = new KafkaUtils()) {
            assertNotNull(kutils.getBrokerEndPoints());
            assertEquals(Collections.singleton("PLAINTEXT://" + KafkaUtils.getBootstrapServers()),
                    kutils.getBrokerEndPoints());
        }
    }

    @Test
    public void testDefaultBootstrapServers() {
        try (KafkaUtils kutils = new KafkaUtils()) {
            assertEquals("PLAINTEXT://" + KafkaUtils.getBootstrapServers(), kutils.getDefaultBootstrapServers());
        }
    }

    @Test
    public void testAssignments() {
        Map<String, Integer> streams = new HashMap<>();
        streams.put("s1", 16);
        streams.put("s2", 8);
        streams.put("s3", 1);
        List<List<LogPartition>> ret = KafkaUtils.roundRobinAssignments(3, streams);
        // there are 25 partitions in total so 5 per
        assertEquals(3, ret.size());
        assertEquals(9, ret.get(0).size());
        assertEquals(8, ret.get(1).size());
        assertEquals(8, ret.get(2).size());
        // assertEquals(null, ret);

        ret = KafkaUtils.rangeAssignments(5, streams);
        assertEquals(5, ret.size());
        assertEquals(7, ret.get(0).size());

        // assertEquals(null, ret);
    }

}
