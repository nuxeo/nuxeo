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
package org.nuxeo.lib.stream.tests.computation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * @since 9.3
 */
public abstract class TestStreamProcessor {
    private static final Log log = LogFactory.getLog(TestStreamProcessor.class);

    public abstract LogManager getLogManager() throws Exception;

    public abstract LogManager getSameLogManager() throws Exception;

    public abstract StreamProcessor getStreamProcessor(LogManager logManager);

    public void testSimpleTopo(int nbRecords, int concurrency) throws Exception {
        final long targetTimestamp = System.currentTimeMillis();
        final long targetWatermark = Watermark.ofTimestamp(targetTimestamp).getValue();
        Topology topology = Topology.builder()
                                    .addComputation(
                                            () -> new ComputationSource("GENERATOR", 1, nbRecords, 5, targetTimestamp),
                                            Collections.singletonList("o1:s1"))
                                    .addComputation(() -> new ComputationForward("C1", 1, 1),
                                            Arrays.asList("i1:s1", "o1:s2"))
                                    .addComputation(() -> new ComputationForward("C2", 1, 1),
                                            Arrays.asList("i1:s2", "o1:s3"))
                                    .addComputation(() -> new ComputationForward("C3", 1, 1),
                                            Arrays.asList("i1:s3", "o1:s4"))
                                    .addComputation(
                                            () -> new ComputationRecordCounter("COUNTER", Duration.ofMillis(100)),
                                            Arrays.asList("i1:s4", "o1:output"))
                                    .build();
        // one thread for each computation
        Settings settings = new Settings(concurrency, concurrency).setConcurrency("GENERATOR", 1);
        // uncomment to get the plantuml diagram
        // System.out.println(topology.toPlantuml(settings));
        try (LogManager manager = getLogManager()) {
            StreamProcessor processor = getStreamProcessor(manager);
            processor.init(topology, settings).start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            long start = System.currentTimeMillis();
            // this check works only if there is only one record with the target timestamp
            // so using concurrency > 1 will fail
            while (!processor.isDone(targetTimestamp)) {
                Thread.sleep(30);
                long lowWatermark = processor.getLowWatermark();
                log.info("low: " + lowWatermark + " dist: " + (targetWatermark - lowWatermark));
            }
            double elapsed = (double) (System.currentTimeMillis() - start) / 1000.0;
            // shutdown brutally so there is no more processing in background
            processor.shutdown();

            // read the results
            int result = readCounterFrom(manager, "output");
            int expected = nbRecords * settings.getConcurrency("GENERATOR");
            if (result != expected) {
                processor = getStreamProcessor(manager);
                processor.init(topology, settings).start();
                int waiter = 200;
                log.warn("FAILURE DEBUG TRACE ========================");
                do {
                    waiter -= 10;
                    Thread.sleep(10);
                    long lowWatermark = processor.getLowWatermark();
                    log.warn("low: " + lowWatermark + " dist: " + (targetWatermark - lowWatermark));
                } while (waiter > 0);
                processor.shutdown();
            }
            log.info(
                    String.format("topo: simple, concurrency: %d, records: %s, took: %.2fs, throughput: %.2f records/s",
                            concurrency, result, elapsed, result / elapsed));
            assertEquals(expected, result);
        }
    }

    @Test
    public void testSimpleTopoOneRecordOneThread() throws Exception {
        testSimpleTopo(1, 1);
    }

    @Test
    public void testSimpleTopoFewRecordsOneThread() throws Exception {
        testSimpleTopo(17, 1);
    }

    @Test
    public void testSimpleTopoManyRecordsOneThread() throws Exception {
        testSimpleTopo(1003, 1);
    }

    // @Test
    public void testSimpleTopoManyRecordsManyThread() throws Exception {
        // because of the concurrency record arrive in disorder in the final counter
        // if the last record is processed by the final counter
        // the global watermark is reached but this does not means that there
        // no pending records on the counter stream, so this test can fail
        // for this kind of test we should test like with testComplexTopo
        // using drainAndStop
        testSimpleTopo(205, 10);
    }

