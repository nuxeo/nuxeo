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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.lib.stream.tests.log.TestLog.DEF_TIMEOUT;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.lib.stream.codec.AvroBinaryCodec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.tests.KeyValueMessage;
import org.nuxeo.lib.stream.tools.Main;

/**
 * @since 9.3
 */
@SuppressWarnings("squid:S2925")
public abstract class TestTools {
    protected static final int NB_RECORD = 10;

    protected static final String LOG_NAME = "myLog";

    protected static final String LOG_NAME_2 = "myLog2";

    protected static final int LOG_SIZE = 1;

    protected boolean initialized;

    protected Record targetRecord;

    public abstract String getManagerOptions();

    @Before
    public void initContent() throws Exception {
        if (initialized) {
            return;
        }
        try (LogManager manager = getManager()) {
            Name logName = Name.ofUrn(LOG_NAME);
            Name logName2 = Name.ofUrn(LOG_NAME_2);
            manager.createIfNotExists(logName, LOG_SIZE);
            LogAppender<Record> appender = manager.getAppender(logName);
            for (int i = 0; i < NB_RECORD; i++) {
                String key = "key" + i;
                String value = "Some value for " + i;
                Record record = Record.of(key, value.getBytes(UTF_8));
                appender.append(i % LOG_SIZE, record);
                if (i == NB_RECORD / 2) {
                    targetRecord = record;
                }
                // needed because some tests expect different watermark to work
                Thread.sleep(2);
            }
            // move some positions
            try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test/aGroup"), logName)) {
                tailer.toStart();
                tailer.read(DEF_TIMEOUT);
                tailer.read(DEF_TIMEOUT);
                tailer.commit();
            }
            try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test/anotherGroup"), logName)) {
                tailer.read(DEF_TIMEOUT);
                tailer.commit();
            }
            // Create another log with non computation record and different encoding
            manager.createIfNotExists(logName2, LOG_SIZE);
            LogAppender<KeyValueMessage> appender2 = manager.getAppender(logName2,
                    new AvroBinaryCodec<>(KeyValueMessage.class));
            appender2.append(0, KeyValueMessage.of("foo", "bar".getBytes(UTF_8)));
            try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test/someGroup"), logName2)) {
                tailer.read(DEF_TIMEOUT);
                tailer.commit();
            }
        }
        initialized = true;
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
    public void testLagForLog() {
        run(String.format("lag %s --verbose --log-name %s", getManagerOptions(), LOG_NAME));
    }

    @Ignore("Takes too long on Kafka with lots of topics")
    @Test
    public void testLag() {
        run(String.format("lag %s --verbose", getManagerOptions()));
    }

    @Test
    public void testLatencyForLog() {
        run(String.format("latency %s --verbose --log-name %s", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testPositionToEnd() {
        run(String.format("position %s --to-end --log-name %s --group test/anotherGroup", getManagerOptions(), LOG_NAME));
        LogLag lag = getManager().getLag(Name.ofUrn(LOG_NAME), Name.ofUrn("test/anotherGroup"));
        assertEquals(lag.toString(), 0, lag.lag());
    }

    @Test
    public void testPositionReset() {
        run(String.format("position %s --reset --log-name %s --group test/anotherGroup", getManagerOptions(), LOG_NAME));
        LogLag lag = getManager().getLag(Name.ofUrn(LOG_NAME), Name.ofUrn("test/anotherGroup"));
        assertTrue(lag.toString(), lag.lag() > 0);
    }

    @Test
    public void testPositionOnPartition() {
        run(String.format("position %s --to-end --log-name %s --group anotherGroup", getManagerOptions(), LOG_NAME));
        List<LogLag> lags = getManager().getLagPerPartition(Name.ofUrn(LOG_NAME), Name.ofUrn("anotherGroup"));
        assertEquals(0, lags.get(0).lag());
        // then reset a the first partition
        run(String.format("position %s --reset --log-name %s --partition 0 --group anotherGroup", getManagerOptions(),
                LOG_NAME));
        lags = getManager().getLagPerPartition(Name.ofUrn(LOG_NAME), Name.ofUrn("anotherGroup"));
        assertTrue(lags.get(0).lag() > 0);
    }

    @Test
    public void testPositionToWatermark() throws InterruptedException {
        // move before all records, lag is maximum
        run(String.format("position %s --to-watermark %s --log-name %s --group anotherGroup", getManagerOptions(),
                Instant.now().minus(30, ChronoUnit.DAYS), LOG_NAME));
        LogManager manager = getManager();
        LogLag lag = manager.getLag(Name.ofUrn(LOG_NAME), Name.ofUrn("anotherGroup"));
        assertTrue(lag.toString(), lag.lag() > 1);

        // move to the position to the last record,
        // consumer will process the last message of each partition so lag is equal to the number o partitions
        run(String.format("position %s --to-watermark %s --log-name %s --group anotherGroup", getManagerOptions(),
                Instant.now().plus(1, ChronoUnit.DAYS), LOG_NAME));
        lag = manager.getLag(Name.ofUrn(LOG_NAME), Name.ofUrn("anotherGroup"));
        assertEquals(lag.toString(), LOG_SIZE, lag.lag());

        // move to the watermark of targetRecord, this work as expected because each record as a unique timestamp
        run(String.format("position %s --to-watermark %s --log-name %s --group anotherGroup", getManagerOptions(),
                Instant.ofEpochMilli(Watermark.ofValue(targetRecord.getWatermark()).getTimestamp()), LOG_NAME));
        // open a tailer with the moved group we should be on the same record
        try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("anotherGroup"), Name.ofUrn(LOG_NAME))) {
            LogRecord<Record> rec = tailer.read(DEF_TIMEOUT);
            assertNotNull(rec);
            assertEquals(targetRecord, rec.message());
        }
    }

    @Test
    public void testCopy() {
        run(String.format("copy %s --src %s --dest %s", getManagerOptions(), LOG_NAME,
                LOG_NAME + "_" + System.currentTimeMillis()));
    }

    @Test
    public void testTracker() {
        // run(String.format("tracker %s --verbose -l ALL -o %s-out -i 2 -c 3", getManagerOptions(), LOG_NAME));
        run(String.format("tracker %s --verbose -l %s -o %s_latencies -i 2 -c 3", getManagerOptions(), LOG_NAME,
                LOG_NAME));
    }

    @Test
    public void testMonitor() {
        // Use UDP, so it works without having a carbon server
        run(String.format("monitor %s --verbose -l %s,%s -h localhost -p 9999 --udp -i 2 -c 2", getManagerOptions(),
                LOG_NAME, LOG_NAME_2));
    }

    @Test
    public void testDatadog() {
        run(String.format("datadog %s --verbose -l %s,%s --api-key 1234 -i 2 -c 2", getManagerOptions(),
                LOG_NAME, LOG_NAME_2));
    }

    @Test
    public void testTrackerAndRestore() throws InterruptedException {
        // Set a consumer position
        Name group = Name.ofUrn("aGroup2Track");
        Record nextRecord;
        try (LogTailer<Record> tailer = getTailer(group)) {
            read(tailer);
            // to start is lazy an must be applied on assigned tailer (after a rebalance)
            tailer.toStart();
            read(tailer);
            read(tailer);
            tailer.commit(); // commit on record 2
            LogRecord<Record> nextLogRecord = tailer.read(Duration.ofSeconds(1));
            nextRecord = nextLogRecord.message();
            // System.out.println("# nextRecord offset: " + nextLogRecord.offset() + " key: " + nextRecord.getKey());
        }
        // track the current latencies
        run(String.format("tracker %s --verbose -l %s -o %s_latencies -i 1 -c 1", getManagerOptions(), LOG_NAME,
                LOG_NAME));

        // reset the position
        run(String.format("position %s --reset --log-name %s --group %s", getManagerOptions(), LOG_NAME, group.getUrn()));

        // restore position
        run(String.format("restore %s --verbose --log-name %s -i %s_latencies", getManagerOptions(), LOG_NAME,
                LOG_NAME));

        // open a tailer we should be good
        try (LogTailer<Record> tailer = getTailer(group)) {
            Record rec = read(tailer);
            assertEquals(nextRecord, rec);
        }
    }

    protected LogTailer<Record> getTailer(Name group) {
        LogManager manager = getManager();
        if (manager.supportSubscribe()) {
            return manager.subscribe(group, Collections.singleton(Name.ofUrn(LOG_NAME)), null);
        }
        return manager.createTailer(group, Name.ofUrn(LOG_NAME));
    }

    protected Record read(LogTailer<Record> tailer) throws InterruptedException {
        try {
            return tailer.read(DEF_TIMEOUT).message();
        } catch (RebalanceException e) {
            return tailer.read(DEF_TIMEOUT).message();
        }
    }

    @Test
    public void testDump() {
        run("help dump");

        run(String.format("dump %s --log-name %s --count 4 --partition 0 --output /tmp/foo.avro", getManagerOptions(),
                LOG_NAME));
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
        // System.out.println("# stream.sh " + commandLine);
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
