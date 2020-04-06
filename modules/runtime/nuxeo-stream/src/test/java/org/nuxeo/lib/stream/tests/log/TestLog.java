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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.lib.stream.codec.AvroBinaryCodec;
import org.nuxeo.lib.stream.codec.AvroJsonCodec;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.tests.KeyValueMessage;

public abstract class TestLog {
    protected static final Log log = LogFactory.getLog(TestLog.class);

    public static final Duration DEF_TIMEOUT = Duration.ofSeconds(1);

    public static final Duration SMALL_TIMEOUT = Duration.ofMillis(10);

    @Rule
    public TestName name = new TestName();

    protected Name logName = Name.ofUrn("test/logName");

    protected LogManager manager;

    public abstract LogManager createManager() throws Exception;

    @Before
    public void initManager() throws Exception {
        logName = Name.of("test", name.getMethodName());
        if (manager == null) {
            manager = createManager();
        }
    }

    @After
    public void closeManager() {
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
    public void testCreateAndOpen() throws Exception {
        final int LOG_SIZE = 5;

        // check that the number of partitions is persisted even if we don't write anything
        assertFalse(manager.exists(logName));
        assertTrue(manager.createIfNotExists(logName, LOG_SIZE));
        assertTrue(manager.exists(logName));
        assertEquals(LOG_SIZE, manager.size(logName));

        resetManager();
        assertTrue(manager.exists(logName));
        assertEquals(LOG_SIZE, manager.size(logName));

        resetManager();
        // this should have no effect
        assertFalse(manager.createIfNotExists(logName, 1));
        assertEquals(LOG_SIZE, manager.size(logName));
    }

    @Test
    public void testGetAppender() {
        final int LOG_SIZE = 5;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        assertNotNull(appender);
        assertFalse(appender.closed());
        assertEquals(logName, appender.name());
        assertEquals(LOG_SIZE, appender.size());
        assertNotNull(appender.append(0, KeyValueMessage.of("foo")));

        try {
            manager.getAppender(Name.ofUrn("unknown_log_name"));
            fail("Accessing an unknown log should have raise an exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void closingManagerShouldCloseTailersAndAppenders() throws Exception {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        appender.append(0, KeyValueMessage.of("foo"));
        LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/defaultTest"),
                LogPartition.of(logName, 0));
        assertNotNull(tailer.read(DEF_TIMEOUT));

        resetManager();

        assertTrue(appender.closed());
        assertTrue(tailer.closed());
    }

    @Test
    public void canNotAppendOnClosedAppender() {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        assertFalse(appender.closed());

        manager.close();
        assertTrue(appender.closed());
        try {
            appender.append(0, KeyValueMessage.of("foo"));
            fail("Cannot append on closed appender");
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testCreateTailer() {
        final int LOG_SIZES = 5;
        final Name group = Name.of("test", "consumer");
        final LogPartition partition = LogPartition.of(logName, 1);

        manager.createIfNotExists(logName, LOG_SIZES);

        LogTailer<KeyValueMessage> tailer = manager.createTailer(group, partition);
        assertNotNull(tailer);
        assertFalse(tailer.closed());
        assertEquals(group, tailer.group());
        assertEquals(Collections.singletonList(partition), tailer.assignments());
        tailer.toEnd();
        tailer.toStart();
        tailer.toLastCommitted();
        tailer.commit();
        tailer.commit(partition);
        tailer.close();

        // we can also create a tailer with all the partitions
        tailer = manager.createTailer(group, logName);
        assertNotNull(tailer);
        assertEquals(LOG_SIZES, tailer.assignments().size());
        tailer.toEnd();
        tailer.toStart();
        tailer.toLastCommitted();
        tailer.commit();

        try {
            manager.createTailer(group, LogPartition.of(Name.ofUrn("unknown_log_name"), 1));
            fail("Accessing an unknown log should have raise an exception");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            manager.createTailer(group, LogPartition.of(logName, 100));
            fail("Should have raise an exception");
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // expected invalid partition
        }

    }

    @Test
    public void canNotTailOnClosedTailer() throws Exception {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/defaultTest"),
                LogPartition.of(logName, 0));
        assertFalse(tailer.closed());
        tailer.close();
        assertTrue(tailer.closed());

        try {
            tailer.read(SMALL_TIMEOUT);
            fail("Cannot tail on closed tailer");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void canNotOpeningTwiceTheSameTailer() {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);

        Name group = Name.ofUrn("test/defaultTest");
        LogTailer<KeyValueMessage> tailer = manager.createTailer(group, LogPartition.of(logName, 0));
        assertEquals(group, tailer.group());
        try {
            manager.createTailer(group, LogPartition.of(logName, 0));
            fail("Opening twice a tailer is not allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // using a different group is ok
        LogTailer<KeyValueMessage> tailer2 = manager.createTailer(Name.ofUrn("test/anotherGroup"),
                LogPartition.of(logName, 0));
        assertEquals(Name.ofUrn("test/anotherGroup"), tailer2.group());
    }

    @Test
    public void basicAppendAndTail() throws Exception {
        final int LOG_SIZE = 5;
        final Name GROUP = Name.ofUrn("test/defaultTest");

        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        appender.append(1, msg1);

        try (LogTailer<KeyValueMessage> tailer1 = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
            assertNull(tailer1.read(SMALL_TIMEOUT));

            // add message on another partition
            appender.append(2, msg2);
            assertNull(tailer1.read(SMALL_TIMEOUT));

            appender.append(1, msg2);
            assertEquals(msg2, tailer1.read(DEF_TIMEOUT).message());
        }

        try (LogTailer<KeyValueMessage> tailer2 = manager.createTailer(GROUP, LogPartition.of(logName, 2))) {
            // with tailer2 we can read msg2
            assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        }

        // open again the tailers, they should starts at the beginning because tailers has not committed their positions
        try (LogTailer<KeyValueMessage> tailer1 = manager.createTailer(GROUP, LogPartition.of(logName, 1));
                LogTailer<KeyValueMessage> tailer2 = manager.createTailer(GROUP, LogPartition.of(logName, 2))) {
            assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(msg2, tailer1.read(DEF_TIMEOUT).message());
            assertNull(tailer1.read(SMALL_TIMEOUT));

            assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
            assertNull(tailer2.read(SMALL_TIMEOUT));
        }

        // consumers didn't commit, there are 3 messages
        assertEquals(LogLag.of(3), manager.getLag(logName, GROUP));
    }

    @Test
    public void testCommitAndSeek() throws Exception {
        final int LOG_SIZE = 5;
        final Name GROUP = Name.ofUrn("test/defaultTest");

        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        appender.append(1, KeyValueMessage.of("id1"));
        LogOffset offset2 = appender.append(1, KeyValueMessage.of("id2"));
        appender.append(1, KeyValueMessage.of("id3"));

        LogOffset offset4 = appender.append(2, KeyValueMessage.of("id4"));
        appender.append(2, KeyValueMessage.of("id5"));

        // process 2 messages and commit
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
            // Thread.sleep(10000);
            assertEquals("id2", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
        }

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 2))) {
            assertEquals("id4", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
            tailer.commit();
        }

        resetManager();

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            tailer.toStart();
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());

            tailer.toEnd();
            assertNull(tailer.read(SMALL_TIMEOUT));

            tailer.toLastCommitted();
            assertEquals("id3", tailer.read(DEF_TIMEOUT).message().key());

            tailer.seek(offset2);
            assertEquals("id2", tailer.read(DEF_TIMEOUT).message().key());

            try {
                tailer.seek(offset4);
                fail("try to seek on an unassigned partition should raise an exception");
            } catch (IllegalStateException e) {
                // this is expected
            }
        }
        // by default the tailer is open on the last committed message
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 2))) {
            assertEquals("id5", tailer.read(DEF_TIMEOUT).message().key());

            tailer.toStart();
            assertEquals("id4", tailer.read(DEF_TIMEOUT).message().key());
        }