    public void testComplexTopo(int nbRecords, int concurrency, int partitions) throws Exception {
        final long targetTimestamp = System.currentTimeMillis();
        final long targetWatermark = Watermark.ofTimestamp(targetTimestamp).getValue();
        Topology topology = Topology.builder()
                                    .addComputation(
                                            () -> new ComputationSource("GENERATOR", 1, nbRecords, 5, targetTimestamp),
                                            Collections.singletonList("o1:s1"))
                                    .addComputation(() -> new ComputationForward("C1", 1, 2),
                                            Arrays.asList("i1:s1", "o1:s2", "o2:s3"))
                                    .addComputation(() -> new ComputationForward("C2", 2, 1),
                                            Arrays.asList("i1:s1", "i2:s4", "o1:s5"))
                                    .addComputation(() -> new ComputationForward("C3", 1, 2),
                                            Arrays.asList("i1:s2", "o1:s5", "o2:s4"))
                                    // .addComputation(() -> new ComputationForwardSlow("C3", 1, 2, 5),
                                    // Arrays.asList("i1:s2", "o1:s5", "o2:s4"))
                                    .addComputation(() -> new ComputationForward("C4", 1, 1),
                                            Arrays.asList("i1:s3", "o1:s5"))
                                    .addComputation(
                                            () -> new ComputationRecordCounter("COUNTER", Duration.ofMillis(100)),
                                            Arrays.asList("i1:s5", "o1:output"))
                                    .build();

        Settings settings = new Settings(concurrency, partitions).setPartitions("output", 1);
        settings.setConcurrency("C4", 16).setPartitions("s3", 16).setConcurrency("COUNTER", 4).setPartitions("s5", 4);
        // uncomment to get the plantuml diagram
        // System.out.println(topology.toPlantuml(settings));
        try (LogManager manager = getLogManager()) {
            StreamProcessor processor = getStreamProcessor(manager);
            long start = System.currentTimeMillis();
            processor.init(topology, settings).start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            // no record are processed so far
            long lowWatermark = processor.getLowWatermark();

            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            double elapsed = (double) (System.currentTimeMillis() - start) / 1000.0;
            // read the results
            int result = readCounterFrom(manager, "output");
            log.info(String.format(
                    "topo: complex, concurrency: %d, records: %s, took: %.2fs, throughput: %.2f records/s", concurrency,
                    result, elapsed, result / elapsed));
            assertEquals(2 * settings.getConcurrency("GENERATOR") * nbRecords, result);
        }
    }

    @Test
    public void testComplexTopoOneRecordOneThreadOnePartition() throws Exception {
        testComplexTopo(1, 1, 1);
    }

    @Test
    public void testComplexTopoOneRecordOneThreadMultiPartitions() throws Exception {
        testComplexTopo(1, 1, 8);
    }

    @Test
    public void testComplexTopoFewRecordsOneThreadOnePartition() throws Exception {
        testComplexTopo(17, 1, 1);
    }

    @Test
    public void testComplexTopoManyRecordsOneThread() throws Exception {
        testComplexTopo(1003, 1, 1);
    }

    @Test
    public void testComplexTopoOneRecord() throws Exception {
        testComplexTopo(1, 8, 8);
    }

    @Test
    public void testComplexTopoFewRecords() throws Exception {
        testComplexTopo(17, 8, 8);
    }

    @Test
    public void testComplexTopoManyRecords() throws Exception {
        testComplexTopo(1003, 12, 32);
    }

    @Test
    public void testComplexTopoManyRecordsOnePartition() throws Exception {
        testComplexTopo(101, 12, 1);
    }

    @Test
    public void testComplexTopoManyRecordsOneThreadManyPartitions() throws Exception {
        testComplexTopo(100, 12, 13);
    }

