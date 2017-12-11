/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.lib.stream.tests.log;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.lib.stream.tests.KeyValueMessage;
import org.nuxeo.lib.stream.tests.TestKafkaUtils;

public class TestLogKafka extends TestLog {

    public static final String TOPIC_PREFIX = "nuxeo-test";

    protected String prefix;

    @BeforeClass
    public static void assumeKafkaEnabled() {
        TestKafkaUtils.assumeKafkaEnabled();
    }

    public static String getPrefix() {
        return TOPIC_PREFIX + "-" + System.currentTimeMillis() + "-";
    }

    public static Properties getProducerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        return props;
    }

    public static Properties getConsumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        // consumer are removed from a group if there more than this interval between poll
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 20000);
        // session timeout, consumer is removed from a group if there is no heartbeat on this interval
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        // short ht interval so that rebalance don't take for ever
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 400);
        // keep number low to reduce time interval between poll
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        return props;
    }

    @Override
    public LogManager createManager() throws Exception {
        if (prefix == null) {
            prefix = getPrefix();
        }
        return new KafkaLogManager(KafkaUtils.getZkServers(), prefix, getProducerProps(), getConsumerProps());
    }

    @After
    public void resetPrefix() {
        prefix = null;
    }

    @Test
    public void testSubscribe() throws Exception {
        final int NB_QUEUE = 3;
        final int NB_MSG = 1000;
        final int NB_CONSUMER = 20;
        final String group = "consumer";

        manager.createIfNotExists(logName, NB_QUEUE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        for (int i = 0; i < NB_MSG; i++) {
            appender.append(i % NB_QUEUE, msg1);
        }

        LogTailer<KeyValueMessage> tailer1 = manager.subscribe(group, Collections.singleton(logName), null);

        // until we call read there is no assignments
        assertTrue(tailer1.assignments().isEmpty());
        LogRecord<KeyValueMessage> record = null;
        try {
            tailer1.read(Duration.ofSeconds(2));
            fail("Should have raise a rebalance exception");
        } catch (RebalanceException e) {
            // expected
        }
        // assignments have been done with a single tailer
        assertFalse(tailer1.assignments().isEmpty());
        assertEquals(NB_QUEUE, tailer1.assignments().size());
        // read all the messages and commit
        for (int i = 0; i < NB_QUEUE; i++) {
            record = tailer1.read(Duration.ofSeconds(1));
            assertNotNull(record);
            assertEquals(msg1, record.message());
        }
        tailer1.commit();
        tailer1.close();

        // And now enter the 2nd tailer
        Callable<Integer> consumer = () -> {
            int count = 0;
            LogTailer<KeyValueMessage> consumerTailer = manager.subscribe(group, Collections.singleton(logName), null);
            LogRecord<KeyValueMessage> consumerRecord = null;
            while (true) {
                try {
                    consumerRecord = consumerTailer.read(Duration.ofMillis(200));
                    if (consumerRecord == null) {
                        // log.warn("returns " + count);
                        // if we don't commit a thread can consume all messages and returns
                        // before being rebalanced, the others will then consume the same message
                        consumerTailer.commit();
                        // if we leave without closing rebalance will wait max.poll.interval before taking decision
                        consumerTailer.close();
                        return count;
                    }
                    count++;
                } catch (RebalanceException e) {
                    // expected, we start from last committed value
                    count = 0;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(NB_CONSUMER, new ThreadFactory() {
            protected final AtomicInteger count = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, String.format("%s-%02d", "consumer", count.getAndIncrement()));
                t.setUncaughtExceptionHandler((t1, e) -> log.error("Uncaught exception: " + e.getMessage(), e));
                return t;
            }
        });

        List<Future<Integer>> ret = new ArrayList<>(NB_CONSUMER);
        for (int i = 0; i < NB_CONSUMER; i++) {
            ret.add(service.submit(consumer));
        }
        service.shutdown();
        service.awaitTermination(60, TimeUnit.SECONDS);
        int total = 0;
        for (Future<Integer> future : ret) {
            int count = future.get();
            total += count;
        }
        assertEquals(NB_MSG - NB_QUEUE, total);
    }

}
