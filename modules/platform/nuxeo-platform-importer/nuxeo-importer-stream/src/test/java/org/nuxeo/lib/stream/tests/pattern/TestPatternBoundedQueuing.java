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
package org.nuxeo.lib.stream.tests.pattern;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.KeyValueMessage;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPool;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerStatus;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.lib.stream.pattern.producer.ProducerStatus;
import org.nuxeo.lib.stream.tests.pattern.consumer.IdMessageFactory;
import org.nuxeo.lib.stream.tests.pattern.producer.RandomIdMessageProducerFactory;

import net.jodah.failsafe.RetryPolicy;

public abstract class TestPatternBoundedQueuing {
    protected static final Log log = LogFactory.getLog(TestPatternBoundedQueuing.class);

    protected static final String LOG_NAME = "logName";

    protected static final int LOG_SIZE = 10;

    @Rule
    public TestName name = new TestName();

    protected LogManager manager;

    public abstract LogManager createManager() throws Exception;

    @Before
    public void initManager() throws Exception {
        if (manager == null) {
            manager = createManager();
        }
    }

    @After
    public void resetManager() throws Exception {
        if (manager != null) {
            manager.close();
        }
        manager = null;
    }

    @Test
    public void producersThenConsumers() throws Exception {
        final short NB_PRODUCERS = 15;
        final int NB_DOCUMENTS = 1_000;

        // 1. Create a log and run the producers
        manager.createIfNotExists(Name.ofUrn(LOG_NAME), LOG_SIZE);
        ProducerPool<KeyValueMessage> producers = new ProducerPool<>(LOG_NAME, manager,
                new RandomIdMessageProducerFactory(NB_DOCUMENTS), NB_PRODUCERS);
        List<ProducerStatus> pret = producers.start().get();

        assertEquals(NB_PRODUCERS, pret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, pret.stream().mapToLong(r -> r.nbProcessed).sum());

        // 2 run the consumers
        ConsumerPolicy consumerPolicy = ConsumerPolicy.builder()
                                                      .waitMessageTimeout(Duration.ofSeconds(5))
                                                      .continueOnFailure(false)
                                                      .maxThreads((short) 8)
                                                      .build();
        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(LOG_NAME, manager, IdMessageFactory.NOOP,
                consumerPolicy);
        List<ConsumerStatus> cret = consumers.start().get();

        assertEquals(consumerPolicy.getMaxThreads(), cret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, cret.stream().mapToLong(r -> r.committed).sum());
    }

    @Test
    public void producersAndConsumersConcurrently() throws Exception {
        final short NB_PRODUCERS = 15;
        final int NB_DOCUMENTS = 1000;

        // Create a log, producer and consumer pool
        manager.createIfNotExists(Name.ofUrn(LOG_NAME), LOG_SIZE);

        ProducerPool<KeyValueMessage> producers = new ProducerPool<>(LOG_NAME, manager,
                new RandomIdMessageProducerFactory(NB_DOCUMENTS), NB_PRODUCERS);
        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(LOG_NAME, manager, IdMessageFactory.NOOP,
                ConsumerPolicy.BOUNDED);
        CompletableFuture<List<ProducerStatus>> pfuture = producers.start();
        CompletableFuture<List<ConsumerStatus>> cfuture = consumers.start();
        List<ConsumerStatus> cret = cfuture.get(); // wait for the completion
        List<ProducerStatus> pret = pfuture.get();

        assertEquals(NB_PRODUCERS, pret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, pret.stream().mapToLong(r -> r.nbProcessed).sum());

        assertEquals(LOG_SIZE, cret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, cret.stream().mapToLong(r -> r.committed).sum());
    }

    @Test
    public void producerAndBuggyConsumers() throws Exception {
        final int LOG_SIZE = 12;
        final short NB_PRODUCERS = 10;
        final short NB_CONSUMERS = 7;
        final int NB_DOCUMENTS = 127;
        final int BATCH_SIZE = 13;

        manager.createIfNotExists(Name.ofUrn(LOG_NAME), LOG_SIZE);
        ProducerPool<KeyValueMessage> producers = new ProducerPool<>(LOG_NAME, manager,
                new RandomIdMessageProducerFactory(NB_DOCUMENTS, RandomIdMessageProducerFactory.ProducerType.ORDERED),
                NB_PRODUCERS);
        List<ProducerStatus> pret = producers.start().get();

        assertEquals(NB_PRODUCERS, pret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, pret.stream().mapToLong(r -> r.nbProcessed).sum());

        // 2. Use the log and run the consumers
        ConsumerPolicy consumerPolicy = ConsumerPolicy.builder()
                                                      .waitMessageTimeout(Duration.ofSeconds(10))
                                                      .maxThreads(NB_CONSUMERS)
                                                      .batchPolicy(BatchPolicy.builder().capacity(BATCH_SIZE).build())
                                                      .retryPolicy(new RetryPolicy().withMaxRetries(10000))
                                                      .build();
        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(LOG_NAME, manager, IdMessageFactory.BUGGY,
                consumerPolicy);

        List<ConsumerStatus> cret = consumers.start().get();

        assertEquals(consumerPolicy.getMaxThreads(), cret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, cret.stream().mapToLong(r -> r.committed).sum());
        assertTrue(NB_PRODUCERS * NB_DOCUMENTS < cret.stream().mapToLong(r -> r.accepted).sum());
    }

    @Test
    public void producerAndBrokenConsumers() throws Exception {
        final int LOG_SIZE = 2;
        final short NB_PRODUCERS = 2;
        final short NB_CONSUMERS = 2;
        final int NB_DOCUMENTS = 1;
        final int BATCH_SIZE = 1;

        manager.createIfNotExists(Name.ofUrn(LOG_NAME), LOG_SIZE);
        ProducerPool<KeyValueMessage> producers = new ProducerPool<>(LOG_NAME, manager,
                new RandomIdMessageProducerFactory(NB_DOCUMENTS, RandomIdMessageProducerFactory.ProducerType.ORDERED),
                NB_PRODUCERS);
        List<ProducerStatus> pret = producers.start().get();
        assertEquals(NB_PRODUCERS, pret.size());
        assertEquals(NB_PRODUCERS * NB_DOCUMENTS, pret.stream().mapToLong(r -> r.nbProcessed).sum());

        // 2. Use the log and run a broken consumers
        ConsumerPolicy consumerPolicy = ConsumerPolicy.builder()
                .waitMessageTimeout(Duration.ofSeconds(10))
                .maxThreads(NB_CONSUMERS)
                .batchPolicy(BatchPolicy.builder().capacity(BATCH_SIZE).build())
                .retryPolicy(new RetryPolicy().withMaxRetries(2))
                .build();
        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(LOG_NAME, manager, IdMessageFactory.ERROR,
                consumerPolicy);
        List<ConsumerStatus> cret = consumers.start().get();

        assertEquals(consumerPolicy.getMaxThreads(), cret.size());
        assertEquals(0, cret.stream().mapToLong(r -> r.committed).sum());
        assertEquals(0, cret.stream().mapToLong(r -> r.accepted).sum());
        assertEquals(0, cret.stream().filter(r -> !r.fail).count());
    }


    public int getNbDocumentForBuggyConsumerTest() {
        return 10151;
    }

}
