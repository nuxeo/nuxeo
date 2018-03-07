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
package org.nuxeo.lib.stream.tests.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.tools.Main;

/**
 * @since 9.3
 */
public abstract class TestTools {
    protected static final int NB_RECORD = 50;

    protected static final String LOG_NAME = "myLog";

    protected boolean initialized;

    public abstract String getManagerOptions();

    public abstract void createContent() throws Exception;

    @Before
    public void initContent() throws Exception {
        if (!initialized) {
            createContent();
            initialized = true;
        }
    }

    @Test
    public void testCat() {
        run(String.format("cat %s --log-name %s --lines 4", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testCatWithGroup() {
        run(String.format("cat %s -l %s -n 1 --group aGroup", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testCatMd() {
        run(String.format("cat %s -l %s -n 4 --render markdown", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testTail() {
        run(String.format("tail %s -l %s -n 5 --render text", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testTailAndFollow() {
        run(String.format("tail %s -f -l %s -n 2 --render text --timeout 1", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testLag() {
        run(String.format("lag %s", getManagerOptions()));
    }

    @Test
    public void testLagForLog() {
        run(String.format("lag %s --verbose --log-name %s", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testLatency() {
        run(String.format("latency %s --verbose", getManagerOptions()));
    }

    @Test
    public void testPositionToEnd() {
        run(String.format("position %s --to-end --log-name %s --group anotherGroup", getManagerOptions(), LOG_NAME));
        LogLag lag = getManager().getLag(LOG_NAME, "anotherGroup");
        assertEquals(lag.toString(), 0, lag.lag());
    }

    @Test
    public void testPositionReset() {
        run(String.format("position %s --reset --log-name %s --group anotherGroup", getManagerOptions(), LOG_NAME));
        LogLag lag = getManager().getLag(LOG_NAME, "anotherGroup");
        assertTrue(lag.toString(), lag.lag() > 0);
    }

    @Test
    public void testPositionAfterDate() {
        run("help position");
        runShouldFail(String.format("position %s --after-date %s --log-name %s --group anotherGroup", Instant.now(),
                getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testPositionToWatermark() throws InterruptedException {
        // move before all records, lag is maximum
        run(String.format("position %s --to-watermark %s --log-name %s --group anotherGroup", getManagerOptions(),
                Instant.now().minus(30, ChronoUnit.DAYS), LOG_NAME));
        LogManager manager = getManager();
        LogLag lag = manager.getLag(LOG_NAME, "anotherGroup");
        assertTrue(lag.toString(), lag.lag() > 1);

        // move to the position to the last record, lag = 1
        run(String.format("position %s --to-watermark %s --log-name %s --group anotherGroup", getManagerOptions(),
                Instant.now().plus(1, ChronoUnit.DAYS), LOG_NAME));
        lag = manager.getLag(LOG_NAME, "anotherGroup");
        assertEquals(lag.toString(), 1, lag.lag());

        // get the watermark of the second record
        long timestamp;
        String key;
        LogPartition logPartition;
        try (LogTailer<Record> tailer = manager.createTailer("tools", LOG_NAME)) {
            tailer.toStart();
            tailer.read(Duration.ofSeconds(1));
            LogRecord<Record> rec = tailer.read(Duration.ofSeconds(1));
            assertNotNull(rec);
            timestamp = Watermark.ofValue(rec.message().watermark).getTimestamp();
            key = rec.message().key;
            logPartition = rec.offset().partition();
        }
        // move to the watermark corresponding to the second record
        run(String.format("position %s --to-watermark %s --log-name %s --group anotherGroup", getManagerOptions(),
                Instant.ofEpochMilli(timestamp), LOG_NAME));
        // open a tailer with the moved group we should be on the same record
        try (LogTailer<Record> tailer = manager.createTailer("anotherGroup", logPartition)) {
            LogRecord<Record> rec = tailer.read(Duration.ofSeconds(1));
            assertNotNull(rec);
            assertEquals(rec.toString(), key, rec.message().key);
        }
    }

    @Test
    public void testPositionTimeTravel() {
        runShouldFail(String.format("position %s --log-name %s --group anotherGroup --after-date %s",
                getManagerOptions(), LOG_NAME, 123));
    }

    @Test
    public void testCopy() {
        run(String.format("copy %s --src %s --dest %s", getManagerOptions(), LOG_NAME,
                LOG_NAME + "-" + System.currentTimeMillis()));
    }

    @Test
    public void testTracker() {
        // run(String.format("tracker %s --verbose -l ALL -o %s-out -i 2 -c 3", getManagerOptions(), LOG_NAME));
        run(String.format("tracker %s --verbose -l %s -o %s-latencies -i 2 -c 3", getManagerOptions(), LOG_NAME,
                LOG_NAME));
    }

    @Test
    public void testTrackerAndRestore() throws InterruptedException {
        // Set a consumer position
        String group = "aGroup2Track";
        Record firstRecord;
        Record nextRecord;
        try (LogTailer<Record> tailer = getManager().createTailer(group, LOG_NAME)) {
            tailer.toStart();
            firstRecord = tailer.read(Duration.ofSeconds(1)).message();
            tailer.read(Duration.ofSeconds(1));
            tailer.commit(); // commit on record 2
            LogRecord<Record> nextLogRecord = tailer.read(Duration.ofSeconds(1));
            nextRecord = nextLogRecord.message();
            System.out.println("# nextRecord offset: " + nextLogRecord.offset() + " key: " + nextRecord.key);
        }
        // track the current latencies
        run(String.format("tracker %s --verbose -l %s -o %s-latencies -i 1 -c 1", getManagerOptions(), LOG_NAME,
                LOG_NAME));

        // reset the position
        run(String.format("position %s --reset --log-name %s --group %s", getManagerOptions(), LOG_NAME, group));
        // ensure that we have reset the position
        try (LogTailer<Record> tailer = getManager().createTailer(group, LOG_NAME)) {
            LogRecord<Record> rec = tailer.read(Duration.ofSeconds(1));
            assertEquals(firstRecord, rec.message());
        }

        // restore position
        run(String.format("restore %s --verbose --log-name %s -i %s-latencies", getManagerOptions(), LOG_NAME,
                LOG_NAME));

        // open a tailer we should be good
        try (LogTailer<Record> tailer = getManager().createTailer(group, LOG_NAME)) {
            LogRecord<Record> rec = tailer.read(Duration.ofSeconds(1));
            assertEquals(nextRecord, rec.message());
        }
    }

    @Test
    public void testHelpOption() {
        run("-h");
    }

    @Test
    public void testHelpCommand() {
        run("help");
    }

    @Test
    public void testHelpOnCommand() {
        run("help tail");
    }

    @Test
    public void testUnknownCommand() {
        runShouldFail("unknown-command");
    }

    @Test
    public void testUnknownOptions() {
        runShouldFail(
                String.format("cat %s --invalid-option %s -n 4 --render markdown", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testEmpty() {
        run("");
    }

    protected void run(String commandLine) {
        boolean result = runCommand(commandLine);
        assertTrue(String.format("Unexpected failure in command: \"%s\"", commandLine), result);
    }

    protected void runShouldFail(String commandLine) {
        boolean result = runCommand(commandLine);
        assertFalse(String.format("Expecting failure on command: \"%s\"", commandLine), result);
    }

    protected boolean runCommand(String commandLine) {
        System.out.println("# stream.sh " + commandLine);
        String[] args = commandLine.split(" ");
        return new Main().run(args);
    }

    protected LogManager getManager() {
        String commandLine = "test " + getManagerOptions();
        String[] args = commandLine.split(" ");
        LogManager ret = new Main().getLogManager(args);
        assertNotNull(ret);
        return ret;
    }

}
