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
import java.time.Instant;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.lib.stream.tests.KeyValueMessage;
import org.nuxeo.lib.stream.tests.TestKafkaUtils;

public class TestLogKafka extends TestLog {

    public static final String TOPIC_PREFIX = "nuxeo-test";

    private static final Name GROUP = Name.ofUrn("test/defaultTest");

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
    public LogManager createManager() {
        if (prefix == null) {
            prefix = getPrefix();
        }
        return new KafkaLogManager(prefix, getProducerProps(), getConsumerProps());
    }

    @After
    public void resetPrefix() {
        prefix = null;
    }

    @Test
    public void testSeekToEnd() throws Exception {
        manager.createIfNotExists(logName, 1);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        appender.append(0, KeyValueMessage.of("mess1"));
        appender.append(0, KeyValueMessage.of("mess2"));
        appender.append(0, KeyValueMessage.of("mess3"));
        appender.append(0, KeyValueMessage.of("mess4"));

        LogPartition logPartition = LogPartition.of(logName, 0);
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logPartition)) {
            Assert.assertEquals("mess1", tailer.read(DEF_TIMEOUT).message().key());
            assertEquals("There should be a lag of 4 uncommitted records", 4, manager.getLag(logName, GROUP).lag());
            tailer.commit();
            assertEquals("There should be a lag of 3 uncommitted records", 3, manager.getLag(logName, GROUP).lag());
            tailer.toEnd();
            tailer.commit();
            assertEquals("There should be a no uncommitted records", 0, manager.getLag(logName, GROUP).lag());
            appender.append(0, KeyValueMessage.of("mess5"));
            Assert.assertEquals("mess5", tailer.read(DEF_TIMEOUT).message().key());
            assertEquals("There should be a 1 uncommitted records", 1, manager.getLag(logName, GROUP).lag());
            tailer.commit();
            assertEquals("There should be a 0 uncommitted records", 0, manager.getLag(logName, GROUP).lag());
        }
    }

    @SuppressWarnings("squid:S2925")
    @Test
    public void testSeekByTimestamp() throws Exception {

        manager.createIfNotExists(logName, 1);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        appender.append(0, KeyValueMessage.of("id1"));
        appender.append(0, KeyValueMessage.of("id2"));
        appender.append(0, KeyValueMessage.of("id3"));
        appender.append(0, KeyValueMessage.of("id4"));
        appender.append(0, KeyValueMessage.of("id5"));

        // Get the time and wait 3 seconds
        Thread.sleep(3000L);
        Instant now = Instant.now();

        appender.append(0, KeyValueMessage.of("id6"));
        appender.append(0, KeyValueMessage.of("id7"));
        appender.append(0, KeyValueMessage.of("id8"));
        LogOffset offset9 = appender.append(0, KeyValueMessage.of("id9"));
        LogPartition logPartition = LogPartition.of(logName, 0);
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logPartition)) {
            Assert.assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
            tailer.seek(offset9);
            Assert.assertEquals("id9", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
            assertEquals("There should be no lag, because we are at the last record", 0,
                    manager.getLag(logName, GROUP).lag());
            LogOffset logOffset = tailer.offsetForTimestamp(logPartition, now.toEpochMilli());
            tailer.seek(logOffset);
            tailer.commit();
            Assert.assertEquals("id6", tailer.read(DEF_TIMEOUT).message().key());
            Assert.assertEquals("id7", tailer.read(DEF_TIMEOUT).message().key());
            Assert.assertEquals("id8", tailer.read(DEF_TIMEOUT).message().key());
            Assert.assertEquals("Must read 9 again", "id9", tailer.read(DEF_TIMEOUT).message().key());
        }
    }

    @Test
    public void testSubscribe() throws Exception {
        final int NB_QUEUE = 3;
        final int NB_MSG = 200;
        final int NB_CONSUMER = 6;
        final Name group = Name.ofUrn("test/consumer");

        manager.createIfNotExists(logName, NB_QUEUE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage.of("id2");
        for (int i = 0; i < NB_MSG; i++) {
            appender.append(i % NB_QUEUE, msg1);
        }

        LogTailer<KeyValueMessage> tailer1 = manager.subscribe(group, Collections.singleton(logName), null);

        // until we call read there is no assignments
        assertTrue(tailer1.assignments().isEmpty());
        LogRecord<KeyValueMessage> record;
        try {
            tailer1.read(Duration.ofSeconds(2));
            fail("Should have raise a rebalance exception");
        } catch (RebalanceException e) {
            // expected
        }
        // assignments have been done with a single tailer
        assertFalse(tailer1.assignments().isEmpty());
        assertEquals(NB_QUEUE, tailer1.assignments().size());
        // read NB_QUEUE msg and commit
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
            LogRecord<KeyValueMessage> consumerRecord;
            while (true) {
                try {
                    consumerRecord = consumerTailer.read(Duration.ofSeconds(2));
                    if (consumerRecord != null) {
                        count++;
                        consumerTailer.commit();
                    } else {
                        // log.warn("returns " + count);
                        // if we leave without closing rebalance will wait max.poll.interval before taking decision
                        consumerTailer.close();
                        return count;
                    }
                } catch (RebalanceException e) {
                    // rebalance is expected on first read and when consumer returns
                    // log.warn("rebalance " +  consumerTailer.assignments().size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(NB_CONSUMER, new ThreadFactory() {
            protected final AtomicInteger count = new AtomicInteger(0);

            @SuppressWarnings("NullableProblems")
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
