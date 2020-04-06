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
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.tests.KeyValueMessage;

/**
 * @since 9.3
 */
@SuppressWarnings("squid:S2925")
public class TestLogChronicle extends TestLog {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected Path basePath;

    @Before
    public void skipWindowsThatDoNotCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    @After
    public void resetBasePath() {
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
        Name fooLog = Name.ofUrn("test/foo");
        ChronicleLogManager manager = (ChronicleLogManager) createManager();
        assertTrue(manager.createIfNotExists(fooLog, NB_QUEUES));
        String basePath = manager.getBasePath();
        assertTrue(manager.delete(fooLog));

        // recreate
        assertTrue(manager.createIfNotExists(fooLog, NB_QUEUES));
        // add a file in the basePath
        Files.createFile(Paths.get(basePath, fooLog.getId(), "foo.txt"));
        try {
            manager.delete(fooLog);
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

        Path queuePath = Paths.get(manager.getBasePath(), logName.getId(), "P-00");
        // there is an extra metadata.cq4t file
        assertEquals(1, Files.list(queuePath).count());
        appender.append(0, msg);
        // after the first append we have the cycle file
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

        // in practice we always have one more cycle file than the expected retention
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
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                appender.append(0, KeyValueMessage.of("msg" + i));
            }
        };
        for (int i = 0; i < NB_APPENDERS; i++) {
            executor.submit(writer);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        // here the retention has kept only 3 cycles each cycle has 1 message per appender
        assertEquals(LogLag.of(NB_APPENDERS * RETENTION_CYCLES), manager.getLag(logName, Name.ofUrn("test/counter")));
    }

    @Test
    public void testRollCycle() throws Exception {
        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        KeyValueMessage msg3 = KeyValueMessage.of("id3");
        manager.createIfNotExists(logName, 1);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/group"), logName)) {
            appender.append(0, msg1);
            assertEquals(msg1, tailer.read(Duration.ofSeconds(1)).message());
            tailer.commit();
            appender.append(0, msg2);
            // change cycle
            Thread.sleep(1010);
            appender.append(0, msg3);
            assertEquals(msg2, tailer.read(Duration.ofSeconds(1)).message());
        }

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/group"), logName)) {
            // check that we start on msg2 which is on the previous cycle
            assertEquals(msg2, tailer.read(Duration.ofSeconds(1)).message());
            assertEquals(msg3, tailer.read(Duration.ofSeconds(1)).message());
        }
    }


    @Test
    public void testRecoverAfterExpirationOfRetention() throws Exception {
        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        KeyValueMessage msg3 = KeyValueMessage.of("id3");
        manager.createIfNotExists(logName, 1);
        LogAppender<KeyValueMessage> appender = manager.getAppender(logName);

        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/group"), logName)) {
            appender.append(0, msg1);
            assertEquals(msg1, tailer.read(Duration.ofSeconds(1)).message());
            tailer.commit();
        }
        appender.append(0, msg2);
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1100);
            appender.append(0, msg3);
        }
        resetManager();
        // Now the last committed message has been deleted by retention (3s)
        try (LogTailer<KeyValueMessage> tailer = manager.createTailer(Name.ofUrn("test/group"), logName)) {
            // msg2 has been lost we should be on msg3
            assertEquals(msg3, tailer.read(Duration.ofSeconds(1)).message());
        }
    }
}
