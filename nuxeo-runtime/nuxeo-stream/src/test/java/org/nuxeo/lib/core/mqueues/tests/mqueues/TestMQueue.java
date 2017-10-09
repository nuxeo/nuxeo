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
package org.nuxeo.lib.core.mqueues.tests.mqueues;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.lib.core.mqueues.mqueues.MQAppender;
import org.nuxeo.lib.core.mqueues.mqueues.MQLag;
import org.nuxeo.lib.core.mqueues.mqueues.MQManager;
import org.nuxeo.lib.core.mqueues.mqueues.MQOffset;
import org.nuxeo.lib.core.mqueues.mqueues.MQPartition;
import org.nuxeo.lib.core.mqueues.mqueues.MQRebalanceException;
import org.nuxeo.lib.core.mqueues.mqueues.MQRecord;
import org.nuxeo.lib.core.mqueues.mqueues.MQTailer;
import org.nuxeo.lib.core.mqueues.pattern.KeyValueMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class TestMQueue {
    protected static final Log log = LogFactory.getLog(TestMQueue.class);
    protected String mqName = "mqName";
    protected static final Duration DEF_TIMEOUT = Duration.ofSeconds(1);
    protected static final Duration SMALL_TIMEOUT = Duration.ofMillis(10);

    @Rule
    public
    TestName name = new TestName();

    protected MQManager manager;

    public abstract MQManager createManager() throws Exception;

    @Before
    public void initManager() throws Exception {
        mqName = name.getMethodName();
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
    public void testCreateAndOpen() throws Exception {
        final int NB_QUEUES = 5;

        // check that the number of queues is persisted even if we don't write anything
        assertFalse(manager.exists(mqName));
        assertTrue(manager.createIfNotExists(mqName, NB_QUEUES));
        assertTrue(manager.exists(mqName));
        assertEquals(NB_QUEUES, manager.getAppender(mqName).size());

        resetManager();
        assertTrue(manager.exists(mqName));
        assertEquals(NB_QUEUES, manager.getAppender(mqName).size());

        resetManager();
        // this should have no effect
        assertFalse(manager.createIfNotExists(mqName, 1));
        assertEquals(NB_QUEUES, manager.getAppender(mqName).size());

    }

    @Test
    public void testGetAppender() throws Exception {
        final int NB_QUEUES = 5;
        manager.createIfNotExists(mqName, NB_QUEUES);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);
        assertNotNull(appender);
        assertFalse(appender.closed());
        assertEquals(mqName, appender.name());
        assertEquals(NB_QUEUES, appender.size());
        assertNotNull(appender.append(0, KeyValueMessage.of("foo")));

        try {
            manager.getAppender("unknown_mqueue");
            fail("Accessing an unknown mqueue should have raise an exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void closingManagerShouldCloseTailersAndAppenders() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);
        appender.append(0, KeyValueMessage.of("foo"));
        MQTailer<KeyValueMessage> tailer = manager.createTailer("defaultTest", MQPartition.of(mqName, 0));
        assertNotNull(tailer.read(DEF_TIMEOUT));

        resetManager();

        assertTrue(appender.closed());
        assertTrue(tailer.closed());
    }


    @Test
    public void canNotAppendOnClosedAppender() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);
        assertFalse(appender.closed());
        appender.close();
        assertTrue(appender.closed());
        try {
            appender.append(0, KeyValueMessage.of("foo"));
            fail("Can not append on closed appender");
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testCreateTailer() throws Exception {
        final int NB_QUEUES = 5;
        final String group = "defaultTest";
        final MQPartition partition = MQPartition.of(mqName, 1);

        manager.createIfNotExists(mqName, NB_QUEUES);

        MQTailer<KeyValueMessage> tailer = manager.createTailer(group, partition);
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
        tailer = manager.createTailer(group, mqName);
        assertNotNull(tailer);
        assertEquals(NB_QUEUES, tailer.assignments().size());
        tailer.toEnd();
        tailer.toStart();
        tailer.toLastCommitted();
        tailer.commit();

        try {
            manager.createTailer(group, MQPartition.of("unknown_mqueue", 1));
            fail("Accessing an unknown mqueue should have raise an exception");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            manager.createTailer(group, MQPartition.of(mqName, 100));
            fail("Should have raise an exception");
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // expected invalid partition
        }


    }

    @Test
    public void canNotTailOnClosedTailer() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQTailer<KeyValueMessage> tailer = manager.createTailer("defaultTest", MQPartition.of(mqName, 0));
        assertFalse(tailer.closed());
        tailer.close();
        assertTrue(tailer.closed());

        try {
            tailer.read(SMALL_TIMEOUT);
            fail("Can not tail on closed tailer");
        } catch (IllegalStateException e) {
            // expected
        }
    }


    @Test
    public void canNotOpeningTwiceTheSameTailer() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);

        MQTailer<KeyValueMessage> tailer = manager.createTailer("defaultTest", MQPartition.of(mqName, 0));
        assertEquals("defaultTest", tailer.group());
        try {
            MQTailer<KeyValueMessage> sameTailer = manager.createTailer("defaultTest", MQPartition.of(mqName, 0));
            fail("Opening twice a tailer is not allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // using a different group is ok
        MQTailer<KeyValueMessage> tailer2 = manager.createTailer("anotherGroup", MQPartition.of(mqName, 0));
        assertEquals("anotherGroup", tailer2.group());
    }


    @Test
    public void basicAppendAndTail() throws Exception {
        final int NB_QUEUE = 10;
        final String GROUP = "defaultTest";

        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);

        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        appender.append(1, msg1);

        try (MQTailer<KeyValueMessage> tailer1 = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
            assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(null, tailer1.read(SMALL_TIMEOUT));

            // add message on another partition
            appender.append(2, msg2);
            assertEquals(null, tailer1.read(SMALL_TIMEOUT));

            appender.append(1, msg2);
            assertEquals(msg2, tailer1.read(DEF_TIMEOUT).message());
        }

        try (MQTailer<KeyValueMessage> tailer2 = manager.createTailer(GROUP, MQPartition.of(mqName, 2))) {
            // with tailer2 we can read msg2
            assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        }

        // open again the tailers, they should starts at the beginning because tailers has not committed their positions
        try (MQTailer<KeyValueMessage> tailer1 = manager.createTailer(GROUP, MQPartition.of(mqName, 1));
             MQTailer<KeyValueMessage> tailer2 = manager.createTailer(GROUP, MQPartition.of(mqName, 2))) {
            assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(msg2, tailer1.read(DEF_TIMEOUT).message());
            assertEquals(null, tailer1.read(SMALL_TIMEOUT));

            assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
            assertEquals(null, tailer2.read(SMALL_TIMEOUT));
        }

        // consumers didn't commit, there are 3 messages
        assertEquals(MQLag.of(3), manager.getLag(mqName, GROUP));
    }


    @Test
    public void testCommitAndSeek() throws Exception {
        final int NB_QUEUE = 10;
        final String GROUP = "defaultTest";

        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);

        // Create a queue
        appender.append(1, KeyValueMessage.of("id1"));
        MQOffset offset2 = appender.append(1, KeyValueMessage.of("id2"));
        appender.append(1, KeyValueMessage.of("id3"));

        MQOffset offset4 = appender.append(2, KeyValueMessage.of("id4"));
        appender.append(2, KeyValueMessage.of("id5"));

        // process 2 messages and commit
        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
            //Thread.sleep(10000);
            assertEquals("id2", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
        }

        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 2))) {
            assertEquals("id4", tailer.read(DEF_TIMEOUT).message().key());
            tailer.commit();
            tailer.commit();
        }

        resetManager();

        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
            tailer.toStart();
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());

            tailer.toEnd();
            assertEquals(null, tailer.read(SMALL_TIMEOUT));

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
        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 2))) {
            assertEquals("id5", tailer.read(DEF_TIMEOUT).message().key());

            tailer.toStart();
            assertEquals("id4", tailer.read(DEF_TIMEOUT).message().key());
        }

        assertEquals(MQLag.of(3, 5), manager.getLag(mqName, GROUP));
    }


    @Test
    public void testMoreCommit() throws Exception {
        final int NB_QUEUE = 10;
        final String GROUP = "defaultTest";
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);


        appender.append(1, KeyValueMessage.of("id1"));
        appender.append(1, KeyValueMessage.of("id2"));
        appender.append(1, KeyValueMessage.of("id3"));
        appender.append(1, KeyValueMessage.of("id4"));

        assertEquals(MQLag.of(0, 4), manager.getLag(mqName, GROUP));
        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
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
        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
            tailer.toLastCommitted();
            // the last committed message was id1
            assertEquals("id2", tailer.read(DEF_TIMEOUT).message().key());
        }
        assertEquals(MQLag.of(1, 4), manager.getLag(mqName, GROUP));

        // reset offsets
        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
            tailer.reset();
            // the last committed message was id1
            assertEquals("id1", tailer.read(DEF_TIMEOUT).message().key());
        }
        assertEquals(MQLag.of(0, 4), manager.getLag(mqName, GROUP));
    }

    @Test
    public void testCommitWithGroup() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);

        final KeyValueMessage msg1 = KeyValueMessage.of("id1");
        final KeyValueMessage msg2 = KeyValueMessage.of("id2");

        for (int i = 0; i < 10; i++) {
            appender.append(0, KeyValueMessage.of("id" + i));
        }
        // each tailers have distinct commit offsets
        try (MQTailer<KeyValueMessage> tailerA = manager.createTailer("group-a", MQPartition.of(mqName, 0));
             MQTailer<KeyValueMessage> tailerB = manager.createTailer("group-b", MQPartition.of(mqName, 0))) {

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

        try (MQTailer<KeyValueMessage> tailer = manager.createTailer("defaultTest", MQPartition.of(mqName, 0));
             MQTailer<KeyValueMessage> tailerA = manager.createTailer("group-a", MQPartition.of(mqName, 0));
             MQTailer<KeyValueMessage> tailerB = manager.createTailer("group-b", MQPartition.of(mqName, 0))) {
            assertEquals("id0", tailer.read(DEF_TIMEOUT).message().key());
            assertEquals("id2", tailerA.read(DEF_TIMEOUT).message().key());
            assertEquals("id1", tailerB.read(DEF_TIMEOUT).message().key());
        }
        assertEquals(MQLag.of(2, 10), manager.getLag(mqName, "group-a"));
        assertEquals(MQLag.of(1, 10), manager.getLag(mqName, "group-b"));
    }

    @Test
    public void testCommitConcurrently() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);

        for (int i = 0; i < 10; i++) {
            appender.append(0, KeyValueMessage.of("id" + i));
        }
        MQTailer<KeyValueMessage> tailerA = manager.createTailer("group-a", MQPartition.of(mqName, 0));
        MQTailer<KeyValueMessage> tailerB = manager.createTailer("group-b", MQPartition.of(mqName, 0));

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

        assertEquals(MQLag.of(2, 10), manager.getLag(mqName, "group-a"));
        assertEquals(MQLag.of(5, 10), manager.getLag(mqName, "group-b"));
    }


    @Test
    public void waitForConsumer() throws Exception {
        final int NB_QUEUE = 1;
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);

        MQOffset offset = null;
        MQOffset offset0 = null;
        MQOffset offset5 = null;
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
        assertFalse(appender.waitFor(offset, "foo", SMALL_TIMEOUT));
        assertFalse(appender.waitFor(offset0, "foo", SMALL_TIMEOUT));
        assertFalse(appender.waitFor(offset5, "foo", SMALL_TIMEOUT));

        String group = "defaultTest";
        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(group, MQPartition.of(mqName, 0))) {
            tailer.read(DEF_TIMEOUT);
            tailer.commit();

            // msg 0 is processed and committed
            assertTrue(appender.waitFor(offset0, group, DEF_TIMEOUT));
            // msg 5 and last is processed and committed
            assertFalse(appender.waitFor(offset5, group, SMALL_TIMEOUT));
            assertFalse(appender.waitFor(offset, group, SMALL_TIMEOUT));

            // drain
            while (tailer.read(DEF_TIMEOUT) != null) ;

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
        final int NB_QUEUE = 2;
        final String group = "defaultTest";
        final String mqName1 = mqName + "1";
        final String mqName2 = mqName + "2";
        final Collection<MQPartition> partitions1 = new ArrayList<>();
        final Collection<MQPartition> partitions2 = new ArrayList<>();
        final KeyValueMessage msg1 = KeyValueMessage.of("id1");
        final KeyValueMessage msg2 = KeyValueMessage.of("id2");
        // create mqueue
        manager.createIfNotExists(mqName1, NB_QUEUE);
        manager.createIfNotExists(mqName2, NB_QUEUE);
        // init tailers
        partitions1.add(MQPartition.of(mqName1, 0));
        partitions1.add(MQPartition.of(mqName2, 0));
        partitions2.add(MQPartition.of(mqName1, 1));
        partitions2.add(MQPartition.of(mqName2, 1));
        MQTailer<KeyValueMessage> tailer1 = manager.createTailer(group, partitions1);
        MQTailer<KeyValueMessage> tailer2 = manager.createTailer(group, partitions2);
        assertEquals(partitions1, tailer1.assignments());
        assertEquals(partitions2, tailer2.assignments());
        // append some msg
        MQAppender<KeyValueMessage> appender1 = manager.getAppender(mqName1);
        MQAppender<KeyValueMessage> appender2 = manager.getAppender(mqName2);

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
        assertEquals(null, tailer1.read(SMALL_TIMEOUT));

        tailer1.toLastCommitted(); // replay from the last commit
        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        assertEquals(msg1, tailer1.read(DEF_TIMEOUT).message());
        assertEquals(null, tailer1.read(SMALL_TIMEOUT));
        tailer1.commit();

        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(null, tailer2.read(SMALL_TIMEOUT));
        tailer2.toStart(); // replay again
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(msg2, tailer2.read(DEF_TIMEOUT).message());
        assertEquals(null, tailer2.read(SMALL_TIMEOUT));

        resetManager();

        // using another tailer with different assignment but same group, the committed offset are preserved
        MQTailer<KeyValueMessage> tailer3 = manager.createTailer(group, MQPartition.of(mqName1, 0));
        assertEquals(null, tailer3.read(SMALL_TIMEOUT));
        MQTailer<KeyValueMessage> tailer4 = manager.createTailer(group, MQPartition.of(mqName1, 1));
        assertEquals(msg2, tailer4.read(DEF_TIMEOUT).message());


    }


    @Test
    public void testTailerOnMultiPartitionsUnbalanced() throws Exception {
        final int NB_QUEUE = 10;
        final int NB_MSG = 50;
        final String group = "defaultTest";
        final Collection<MQPartition> partitions = new ArrayList<>();
        final KeyValueMessage msg1 = KeyValueMessage.of("id1");
        // create mqueue
        manager.createIfNotExists(mqName, NB_QUEUE);
        // init tailers
        for (int i = 0; i < NB_QUEUE; i++) {
            partitions.add(MQPartition.of(mqName, i));
        }
        MQTailer<KeyValueMessage> tailer = manager.createTailer(group, partitions);
        assertEquals(partitions, tailer.assignments());
        // append all message into a single partition
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);
        for (int i = 0; i < NB_MSG; i++) {
            appender.append(1, msg1);
        }
        //appender.append(0, msg1);
        boolean stop = false;
        int i = 0;
        do {
            MQRecord<KeyValueMessage> record = tailer.read(DEF_TIMEOUT);
            if (record == null) {
                stop = true;
            } else {
                // System.out.println(record.value());
                assertEquals(msg1, record.message());
                i++;
            }
        } while (!stop);
        assertEquals(NB_MSG, i);

    }

    @Test
    public void testLag() throws Exception {
        final int NB_QUEUE = 5;
        final String GROUP = "defaultTest";
        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);

        assertEquals(MQLag.of(0), manager.getLag(mqName, "unknown-group"));
        appender.append(1, msg1);

        assertEquals(MQLag.of(1), manager.getLag(mqName, "unknown-group"));

        try (MQTailer<KeyValueMessage> tailer = manager.createTailer(GROUP, MQPartition.of(mqName, 1))) {
            assertEquals(msg1, tailer.read(DEF_TIMEOUT).message());
            assertEquals(MQLag.of(1), manager.getLag(mqName, GROUP));
            tailer.commit();
        }
        assertEquals(MQLag.of(1), manager.getLag(mqName, "unknown-group"));
        assertEquals(MQLag.of(0), manager.getLag(mqName, GROUP));
    }

    @Test
    public void testListAll() throws Exception {
        final int NB_QUEUES = 2;

        assertTrue(manager.createIfNotExists(mqName, NB_QUEUES));
        String mqName2 = mqName + "2";
        assertTrue(manager.createIfNotExists(mqName2, NB_QUEUES));

        List<String> mqueues = manager.listAll();
        // System.out.println(mqueues);
        assertFalse(mqueues.isEmpty());
        assertTrue(mqueues.toString(), mqueues.contains(mqName));
        assertTrue(mqueues.toString(), mqueues.contains(mqName2));
    }


    @Test
    public void testListConsumerGroups() throws Exception {
        final int NB_QUEUE = 1;
        final String GROUP1 = "group1";
        final String GROUP2 = "group2";

        manager.createIfNotExists(mqName, NB_QUEUE);
        MQAppender<KeyValueMessage> appender = manager.getAppender(mqName);
        appender.append(0, KeyValueMessage.of("id1"));
        appender.append(0, KeyValueMessage.of("id2"));
        appender.append(0, KeyValueMessage.of("id3"));

        MQTailer<KeyValueMessage> tailer;
        MQTailer<KeyValueMessage> tailer2;
        if (manager.supportSubscribe()) {
            tailer = manager.subscribe(GROUP1, Collections.singleton(mqName), null);
            tailer2 = manager.subscribe(GROUP2, Collections.singleton(mqName), null);
        } else {
            tailer = manager.createTailer(GROUP1, MQPartition.of(mqName, 0));
            tailer2 = manager.createTailer(GROUP2, MQPartition.of(mqName, 0));
        }
        try {
            assertEquals("id1", readKey(tailer));
            assertEquals("id2", readKey(tailer));
            tailer.commit();
            assertEquals("id1", readKey(tailer2));
            tailer2.commit();
            List<String> groups = manager.listConsumerGroups(mqName);
            assertFalse(groups.isEmpty());
            assertTrue(groups.toString(), groups.contains(GROUP1));
            assertTrue(groups.toString(), groups.contains(GROUP2));
            System.out.println(manager.getLagPerPartition(mqName, GROUP1));
        } finally {
            tailer.close();
            tailer2.close();
        }
        // listConsumerLags();
    }

    protected String readKey(MQTailer<KeyValueMessage> tailer) throws InterruptedException {
        try {
            return tailer.read(DEF_TIMEOUT).message().key();
        } catch (MQRebalanceException e) {
            return tailer.read(DEF_TIMEOUT).message().key();
        }
    }

    protected void listConsumerLags() throws Exception {
        List<String> names = manager.listAll();
        for (String name : names) {
            System.out.println("# MQueue: " + name);
            for (String group : manager.listConsumerGroups(name)) {
                System.out.println("## consumer group: " + group);
                List<MQLag> lags = manager.getLagPerPartition(name, group);
                int i = 0;
                for (MQLag lag : lags) {
                    System.out.println(String.format(" partition: %d, position: %d, end: %d, lag: %d",
                            i++, lag.lower(), lag.upper(), lag.lag()));
                }
            }
        }
    }

}