    @Test
    public void testStopAndResume() throws Exception {
        final long targetTimestamp = System.currentTimeMillis();
        final long targetWatermark = Watermark.ofTimestamp(targetTimestamp).getValue();
        final int nbRecords = 1001;
        final int concurrent = 8;
        Topology topology1 = Topology.builder()
                                     .addComputation(
                                             () -> new ComputationSource("GENERATOR", 1, nbRecords, 5, targetTimestamp),
                                             Collections.singletonList("o1:s1"))
                                     .build();

        Settings settings1 = new Settings(concurrent, concurrent);

        Topology topology2 = Topology.builder()
                                     .addComputation(() -> new ComputationForward("C1", 1, 2),
                                             Arrays.asList("i1:s1", "o1:s2", "o2:s3"))
                                     .addComputation(() -> new ComputationForward("C2", 2, 1),
                                             Arrays.asList("i1:s1", "i2:s4", "o1:s5"))
                                     .addComputation(() -> new ComputationForward("C3", 1, 2),
                                             Arrays.asList("i1:s2", "o1:s5", "o2:s4"))
                                     .addComputation(() -> new ComputationForward("C4", 1, 1),
                                             Arrays.asList("i1:s3", "o1:s5"))
                                     .addComputation(
                                             () -> new ComputationRecordCounter("COUNTER", Duration.ofMillis(100)),
                                             Arrays.asList("i1:s5", "o1:output"))
                                     .build();

        Settings settings2 = new Settings(concurrent, concurrent).setPartitions("output", 1).setConcurrency("COUNTER",
                concurrent);
        // uncomment to get the plantuml diagram
        // System.out.println(topology.toPlantuml(settings));

        // 1. run generators
        try (LogManager manager = getLogManager()) {
            StreamProcessor processor = getStreamProcessor(manager);
            long start = System.currentTimeMillis();
            processor.init(topology1, settings1).start();
            // This is needed because drainAndStop might consider the source generator as terminated
            // because of a random lag due to kafka init and/or GC > 500ms.
            Thread.sleep(2000);
            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            double elapsed = (double) (System.currentTimeMillis() - start) / 1000.0;
            long total = settings1.getConcurrency("GENERATOR") * nbRecords;
            log.info(String.format("generated: %s in %.2fs, throughput: %.2f records/s", total, elapsed,
                    total / elapsed));
        }
        int result = 0;
        // 2. resume and kill loop
        for (int i = 0; i < 10; i++) {
            try (LogManager manager = getSameLogManager()) {
                StreamProcessor processor = getStreamProcessor(manager);
                long start = System.currentTimeMillis();
                log.info("RESUME computations");
                processor.init(topology2, settings2).start();
                assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
                // must be greater than kafka heart beat ?
                Thread.sleep(400 + i * 10);
                log.info("KILL computations pool");
                processor.shutdown();
                long processed = readCounterFrom(manager, "output");
                result += processed;
                log.info("processed: " + processed + " total: " + result);
            }
        }
        // 3. run the rest
        log.info("Now draining without interruption");
        try (LogManager manager = getSameLogManager()) {
            StreamProcessor processor = getStreamProcessor(manager);
            long start = System.currentTimeMillis();
            processor.init(topology2, settings2).start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            assertTrue(processor.drainAndStop(Duration.ofSeconds(200)));
            double elapsed = (double) (System.currentTimeMillis() - start) / 1000.0;
            // read the results
            long processed = readCounterFrom(manager, "output");
            result += processed;
            // the number of results can be bigger than expected, in the case of checkpoint failure
            // some records can be reprocessed (duplicate), this is a delivery at least one, not exactly one.
            long expected = 2 * settings1.getConcurrency("GENERATOR") * nbRecords;
            log.warn(String.format("count: %s, expected: %s, in %.2fs, throughput: %.2f records/s", result, expected,
                    elapsed, result / elapsed));
            assertTrue(expected <= result);
        }

    }

    @Test
    public void testDrainSource() throws Exception {
        final long targetTimestamp = System.currentTimeMillis();
        final int nbRecords = 1;
        final int concurrent = 4;
        Topology topology1 = Topology.builder()
                                     .addComputation(
                                             () -> new ComputationSource("GENERATOR", 1, nbRecords, 1, targetTimestamp),
                                             Collections.singletonList("o1:s1"))
                                     .build();
        Settings settings1 = new Settings(concurrent, concurrent);

        try (LogManager manager = getLogManager()) {
            StreamProcessor processor = getStreamProcessor(manager);
            long start = System.currentTimeMillis();
            processor.init(topology1, settings1).start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            // no record are processed so far
            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            double elapsed = (double) (System.currentTimeMillis() - start) / 1000.0;
            int result = countRecordIn(manager, "s1");
            log.info(
                    String.format("count: %s in %.2fs, throughput: %.2f records/s", result, elapsed, result / elapsed));
            assertEquals(settings1.getConcurrency("GENERATOR") * nbRecords, result);
        }
    }

    protected int readCounterFrom(LogManager manager, String stream) throws InterruptedException {
        int partitions = manager.getAppender(stream).size();
        int ret = 0;
        for (int i = 0; i < partitions; i++) {
            ret += readCounterFromPartition(manager, stream, i);
        }
        return ret;
    }

    protected int readCounterFromPartition(LogManager manager, String stream, int partition)
            throws InterruptedException {
        LogTailer<Record> tailer = manager.createTailer("results", LogPartition.of(stream, partition));
        int result = 0;
        tailer.toStart();
        for (LogRecord<Record> logRecord = tailer.read(
                Duration.ofMillis(1000)); logRecord != null; logRecord = tailer.read(Duration.ofMillis(500))) {
            result += Integer.valueOf(logRecord.message().key);
        }
        tailer.commit();
        return result;
    }

    protected int countRecordIn(LogManager manager, String stream) throws Exception {
        int ret = 0;
        for (int i = 0; i < manager.getAppender(stream).size(); i++) {
            ret += countRecordInPartition(manager, stream, i);
        }
        return ret;
    }

    protected int countRecordInPartition(LogManager manager, String stream, int partition) throws Exception {
        try (LogTailer<Record> tailer = manager.createTailer("results", LogPartition.of(stream, partition))) {
            int result = 0;
            tailer.toStart();
            for (LogRecord<Record> logRecord = tailer.read(
                    Duration.ofMillis(1000)); logRecord != null; logRecord = tailer.read(Duration.ofMillis(500))) {
                result += 1;
            }
            return result;
        }
    }

}
