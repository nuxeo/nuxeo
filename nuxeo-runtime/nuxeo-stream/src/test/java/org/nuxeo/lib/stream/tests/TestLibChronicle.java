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

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.TailerState;
import net.openhft.chronicle.queue.impl.StoreFileListener;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

/**
 * Unit Test to learn the Chronicle Queue lib.
 *
 * @since 9.3
 */
@SuppressWarnings("squid:S2925")
public class TestLibChronicle implements StoreFileListener {
    protected static final Log log = LogFactory.getLog(TestLibChronicle.class);

    protected final static String OS = System.getProperty("os.name").toLowerCase();

    public final static boolean IS_WIN = OS.startsWith("win");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void skipWindowsThatDoNotCleanTempFolder() {
        assumeFalse(IS_WIN);
    }

    ChronicleQueue createQueue() throws IOException {
        File path = folder.newFolder("cq");
        deleteDirectory(path);
        return openQueue(path);
    }

    ChronicleQueue openQueue(File path) {
        log.debug("Use chronicle queue base: " + path);
        SingleChronicleQueue ret = SingleChronicleQueueBuilder.binary(path)
                                                              .rollCycle(RollCycles.TEST_SECONDLY)
                                                              .storeFileListener(this)
                                                              .build();
        assertNotNull(ret);
        return ret;
    }

    public KeyValueMessage poll(ExcerptTailer tailer) {
        try {
            return poll(tailer, 1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("poll timeout", e);
        }
        return null;
    }

    public KeyValueMessage poll(ExcerptTailer tailer, long timeout, TimeUnit unit) throws InterruptedException {
        KeyValueMessage ret = get(tailer);
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

    protected KeyValueMessage get(ExcerptTailer tailer) {
        final KeyValueMessage[] ret = new KeyValueMessage[1];
        if (tailer.readDocument(w -> ret[0] = (KeyValueMessage) w.read("node").object())) {
            return ret[0];
        }
        return null;
    }

    public void put(ExcerptAppender app, KeyValueMessage node) {
        app.writeDocument(w -> w.write("node").object(node));
    }

    @Test
    public void testSimple() throws Exception {
        try (ChronicleQueue queue = createQueue()) {
            ExcerptAppender app = queue.acquireAppender();
            ExcerptTailer tailer = queue.createTailer().toEnd();
            // assertEquals(TailerState.UNINITIALISED, tailer.state());

            KeyValueMessage srcNode = KeyValueMessage.of("test");
            put(app, srcNode);

            KeyValueMessage receiveNode = poll(tailer);
            assertEquals(TailerState.FOUND_CYCLE, tailer.state());
            assertEquals(srcNode, receiveNode);
        }
    }

    @Test
    public void testReopenQueue() throws Exception {
        File path;
        KeyValueMessage srcNode = KeyValueMessage.of("node1");
        KeyValueMessage srcNode2 = KeyValueMessage.of("node2");
        ExcerptAppender app;
        ExcerptTailer tailer;
        long index;
        try (ChronicleQueue queue = createQueue()) {
            path = queue.file();
            app = queue.acquireAppender();
            tailer = queue.createTailer().toEnd();
            put(app, srcNode);
            put(app, srcNode2);
            assertEquals(srcNode, poll(tailer));
            index = tailer.index();
        }
        // From startTime
        try (ChronicleQueue queue = openQueue(path)) {
            tailer = queue.createTailer().toStart();
            assertEquals(srcNode, poll(tailer));
            assertEquals(srcNode2, poll(tailer));
        }
        // From end
        try (ChronicleQueue queue = openQueue(path)) {
            tailer = queue.createTailer().toEnd();
            KeyValueMessage receiveNode = poll(tailer, 50, TimeUnit.MILLISECONDS);
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
        KeyValueMessage srcNode = KeyValueMessage.of("node1");
        KeyValueMessage srcNode2 = KeyValueMessage.of("node2");
        ExcerptAppender app;
        ExcerptTailer tailer;
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
        // From startTime
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
        KeyValueMessage srcNode = KeyValueMessage.of("node1");
        KeyValueMessage srcNode2 = KeyValueMessage.of("node2");
        ExcerptAppender app;
        ExcerptTailer tailer;
        try (ChronicleQueue queue = createQueue()) {
            assertFalse(queue.isClosed());
            app = queue.acquireAppender();
            tailer = queue.createTailer().toEnd();

            put(app, srcNode);
            put(app, srcNode2);

            KeyValueMessage receiveNode = poll(tailer);
            assertEquals(srcNode, receiveNode);
        }
        assertTrue(tailer.queue().isClosed());
        assertTrue(app.queue().isClosed());
        // the state does not help
        assertEquals(TailerState.FOUND_CYCLE, tailer.state());
        try {
            poll(tailer);
            fail("Expecting a exception on closed tailer");
        } catch (IllegalStateException exception) {
            // here the queue has been closed the tailer is closed
        }

    }

    @Override
    public void onAcquired(int cycle, File file) {
        log.debug("New file: " + file + " cycle: " + cycle);
    }

    @Override
    public void onReleased(int cycle, File file) {
        log.debug("Release file: " + file + " cycle" + cycle);
        /*
         * // this will not work, a queue file will be released when a queue is closed // this does not mean that
         * consumers have read all message // so it is better to delete externally the queue try { forceDelete(file); }
         * catch (IOException e) { throw new RuntimeException(e); }
         */

    }
}
