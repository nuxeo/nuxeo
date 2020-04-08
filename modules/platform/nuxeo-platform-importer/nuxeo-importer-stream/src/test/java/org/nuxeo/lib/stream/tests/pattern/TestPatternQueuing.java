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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.KeyValueMessage;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPool;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerStatus;
import org.nuxeo.lib.stream.tests.pattern.consumer.IdMessageFactory;

public abstract class TestPatternQueuing {
    protected static final Log log = LogFactory.getLog(TestPatternQueuing.class);

    protected Name logName = Name.ofUrn("logName");

    @Rule
    public TestName name = new TestName();

    protected LogManager manager;

    public abstract LogManager createManager() throws Exception;

    @Before
    public void initManager() throws Exception {
        logName = Name.ofUrn(name.getMethodName());
        if (manager == null) {
            manager = createManager();
        }
    }

    @After
    public void closeManager() throws Exception {
        if (manager != null) {
            manager.close();
        }
        manager = null;
    }

    public void resetManager() throws Exception {
        closeManager();
        initManager();
    }

    @Test
    public void endWithPoisonPill() throws Exception {
        final int LOG_SIZE = 2;

        manager.createIfNotExists(logName, LOG_SIZE);

        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(logName.getUrn(), manager, IdMessageFactory.NOOP,
                ConsumerPolicy.UNBOUNDED);
        // run the consumers pool
        CompletableFuture<List<ConsumerStatus>> consumersFuture = consumers.start();

        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        // submit messages
        LogOffset offset1 = appender.append(0, KeyValueMessage.of("id1"));
        // may be processed but not committed because batch is not full
        assertFalse(appender.waitFor(offset1, consumers.getConsumerGroupName(), Duration.ofMillis(0)));
        // send a force batch
        appender.append(0, KeyValueMessage.ofForceBatch("batch now"));
        assertTrue(appender.waitFor(offset1, consumers.getConsumerGroupName(), Duration.ofSeconds(10)));

        // terminate consumers
        appender.append(0, KeyValueMessage.POISON_PILL);
        appender.append(1, KeyValueMessage.POISON_PILL);

        List<ConsumerStatus> ret = consumersFuture.get();
        assertEquals(LOG_SIZE, ret.size());
        assertEquals(2, ret.stream().mapToLong(r -> r.committed).sum());
    }

    @Test
    public void endWithPoisonPillCommitTheBatch() throws Exception {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(logName.getUrn(), manager, IdMessageFactory.NOOP,
                ConsumerPolicy.UNBOUNDED);

        // run the consumers pool
        CompletableFuture<List<ConsumerStatus>> consumersFuture = consumers.start();

        // submit messages
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        appender.append(0, KeyValueMessage.of("id1"));
        // terminate consumers
        appender.append(0, KeyValueMessage.POISON_PILL);
        appender.append(0, KeyValueMessage.of("no consumer to read this one"));
        List<ConsumerStatus> ret = consumersFuture.get();
        assertEquals(LOG_SIZE, ret.size());
        // with Kafka subscribe there is one more commit because of rebalance
        // assertEquals(1, ret.stream().mapToLong(r -> r.batchCommit).sum());
        assertEquals(1, ret.stream().mapToLong(r -> r.committed).sum());
    }