        assertEquals(LogLag.of(3, 5), manager.getLag(logName, GROUP));
    }

    @Test
    public void testMoreCommit() throws Exception {
        final int LOG_SIZE = 5;
        final Name GROUP = Name.ofUrn("test/defaultTest");
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        appender.append(1, KeyValueMessage.of("id1"));
        appender.append(1, KeyValueMessage.of("id2"));
        appender.append(1, KeyValueMessage.of("id3"));
        appender.append(1, KeyValueMessage.of("id4"));

        assertEquals(LogLag.of(0, 4), manager.getLag(logName, GROUP));
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
            assertEquals("id2", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();

            // restart from the beginning and commit after the first message
            tailer.toStart();
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
        }

        // reopen
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            tailer.toLastCommitted();
            // the last committed message was id1
            assertEquals("id2", tailer.read(DEF_TIMEOUT).message().key());
        }
        assertEquals(LogLag.of(1, 4), manager.getLag(logName, GROUP));
        // reset offsets
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            tailer.reset();
            // the last committed message was id1
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
        }
        assertEquals(LogLag.of(0, 4), manager.getLag(logName, GROUP));
    }

    @Test
    public void testCommitWithGroup() throws Exception {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        for (int i = 0; i < 10; i++) {
            appender.append(0, KeyValueMessage.of("id" + i));
        }
        Name groupA = Name.ofUrn("test/group-a");
        Name groupB = Name.ofUrn("test/group-b");
        // each tailers have distinct commit offsets
        try (LogTailer<KeyValueMessage> tailerA = manager.createTailer(groupA, LogPartition.of(logName, 0));
                LogTailer<KeyValueMessage> tailerB = manager.createTailer(groupB, LogPartition.of(logName, 0))) {

            assertEquals("id0", tailerA.read(DEF_TIMEOUT).message().key());
            assertEquals("id1", tailerA.read(DEF_TIMEOUT).message().key());
            tailerA.commit();
            assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
            assertEquals("id3", tailerA.read(DEF_TIMEOUT).message().key());
            tailerA.toLastCommitted();
            assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
            assertEquals("id3", tailerA.read(DEF_TIMEOUT).message().key());

            assertEquals("id0", tailerB.read(DEF_TIMEOUT).message().key());
            tailerB.commit();
            assertEquals("id1", tailerB.read(DEF_TIMEOUT).message().key());
            assertEquals("id2", tailerB.read(DEF_TIMEOUT).message().key());

            tailerB.toLastCommitted();
            assertEquals("id1", tailerB.read(DEF_TIMEOUT).message().key());

            tailerA.toLastCommitted();
            assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
        }

        // reopen
        resetManager();

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/defaultTest"),
                LogPartition.of(logName, 0));
                LogTailer<KeyValueMessage> tailerA = manager.createTailer(groupA, LogPartition.of(logName, 0));
                LogTailer<KeyValueMessage> tailerB = manager.createTailer(groupB, LogPartition.of(logName, 0))) {
            assertEquals("id0", tailer.read(DEF_TIMEOUT).message().key());
            assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
            assertEquals("id1", tailerB.read(DEF_TIMEOUT).message().key());
        }
        assertEquals(LogLag.of(2, 10), manager.getLag(logName, groupA));
        assertEquals(LogLag.of(1, 10), manager.getLag(logName, groupB));
    }

    @Test
    public void testCommitConcurrently() throws Exception {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        for (int i = 0; i < 10; i++) {
            appender.append(0, KeyValueMessage.of("id" + i));
        }
        LogTailer<KeyValueMessage> tailerA = manager.createTailer(Name.ofUrn("test/group-a"),
                LogPartition.of(logName, 0));
        LogTailer<KeyValueMessage> tailerB = manager.createTailer(Name.ofUrn("test/group-b"),
                LogPartition.of(logName, 0));

        assertEquals("id0", tailerA.read(DEF_TIMEOUT).message().key());
        assertEquals("id0", tailerB.read(DEF_TIMEOUT).message().key());

        assertEquals("id1", tailerA.read(DEF_TIMEOUT).message().key());
        tailerA.commit();
        tailerB.commit();

        assertEquals("id1", tailerB.read(DEF_TIMEOUT).message().key());
        assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
        assertEquals("id2", tailerB.read(DEF_TIMEOUT).message().key());
        assertEquals("id3", tailerB.read(DEF_TIMEOUT).message().key());
        assertEquals("id4", tailerB.read(DEF_TIMEOUT).message().key());
        tailerB.commit();

        tailerA.toLastCommitted();
        tailerB.toStart();
        assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
        assertEquals("id0", tailerB.read(DEF_TIMEOUT).message().key());

        tailerB.toLastCommitted();
        assertEquals("id5", tailerB.read(DEF_TIMEOUT).message().key());

        tailerA.close();
        tailerB.close();

        assertEquals(LogLag.of(2, 10), manager.getLag(logName, Name.ofUrn("test/group-a")));
        assertEquals(LogLag.of(5, 10), manager.getLag(logName, Name.ofUrn("test/group-b")));
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    public void waitForConsumer() throws Exception {
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        LogOffset offset = null;
        LogOffset offset0 = null;
        LogOffset offset5 = null;
        // appends some msg and keep some offsets
        for (int i = 0; i < 10; i++) {
            offset = appender.append(0, KeyValueMessage.of("id" + i));
            if (i == 0) {
                offset0 = offset;
            } else if (i == 5) {
                offset5 = offset;
            }
        }
        // nothing committed
        assertFalse(appender.waitFor(offset, Name.ofUrn("test/foo"), SMALL_TIMEOUT));
        assertFalse(appender.waitFor(offset0, Name.ofUrn("test/foo"), SMALL_TIMEOUT));
        assertFalse(appender.waitFor(offset5, Name.ofUrn("test/foo"), SMALL_TIMEOUT));

        Name group = Name.ofUrn("test/defaultTest");
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(group, LogPartition.of(logName, 0))) {
            tailer.read(DEF_TIMEOUT);
            tailer.commit();

            // msg 0 is processed and committed
            assertTrue(appender.waitFor(offset0, group, DEF_TIMEOUT));
            // msg 5 and last is processed and committed
            assertFalse(appender.waitFor(offset5, group, SMALL_TIMEOUT));
            assertFalse(appender.waitFor(offset, group, SMALL_TIMEOUT));

            while (tailer.read(DEF_TIMEOUT) != null) {
                // drain
            }

            // message is processed but not yet committed
            assertFalse(appender.waitFor(offset, group, SMALL_TIMEOUT));
            tailer.commit();
        }

        // message is processed and committed
        assertTrue(appender.waitFor(offset0, group, DEF_TIMEOUT));
        assertTrue(appender.waitFor(offset5, group, DEF_TIMEOUT));
        assertTrue(appender.waitFor(offset, group, DEF_TIMEOUT));

    }

    @Test
    public void testTailerOnMultiPartitions() throws Exception {
        final int LOG_SIZE = 2;
        final Name group = Name.ofUrn("test/defaultTest");
        final Name logName1 = Name.ofUrn(logName.getUrn() + "1");
        final Name logName2 = Name.ofUrn(logName.getUrn() + "2");
        final Collection<LogPartition> partitions1 = new ArrayList<>();
        final Collection<LogPartition> partitions2 = new ArrayList<>();
        final KeyValueMessage msg1 = KeyValueMessage.of("id1");
        final KeyValueMessage msg2 = KeyValueMessage.of("id2");
        // create logs
        manager.createIfNotExists(logName1, LOG_SIZE);
        manager.createIfNotExists(logName2, LOG_SIZE);
        // init tailers
        partitions1.add(LogPartition.of(logName1, 0));
        partitions1.add(LogPartition.of(logName2, 0));
        partitions2.add(LogPartition.of(logName1, 1));
        partitions2.add(LogPartition.of(logName2, 1));
        LogTailer<KeyValueMessage> tailer1 = manager.createTailer(group, partitions1);
        LogTailer<KeyValueMessage> tailer2 = manager.createTailer(group, partitions2);
        assertEquals(partitions1, tailer1.assignments());
        assertEquals(partitions2, tailer2.assignments());
        // append some msg
        LogAppender<KeyValueMessage> appender1 = manager.getAppender(logName1);
        LogAppender<KeyValueMessage> appender2 = manager.getAppender(logName2);

        appender1.append(0, msg1);
        appender1.append(0, msg1);
        appender2.append(0, msg1);

        appender1.append(1, msg2);
        appender2.append(1, msg2);
        appender2.append(1, msg2);

        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        tailer1.commit();

        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        assertNull(tailer1.read(SMALL_TIMEOUT));

        tailer1.toLastCommitted(); // replay from the last commit
        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        assertNull(tailer1.read(SMALL_TIMEOUT));
        tailer1.commit();

        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertNull(tailer2.read(SMALL_TIMEOUT));
        tailer2.toStart(); // replay again
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertNull(tailer2.read(SMALL_TIMEOUT));

        resetManager();

        // using another tailer with different assignment but same group, the committed offset are preserved
        LogTailer<KeyValueMessage> tailer3 = manager.createTailer(group, LogPartition.of(logName1, 0));
        assertNull(tailer3.read(SMALL_TIMEOUT));
        LogTailer<KeyValueMessage> tailer4 = manager.createTailer(group, LogPartition.of(logName1, 1));
        assertEquals(msg2, tailer4.read(DEF_TIMEOUT).message());

    }

    @Test
    public void testTailerOnMultiPartitionsUnbalanced() throws Exception {
        final int LOG_SIZE = 5;
        final int NB_MSG = 50;
        final Name group = Name.ofUrn("test/defaultTest");
        final Collection<LogPartition> partitions = new ArrayList<>();
        final KeyValueMessage msg1 = KeyValueMessage.of("id1");
        // create log
        manager.createIfNotExists(logName, LOG_SIZE);
        // init tailers
        for (int i = 0; i < LOG_SIZE; i++) {
            partitions.add(LogPartition.of(logName, i));
        }
        LogTailer<KeyValueMessage> tailer = manager.createTailer(group, partitions);
        assertEquals(partitions, tailer.assignments());
        // append all message into a single partition
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        for (int i = 0; i < NB_MSG; i++) {
            appender.append(1, msg1);
        }
        // appender.append(0, msg1);
        boolean stop = false;
        int i = 0;
        do {
            LogRecord<KeyValueMessage> record = tailer.read(DEF_TIMEOUT);
            if (record == null) {
                stop = true;
            } else {
                assertEquals(msg1, record.message());
                i++;
            }
        } while (!stop);
        assertEquals(NB_MSG, i);

    }

    @Test
    public void testLag() throws Exception {
        final int LOG_SIZE = 5;
        final Name GROUP = Name.ofUrn("test/defaultTest");
        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        assertEquals(LogLag.of(0), manager.getLag(logName, Name.ofUrn("unknownGroup")));
        appender.append(1, msg1);

        assertEquals(LogLag.of(1), manager.getLag(logName, Name.ofUrn("unknownGroup")));

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, LogPartition.of(logName, 1))) {
            assertEquals(msg1, tailer.read(DEF_TIMEOUT).message());
            assertEquals(LogLag.of(1), manager.getLag(logName, GROUP));
            tailer.commit();
        }
        assertEquals(LogLag.of(1), manager.getLag(logName, Name.ofUrn("unknownGroup")));
        assertEquals(LogLag.of(0), manager.getLag(logName, GROUP));
    }

    @Test
    public void testLagLeak() {
        final int LOG_SIZE = 1;
        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        assertEquals(LogLag.of(0), manager.getLag(logName, Name.ofUrn("unknownGroup")));
        appender.append(0, msg1);
        for (int i = 0; i < 100; i++) {
            assertEquals(LogLag.of(1), manager.getLag(logName, Name.ofUrn("unknownGroup")));
        }
    }

    @Test
    public void testListAll() {
        final int LOG_SIZES = 2;

        assertTrue(manager.createIfNotExists(logName, LOG_SIZES));
        Name logName2 = Name.ofUrn(logName.getUrn() + "2");
        assertTrue(manager.createIfNotExists(logName2, LOG_SIZES));

        List<Name> logs = manager.listAll();
        // System.out.println(logs);
        assertFalse(logs.isEmpty());
        assertTrue(logs.toString(), logs.contains(logName));
        assertTrue(logs.toString(), logs.contains(logName2));
    }

    @Test
    public void testListConsumerGroups() throws Exception {
        final int LOG_SIZE = 1;
        final Name GROUP1 = Name.ofUrn("test/group1");
        final Name GROUP2 = Name.ofUrn("test/group2");

        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        appender.append(0, KeyValueMessage.of("id1"));
        appender.append(0, KeyValueMessage.of("id2"));
        appender.append(0, KeyValueMessage.of("id3"));

        LogTailer<KeyValueMessage> tailer;
        LogTailer<KeyValueMessage> tailer2;
        if (manager.supportSubscribe()) {
            tailer = manager.subscribe(GROUP1, Collections.singleton(logName), null);
            tailer2 = manager.subscribe(GROUP2, Collections.singleton(logName), null);
        } else {
            tailer = manager.createTailer(GROUP1, LogPartition.of(logName, 0));
            tailer2 = manager.createTailer(GROUP2, LogPartition.of(logName, 0));
        }
        try {
            assertEquals("id1", readKey(tailer));
            assertEquals("id2", readKey(tailer));
            tailer.commit();
            assertEquals("id1", readKey(tailer2));
            tailer2.commit();
            List<Name> groups = manager.listConsumerGroups(logName);
            assertFalse(groups.isEmpty());
            assertTrue(groups.toString(), groups.contains(GROUP1));
            assertTrue(groups.toString(), groups.contains(GROUP2));
            // System.out.println(manager.getLagPerPartition(logName, GROUP1));
        } finally {
            tailer.close();
            tailer2.close();
        }
        // listConsumerLags();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Test
    public void testConcurrentAppenders() throws Exception {
        final int NB_APPENDERS = 4;
        final int NB_MSG = 100;
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        ExecutorService executor = Executors.newFixedThreadPool(NB_APPENDERS);
        Runnable writer = () -> {
            for (int i = 0; i < NB_MSG; i++) {
                appender.append(0, KeyValueMessage.of("msg" + i));
            }
        };
        for (int i = 0; i < NB_APPENDERS; i++) {
            executor.submit(writer);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        assertEquals(LogLag.of(NB_APPENDERS * NB_MSG), manager.getLag(logName, Name.ofUrn("test/counter")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLatencies() throws Exception {
        final int LOG_SIZE = 5;
        final Name GROUP1 = Name.ofUrn("test/latGroup");
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<Record> appender = manager.getAppender(logName);
        // try to check all possible positions in a partition
        appender.append(0, createRecord("first"));
        appender.append(0, createRecord("here"));
        appender.append(0, createRecord("end"));

        appender.append(1, createRecord("first"));
        appender.append(1, createRecord("here"));

        appender.append(2, createRecord("here"));
        appender.append(2, createRecord("end"));

        appender.append(3, createRecord("first"));
        // partition 4 is empty

        try (LogTailer<Record> tailer0 = manager.createTailer(GROUP1, new LogPartition(logName, 0));
                LogTailer<Record> tailer1 = manager.createTailer(GROUP1, new LogPartition(logName, 1));
                LogTailer<Record> tailer2 = manager.createTailer(GROUP1, new LogPartition(logName, 2))) {
            // position in the middle
            assertRecordKeyEquals("first", tailer0.read(DEF_TIMEOUT));
            assertRecordKeyEquals("here", tailer0.read(DEF_TIMEOUT));
            // position on last record
            assertRecordKeyEquals("first", tailer1.read(DEF_TIMEOUT));
            assertRecordKeyEquals("here", tailer1.read(DEF_TIMEOUT));
            // position on first record
            assertRecordKeyEquals("here", tailer2.read(DEF_TIMEOUT));
            // position nothing processed in partition 3
            // position nothing processed on empty partition 4
            tailer0.commit();
            tailer1.commit();
            tailer2.commit();
        }
        List<Latency> latencies = manager.getLatencyPerPartition(logName, GROUP1, NO_CODEC,
                (rec -> Watermark.ofValue(rec.getWatermark()).getTimestamp()), (Record::getKey));
        // check that the latency point to the last committed keys
        assertEquals(latencies.get(0).toString(), "here", latencies.get(0).key());
        assertNull(latencies.get(1).toString(), latencies.get(1).key());
        assertEquals(latencies.get(2).toString(), "here", latencies.get(2).key());
        assertNull(latencies.get(3).toString(), latencies.get(3).key());
        assertNull(latencies.get(4).toString(), latencies.get(4).key());
        Latency latency = Latency.of(latencies);
        // there are 3 un read records
        assertEquals(3, latency.lag().lag());
        // System.out.println(latency);
    }

    protected void assertRecordKeyEquals(String expectedKey, LogRecord<Record> record) {
        assertNotNull(record);
        assertEquals(expectedKey, record.message().getKey());
    }

    protected Record createRecord(String key) throws UnsupportedEncodingException {
        return Record.of(key, ("value" + key).getBytes(UTF_8));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNoCodec() throws Exception {
        testCodec(NO_CODEC);
    }

    @Test
    public void testSerializableCodec() throws Exception {
        Codec<KeyValueMessage> codec = new SerializableCodec<>();
        testCodec(codec);
    }

    @Test
    public void testAvroBinaryCodec() throws Exception {
        Codec<KeyValueMessage> codec = new AvroBinaryCodec<>(KeyValueMessage.class);
        testCodec(codec);
    }

    @Test
    public void testAvroJsonCodec() throws Exception {
        Codec<KeyValueMessage> codec = new AvroJsonCodec<>(KeyValueMessage.class);
        testCodec(codec);
    }

    @Test
    public void testAvroMessageCodec() throws Exception {
        Codec<KeyValueMessage> codec = new AvroMessageCodec<>(KeyValueMessage.class);
        testCodec(codec);
    }

    protected void testCodec(Codec<KeyValueMessage> codec) throws Exception {
        final int LOG_SIZE = 1;
        final Name GROUP = Name.ofUrn("test/defaultTest");
        manager.createIfNotExists(logName, LOG_SIZE);

        LogAppender<KeyValueMessage> appender = manager.getAppender(logName, codec);
        KeyValueMessage msg1 = KeyValueMessage.ofForceBatch("key", "value".getBytes(UTF_8));
        KeyValueMessage msg2 = KeyValueMessage.of("id2", "foo".getBytes(UTF_8));
        KeyValueMessage msg3 = KeyValueMessage.of("1234567890", "0987654321".getBytes(UTF_8));
        appender.append(0, msg1);
        appender.append(0, msg2);
        appender.append(0, msg3);
        appender.append(0, msg1);

        try (LogTailer<KeyValueMessage> tailer1 = manager.createTailer(GROUP, LogPartition.of(logName, 0), codec)) {
            assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(msg2, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(msg3, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
            assertNull(tailer1.read(SMALL_TIMEOUT));
        }
    }

    @Test
    public void testCodecCheck() throws Exception {
        final int LOG_SIZE = 1;
        final Name GROUP = Name.ofUrn("test/defaultTest");
        manager.createIfNotExists(logName, LOG_SIZE);
        KeyValueMessage msg1 = KeyValueMessage.of("1234567890", "0987654321".getBytes(UTF_8));

        Codec<KeyValueMessage> codec1 = new AvroBinaryCodec<>(KeyValueMessage.class);
        Codec<KeyValueMessage> codec2 = new SerializableCodec<>();

        LogAppender<KeyValueMessage> appender = manager.getAppender(logName, codec1);
        assertEquals(codec1.getName(), appender.getCodec().getName());
        appender.append(0, msg1);
        try {
            LogAppender<KeyValueMessage> wrongAppender = manager.getAppender(logName, codec2);
            wrongAppender.append(0, msg1);
            fail("Should not be possible to open an appender with a wrong codec on the same manager");
        } catch (IllegalArgumentException e) {
            // expected
        }
        Codec<KeyValueMessage> codec1Bis = new AvroBinaryCodec<>(KeyValueMessage.class);
        LogAppender<KeyValueMessage> goodAppender = manager.getAppender(logName, codec1Bis);
        goodAppender.append(0, msg1);
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logName, codec2)) {
            tailer.read(DEF_TIMEOUT);
            fail("Should not be possible to read with a wrong codec");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // the encoding is known by the log manager
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logName)) {
            assertEquals(codec1.getName(), tailer.getCodec().getName());
        }
        // reset the log manager, so read on legacy
        resetManager();
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logName)) {
            tailer.read(DEF_TIMEOUT);
            fail("Should not be possible to read with a wrong codec");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logName)) {
            tailer.read(DEF_TIMEOUT);
            fail("Should not be possible to read with a wrong legacy codec");
        } catch (IllegalArgumentException e) {
            // expected
        }
        resetManager();
        // works only with the explicit good codec
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, logName, codec1)) {
            assertEquals(msg1, tailer.read(DEF_TIMEOUT).message());
            tailer.read(DEF_TIMEOUT);
        }

    }

    @Test
    public void testInitialOffset() throws Exception {
        final int LOG_SIZE = 1;
        KeyValueMessage msg1 = KeyValueMessage.of("1234567890", "0987654321".getBytes(UTF_8));
        assertTrue(manager.createIfNotExists(logName, LOG_SIZE));
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/someGroup"), logName)) {
            LogOffset offset = appender.append("foo", msg1);
            LogRecord<KeyValueMessage> record = tailer.read(Duration.ofSeconds(1));
            assertEquals(offset, record.offset());
        }
    }

    protected String readKey(LogTailer<KeyValueMessage> tailer) throws InterruptedException {
        try {
            return tailer.read(DEF_TIMEOUT).message().key();
        } catch (RebalanceException e) {
            return tailer.read(DEF_TIMEOUT).message().key();
        }
    }

}
