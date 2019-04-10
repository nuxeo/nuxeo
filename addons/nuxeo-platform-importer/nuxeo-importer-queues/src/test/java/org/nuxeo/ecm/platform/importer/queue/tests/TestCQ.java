package org.nuxeo.ecm.platform.importer.queue.tests;/*
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


import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.TailerState;
import net.openhft.chronicle.queue.impl.StoreFileListener;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.RuntimeExtension;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test the chronicle queue lib.
 *
 * @since 9.1
 */
public class TestCQ implements StoreFileListener {
    protected static final Log log = LogFactory.getLog(TestCQ.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    ChronicleQueue createQueue() throws IOException {
        File path = folder.newFolder("cq");
        deleteDirectory(path);
        return openQueue(path);
    }

    ChronicleQueue openQueue(File path) throws IOException {
        log.debug("Use chronicle queue base: " + path);
        SingleChronicleQueue ret = SingleChronicleQueueBuilder
                .binary(path)
                .rollCycle(RollCycles.TEST_SECONDLY)
                .storeFileListener(this)
                .build();
        assertNotNull(ret);
        return ret;
    }


    public SourceNode poll(ExcerptTailer tailer) {
        try {
            return poll(tailer, 1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("poll timeout", e);
            ExceptionUtils.checkInterrupt(e);
        }
        return null;
    }

    public SourceNode poll(ExcerptTailer tailer, long timeout, TimeUnit unit) throws InterruptedException {
        SourceNode ret = get(tailer);
        if (ret != null) {
            return ret;
        }
        final long deadline = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
        while (ret == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
            ret = get(tailer);
        }
        return ret;
    }

    private SourceNode get(ExcerptTailer tailer) {
        final SourceNode[] ret = new SourceNode[1];
        if (tailer.readDocument(w -> {
            ret[0] = (SourceNode) w.read("node").object();
        })) {
            return ret[0];
        }
        return null;
    }

    public void put(ExcerptAppender app, SourceNode node) throws InterruptedException {
        app.writeDocument(w -> w.write("node").object(node));
    }

    @Test
    public void testSimple() throws Exception {
        try (ChronicleQueue queue = createQueue()) {
            ExcerptAppender app = queue.acquireAppender();
            ExcerptTailer tailer = queue.createTailer().toEnd();
            assertEquals(TailerState.UNINTIALISED, tailer.state());

            SourceNode srcNode = new BuggySourceNode(1, false, false);
            put(app, srcNode);

            SourceNode receiveNode = poll(tailer);
            assertEquals(TailerState.FOUND_CYCLE, tailer.state());
            assertEquals(srcNode, receiveNode);
        }
    }

    @Test
    public void testReopenQueue() throws Exception {
        File path;
        SourceNode srcNode = new BuggySourceNode(1, false, false);
        SourceNode srcNode2 = new BuggySourceNode(2, false, true);
        ExcerptAppender app;
        ExcerptTailer tailer;
        long index = 0;
        try (ChronicleQueue queue = createQueue()) {
            path = queue.file();
            app = queue.acquireAppender();
            tailer = queue.createTailer().toEnd();
            put(app, srcNode);
            put(app, srcNode2);
            assertEquals(srcNode, poll(tailer));
            index = tailer.index();
        }
        // From start
        try (ChronicleQueue queue = openQueue(path)) {
            tailer = queue.createTailer().toStart();
            assertEquals(srcNode, poll(tailer));
            assertEquals(srcNode2, poll(tailer));
        }
        // From end
        try (ChronicleQueue queue = openQueue(path)) {
            tailer = queue.createTailer().toEnd();
            SourceNode receiveNode = poll(tailer, 50, TimeUnit.MILLISECONDS);
            assertNull(receiveNode);
        }
        // From index
        try (ChronicleQueue queue = openQueue(path)) {
            tailer = queue.createTailer();
            tailer.moveToIndex(index);
            assertEquals(srcNode2, poll(tailer));
            assertNull(poll(tailer, 50, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testRollingQueue() throws Exception {
        File path;
        SourceNode srcNode = new BuggySourceNode(1, false, false);
        SourceNode srcNode2 = new BuggySourceNode(2, false, true);
        ExcerptAppender app;
        ExcerptTailer tailer;
        long index = 0;
        try (ChronicleQueue queue = createQueue()) {
            path = queue.file();
            app = queue.acquireAppender();
            tailer = queue.createTailer().toEnd();
            put(app, srcNode);
            // wait a second to roll on a new queue file
            Thread.sleep(1001);
            put(app, srcNode2);
            Thread.sleep(1001);
            put(app, srcNode2);
            assertEquals(srcNode, poll(tailer));
        }
        // From start
        try (ChronicleQueue queue = openQueue(path)) {
            tailer = queue.createTailer().toStart();
            assertEquals(srcNode, poll(tailer));
            assertEquals(srcNode2, poll(tailer));
            Thread.sleep(1000);
            log.debug("Still one");
            assertEquals(srcNode2, poll(tailer));
            log.debug("nothing in queue");
            Thread.sleep(1000);
            log.debug("end");
        }
    }

    @Test
    public void testPollOnClosedQueue() throws Exception {
        SourceNode srcNode = new BuggySourceNode(1, false, false);
        SourceNode srcNode2 = new BuggySourceNode(2, false, true);
        ExcerptAppender app;
        ExcerptTailer tailer;
        try (ChronicleQueue queue = createQueue()) {
            assertFalse(queue.isClosed());
            app = queue.acquireAppender();
            tailer = queue.createTailer().toEnd();

            put(app, srcNode);
            put(app, srcNode2);

            SourceNode receiveNode = poll(tailer);
            assertEquals(srcNode, receiveNode);
        }
        assertTrue(tailer.queue().isClosed());
        assertTrue(app.queue().isClosed());
        // the state does not help
        assertEquals(TailerState.FOUND_CYCLE, tailer.state());
        try {
            SourceNode receiveNode = poll(tailer);
            fail("Excpecting a NPE on closed tailer");
        } catch (NullPointerException exception) {
            // here the queue has been closed the tailer is closed
        }

    }


    @Override
    public void onAcquired(int cycle, File file) {
        log.debug("New file: " + file + " cycle: " + cycle);
    }

    @Override
    public void onReleased(int cycle, File file) {
        log.debug("Release file: " + file + " cycle"  + cycle);
        /*
        // this will not work, a queue file will be released when a queue is closed
        // this does not mean that consumers have read all message
        // so it is better to delete externally the queue
        try {
            forceDelete(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        */

    }
}
