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
package org.nuxeo.lib.stream.tests.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.lib.stream.tests.TestLibChronicle.IS_WIN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.tests.KeyValueMessage;

/**
 * @since 9.3
 */
public class TestLogChronicle extends TestLog {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected Path basePath;

    @Before
    public void skipWindowsThatDontCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    @After
    public void resetBasePath() throws IOException {
        basePath = null;
    }

    @Override
    public LogManager createManager() throws Exception {
        if (basePath == null) {
            basePath = folder.newFolder().toPath();
        }
        return new ChronicleLogManager(basePath, "3s");
    }

    @Test
    public void deleteInvalidPath() throws Exception {
        final int NB_QUEUES = 5;

        ChronicleLogManager manager = (ChronicleLogManager) createManager();
        assertTrue(manager.createIfNotExists("foo", NB_QUEUES));
        String basePath = manager.getBasePath();
        assertTrue(manager.delete("foo"));

        // recreate
        assertTrue(manager.createIfNotExists("foo", NB_QUEUES));
        // add a file in the basePath
        Files.createFile(Paths.get(basePath, "foo/foo.txt"));
        try {
            manager.delete("foo");
            fail("Cannot delete a Log with external data");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testFileRetention() throws Exception {
        KeyValueMessage msg = KeyValueMessage.of("id");
        ChronicleLogManager manager = (ChronicleLogManager) createManager();
        manager.createIfNotExists(logName, 1);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        Path queuePath = Paths.get(manager.getBasePath(), logName, "P-00");
        // there is an extra directory-listing.cq4t file in addition of the cycle
        assertEquals(1, Files.list(queuePath).count());
        appender.append(0, msg);
        assertEquals(2, Files.list(queuePath).count());
        Thread.sleep(1010);
        // cycle 2
        appender.append(0, msg);
        assertEquals(3, Files.list(queuePath).count());
        Thread.sleep(2010);
        // cycle 3
        appender.append(0, msg);
        assertEquals(4, Files.list(queuePath).count());
        Thread.sleep(1010);

        // cycle 4:
        // purge is done on cycle release
        // cycle 3 is released and there is no purge because we see only 3 cycles
        // cycle 4 is acquired we now have 4 cycles
        appender.append(0, msg);
        assertEquals(5, Files.list(queuePath).count());
        Thread.sleep(1010);

        // cycle 5: cycle 4 is released and purge cycle 1 we have 3 cycles
        // then cycle 5 is acquired we have 4 cycles files
        appender.append(0, msg);
        assertEquals(5, Files.list(queuePath).count());

        // calling lag will trigger a release so we purge cycle 2 and reach the 3s retention with 3 cycles
        // sleep is needed because we don't purge more than one time per cycle duration
        Thread.sleep(1010);
        manager.getLag(logName, "foo");
        assertEquals(4, Files.list(queuePath).count());
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Test
    public void testConcurrentFileRetentions() throws Exception {
        final int NB_APPENDERS = 5;
        final int RETENTION_CYCLES = 3; // retention is 3s
        final int NB_MSG = 5;
        final int LOG_SIZE = 1;
        manager.createIfNotExists(logName, LOG_SIZE);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);
        ExecutorService executor = Executors.newFixedThreadPool(NB_APPENDERS);
        Runnable writer = () -> {
            for (int i = 0; i < NB_MSG; i++) {
                appender.append(0, KeyValueMessage.of("msg" + i));
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        };
        for (int i = 0; i < NB_APPENDERS; i++) {
            executor.submit(writer);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        // here the retention has kept only 3 cyles each cycle has 1 message per appender
        assertEquals(LogLag.of(NB_APPENDERS * RETENTION_CYCLES), manager.getLag(logName, "counter"));
    }

    @Test
    public void testRollCycle() throws Exception {
        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        KeyValueMessage msg3 = KeyValueMessage.of("id3");
        manager.createIfNotExists(logName, 1);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer("group", logName)) {
            appender.append(0, msg1);
            assertEquals(msg1, tailer.read(Duration.ofSeconds(1)).message());
            tailer.commit();
            appender.append(0, msg2);
            // change cycle
            Thread.sleep(1010);
            appender.append(0, msg3);
            assertEquals(msg2, tailer.read(Duration.ofSeconds(1)).message());
        }

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer("group", logName)) {
            // check that we start on msg2 which is on the previous cycle
            assertEquals(msg2, tailer.read(Duration.ofSeconds(1)).message());
            assertEquals(msg3, tailer.read(Duration.ofSeconds(1)).message());
        }
    }

}