    @Test
    public void killConsumers() throws Exception {
        final int LOG_SIZE = 2;
        manager.createIfNotExists(logName, LOG_SIZE);
        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(logName.getUrn(), manager, IdMessageFactory.NOOP,
                ConsumerPolicy.UNBOUNDED);
        // run the consumers pool
        CompletableFuture<List<ConsumerStatus>> future = consumers.start();

        // submit messages
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        LogOffset offset1 = appender.append(0, KeyValueMessage.of("id1"));

        // may be processed but not committed because batch is not full
        assertFalse(appender.waitFor(offset1, consumers.getConsumerGroupName(), Duration.ofMillis(0)));
        // send a force batch
        appender.append(0, KeyValueMessage.ofForceBatch("batch now"));
        assertTrue(appender.waitFor(offset1, consumers.getConsumerGroupName(), Duration.ofSeconds(10)));

        // send 2 more messages
        appender.append(0, KeyValueMessage.of("foo"));
        appender.append(0, KeyValueMessage.of("foo"));

        // terminate consumers abruptly without committing the last message
        consumers.close();
        List<ConsumerStatus> ret;
        try {
            ret = future.get();
            assertEquals(LOG_SIZE, ret.size());
            assertEquals(2, ret.stream().filter(s -> s.fail).count());
        } catch (ExecutionException e) {
            // When executor is shutdownNow async task can be reported as being rejected
            // see java.util.concurrent.ThreadPoolExecutor#execute last reject case.
            // Also depending on when the thread are interrupted some uncaught error can occur.
            log.warn("Got some execution exception: ", e);
        }

        // reset manager
        resetManager();

        consumers = new ConsumerPool<>(logName.getUrn(), manager, IdMessageFactory.NOOP, ConsumerPolicy.UNBOUNDED);
        // run the consumers pool again
        future = consumers.start();
        appender = manager.getAppender(logName);
        // terminate the consumers with pills
        appender.append(0, KeyValueMessage.POISON_PILL);
        appender.append(1, KeyValueMessage.POISON_PILL);

        ret = future.get();
        // 2 messages from the previous run, poison pill are not counted
        assertEquals(2, ret.stream().mapToLong(r -> r.committed).sum());
    }

    @Test
    public void killLogManager() throws Exception {
        final int LOG_SIZE = 2;
        manager.createIfNotExists(logName, LOG_SIZE);

        ConsumerPool<KeyValueMessage> consumers = new ConsumerPool<>(logName.getUrn(), manager, IdMessageFactory.NOOP,
                ConsumerPolicy.UNBOUNDED);
        // run the consumers pool
        CompletableFuture<List<ConsumerStatus>> future = consumers.start();

        // submit messages
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        LogOffset offset1 = appender.append(0, KeyValueMessage.of("id1"));
        LogOffset offset2 = appender.append(1, KeyValueMessage.of("id2"));
        // may be processed but not committed because batch is not full
        assertFalse(appender.waitFor(offset1, consumers.getConsumerGroupName(), Duration.ofMillis(0)));
        // send a force batch
        appender.append(0, KeyValueMessage.ofForceBatch("batch now"));
        appender.append(1, KeyValueMessage.ofForceBatch("batch now"));
        assertTrue(appender.waitFor(offset1, consumers.getConsumerGroupName(), Duration.ofSeconds(10)));
        assertTrue(appender.waitFor(offset2, consumers.getConsumerGroupName(), Duration.ofSeconds(10)));

        // send 2 more messages
        appender.append(0, KeyValueMessage.of("foo"));
        appender.append(0, KeyValueMessage.of("foo"));
        // close the log

        log.warn("Close the LogManager (errors expected)");
        resetManager();

        appender = manager.getAppender(logName);
        // open a new log

        appender.append(0, KeyValueMessage.ofForceBatch("force batch"));

        // the consumers should be in error because their tailer are associated to a closed partition
        List<ConsumerStatus> ret = future.get();
        // 2 failures
        assertEquals(2, ret.stream().filter(r -> r.fail).count());

        // run the consumers pool again
        consumers = new ConsumerPool<>(logName.getUrn(), manager, IdMessageFactory.NOOP, ConsumerPolicy.UNBOUNDED);
        future = consumers.start();
        // terminate the consumers with pills
        // WARN: this will not work with subscribe if the partition are unbalanced
        appender.append(0, KeyValueMessage.POISON_PILL);
        appender.append(1, KeyValueMessage.POISON_PILL);

        ret = future.get();
        // 3 messages from the previous run + 2 poison pills
        assertEquals(3, ret.stream().mapToLong(r -> r.committed).sum());
    }

}
