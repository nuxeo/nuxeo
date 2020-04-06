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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.lib.stream.codec.AvroBinaryCodec;
import org.nuxeo.lib.stream.codec.AvroJsonCodec;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.3
 */
@SuppressWarnings("squid:S2925")
public abstract class TestStreamProcessor {
    private static final Log log = LogFactory.getLog(TestStreamProcessor.class);

    protected static final String OUTPUT_STREAM = "output";

    public Codec<Record> codec = new AvroMessageCodec<>(Record.class);

    public abstract LogManager getLogManager() throws Exception;

    public abstract LogManager getSameLogManager();

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
                                            Arrays.asList("i1:s4", "o1:" + OUTPUT_STREAM))
                                    .build();
        // one thread for each computation
        Settings settings = new Settings(concurrency, concurrency, codec).setConcurrency("GENERATOR", 1);
        // uncomment to get the plantuml diagram
        // System.out.println(topology.toPlantuml(settings));
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            long start = System.currentTimeMillis();
            // this check works only if there is only one record with the target timestamp
            // so using concurrency > 1 will fail
            while (!processor.isDone(targetTimestamp)) {
                Thread.sleep(30);
                long lowWatermark = processor.getLowWatermark();
                log.info("low: " + lowWatermark + " dist: " + (targetWatermark - lowWatermark));
            }
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            // shutdown brutally so there is no more processing in background
            processor.shutdown();

            Latency latency = processor.getLatency("COUNTER");
            assertEquals(latency.toString(), 0, latency.latency());
            // read the results
            int result = readOutputCounter(manager);
            int expected = nbRecords * settings.getConcurrency("GENERATOR");
            if (result != expected) {
                processor = streamManager.createStreamProcessor("processor");
                processor.start();
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

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleTopoOneRecordOneThreadLegacyCodec() throws Exception {
        codec = NO_CODEC;
        try {
            testSimpleTopo(1, 1);
        } finally {
            restoreDefaultCodec();
        }
    }

    @Test
    public void testSimpleTopoOneRecordOneThreadAvroJsonCodec() throws Exception {
        codec = new AvroJsonCodec<>(Record.class);
        try {
            testSimpleTopo(1, 1);
        } finally {
            restoreDefaultCodec();
        }
    }

    @Test
    public void testSimpleTopoOneRecordOneThreadSerializableCodec() throws Exception {
        codec = new SerializableCodec<>();
        try {
            testSimpleTopo(1, 1);
        } finally {
            restoreDefaultCodec();
        }
    }

    @Test
    public void testSimpleTopoOneRecordOneThreadAvroCodec() throws Exception {
        codec = new AvroBinaryCodec<>(Record.class);
        try {
            testSimpleTopo(1, 1);
        } finally {
            restoreDefaultCodec();
        }
    }

    protected void restoreDefaultCodec() {
        codec = new AvroMessageCodec<>(Record.class);
    }

    @Test
    public void testSimpleTopoFewRecordsOneThread() throws Exception {
        testSimpleTopo(17, 1);
    }

    @Test
    public void testSimpleTopoManyRecordsOneThread() throws Exception {
        testSimpleTopo(1003, 1);
    }

    @Ignore("A wrong case")
    @Test
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

        Settings settings = new Settings(concurrency, partitions, codec).setPartitions("output", 1);
        settings.setConcurrency("C4", 4).setPartitions("s3", 4).setConcurrency("COUNTER", 4).setPartitions("s5", 4);
        // uncomment to get the plantuml diagram
        // System.out.println(topology.toPlantuml(settings));
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            long start = System.currentTimeMillis();
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            // no record are processed so far

            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            // read the results
            int result = readOutputCounter(manager);
            log.info(String.format(
                    "topo: complex, concurrency: %d, records: %s, took: %.2fs, throughput: %.2f records/s", concurrency,
                    result, elapsed, result / elapsed));
            assertEquals(2 * settings.getConcurrency("GENERATOR") * nbRecords, result);
        }
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoOneRecordOneThreadOnePartition() throws Exception {
        testComplexTopo(1, 1, 1);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoOneRecordOneThreadMultiPartitions() throws Exception {
        testComplexTopo(1, 1, 8);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoFewRecordsOneThreadOnePartition() throws Exception {
        testComplexTopo(17, 1, 1);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoManyRecordsOneThread() throws Exception {
        testComplexTopo(1003, 1, 1);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoOneRecord() throws Exception {
        testComplexTopo(1, 4, 4);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoFewRecords() throws Exception {
        testComplexTopo(17, 4, 4);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoManyRecords() throws Exception {
        testComplexTopo(1003, 4, 8);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoManyRecordsOnePartition() throws Exception {
        testComplexTopo(101, 8, 1);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testComplexTopoManyRecordsOneThreadManyPartitions() throws Exception {
        testComplexTopo(100, 6, 8);
    }

    @Ignore(value = "NXP-27559")
    @Test
    public void testStopAndResume() throws Exception {
        final long targetTimestamp = System.currentTimeMillis();
        final int nbRecords = 1001;
        final int concurrent = 8;
        Topology topology1 = Topology.builder()
                                     .addComputation(
                                             () -> new ComputationSource("GENERATOR", 1, nbRecords, 5, targetTimestamp),
                                             Collections.singletonList("o1:s1"))
                                     .build();

        Settings settings1 = new Settings(concurrent, concurrent, codec);

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

        Settings settings2 = new Settings(concurrent, concurrent, codec).setPartitions("output", 1)
                                                                        .setConcurrency("COUNTER", concurrent);
        // uncomment to get the plantuml diagram
        // System.out.println(topology.toPlantuml(settings));

        // 1. run generators
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor1", topology1, settings1);
            long start = System.currentTimeMillis();
            processor.start();
            // This is needed because drainAndStop might consider the source generator as terminated
            // because of a random lag due to kafka init and/or GC > 500ms.
            Thread.sleep(2000);
            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            long total = settings1.getConcurrency("GENERATOR") * nbRecords;
            log.info(String.format("generated: %s in %.2fs, throughput: %.2f records/s", total, elapsed,
                    total / elapsed));
        }
        int result = 0;
        // 2. resume and kill loop
        for (int i = 0; i < 10; i++) {
            try (LogManager manager = getSameLogManager()) {
                StreamManager streamManager = new LogStreamManager(manager);
                StreamProcessor processor = streamManager.registerAndCreateProcessor("processor2", topology2, settings2);
                log.info("RESUME computations");
                processor.start();
                assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
                // must be greater than kafka heart beat ?
                Thread.sleep(400 + i * 10);
                log.info("KILL computations pool");
                processor.shutdown();
                long processed = readOutputCounter(manager);
                result += processed;
                log.info("processed: " + processed + " total: " + result);
            }
        }
        // 3. run the rest
        log.info("Now draining without interruption");
        try (LogManager manager = getSameLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor2", topology2, settings2);
            long start = System.currentTimeMillis();
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            assertTrue(processor.drainAndStop(Duration.ofSeconds(200)));
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            // read the results
            long processed = readOutputCounter(manager);
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
        Settings settings1 = new Settings(concurrent, concurrent, codec);

        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor1", topology1, settings1);
            long start = System.currentTimeMillis();
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            // no record are processed so far
            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            int result = countRecordIn(manager, "s1");
            log.info(
                    String.format("count: %s in %.2fs, throughput: %.2f records/s", result, elapsed, result / elapsed));
            assertEquals(settings1.getConcurrency("GENERATOR") * nbRecords, result);
        }
    }

    @Test
    public void testDrainTimeout() throws Exception {
        final long targetTimestamp = System.currentTimeMillis();
        final int nbRecords = 3;
        final int concurrent = 1;
        Topology topology1 = Topology.builder()
                .addComputation(
                        () -> new ComputationSource("GENERATOR", 1, nbRecords, 1, targetTimestamp),
                        Collections.singletonList("o1:input"))
                .addComputation(
                        () -> new ComputationForwardSlow("SLOW", 1, 1, 1500),
                        Arrays.asList("i1:input", "o1:output"))
                .build();
        Settings settings1 = new Settings(concurrent, concurrent, codec);

        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor1", topology1, settings1);
            long start = System.currentTimeMillis();
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            // no record are processed so far
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            int result = countRecordIn(manager, "output");
            log.info(
                    String.format("count: %s in %.2fs, throughput: %.2f records/s", result, elapsed, result / elapsed));
            assertEquals(settings1.getConcurrency("GENERATOR") * nbRecords, result);
        }
    }

    @Test
    public void testSingleSource() throws Exception {
        final int nbRecords = 10;
        final long targetTimestamp = System.currentTimeMillis();
        // This is a pattern to have a single producer running with fail over.
        // The computation has an unused input stream with a single partition,
        // only the computation instance affected to the partition is generating new records.
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationSource("GENERATOR", 1, nbRecords, 1,
                                            targetTimestamp, true), Collections.singletonList("o1:s1"))
                                    .build();
        Settings settings = new Settings(2, 1).setConcurrency("GENERATOR", 2).setPartitions("s1", 1);
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            // source computation will start on assignment, let them work a bit
            Thread.sleep(1000);
            assertTrue(processor.drainAndStop(Duration.ofSeconds(60)));
            LogLag lag = manager.getLag(Name.ofUrn("s1"), Name.ofUrn("test"));
            // without rebalancing we should have lag == nbRecords, but a rebalancing happens
            // so we can have up to concurrency * nbRecords
            assertTrue(lag.toString() + ", records: " + nbRecords, lag.lag() >= nbRecords);
        }
    }

    @Test
    public void testInvalidProcessorWithMultipleInputCodec() throws Exception {
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationForward("C1", 2, 1),
                                            Arrays.asList("i1:inputJson", "i2:inputAvro", "o1:outputAvro"))
                                    .build();
        Settings settings = new Settings(1, 1).setCodec("inputJson", new AvroJsonCodec<>(Record.class))
                                              .setCodec("inputJava", new SerializableCodec<>())
                                              .setCodec("outputAvro", new AvroMessageCodec<>(Record.class));
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            try {
                StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
                processor.start();
                fail("It should not be possible to read from input streams with different encoding");
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

    @Test
    public void testProcessorWithMultipleOutputCodec() throws Exception {
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationForward("C1", 1, 2),
                                            Arrays.asList("i1:input", "o1:outputAvro", "o2:outputJson"))
                                    .build();
        Settings settings = new Settings(1, 1, new AvroJsonCodec<>(Record.class)).setCodec("outputAvro",
                new AvroMessageCodec<>(Record.class));
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            streamManager.append("input", Record.of("key", "bar".getBytes(StandardCharsets.UTF_8)));
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            assertEquals(lag.toString(), 0, lag.lag());
        }
    }

    @Test
    public void testProcessorWithDifferentInputOutputCodec() throws Exception {
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationForward("C1", 1, 1),
                                            Arrays.asList("i1:inputJson", "o1:outputAvro"))
                                    .build();
        Settings settings = new Settings(1, 1).setCodec("inputJson", new AvroJsonCodec<>(Record.class))
                                              .setCodec("outputAvro", new AvroMessageCodec<>(Record.class));
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            // The processor init has already configured the codec for source stream
            LogAppender<Record> appender = manager.getAppender(Name.ofUrn("inputJson"));
            assertEquals("avroJson", appender.getCodec().getName());

            // write some input
            appender.append(0, Record.of("key", "value".getBytes(StandardCharsets.UTF_8)));
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            assertTrue(processor.drainAndStop(Duration.ofSeconds(100)));
            // read output using avro codec
            try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test"), Name.ofUrn("outputAvro"),
                    new AvroMessageCodec<>(Record.class))) {
                assertEquals("avro", tailer.getCodec().getName());
                assertNotNull(tailer.read(Duration.ofSeconds(1)));
            }
        }
    }

    @Test
    public void testComputationPolicy() throws Exception {
        // Define a topology
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationFailureForward("C1", 1, 1),
                                            Arrays.asList("i1:input", "o1:output"))
                                    .build();
        // Default policy (no retry, abort on failure)
        try (LogManager manager = getLogManager()) {
            // Run the processor
            StreamManager streamManager = new LogStreamManager(manager);
            Settings settings = new Settings(1, 1);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            processor.waitForAssignments(Duration.ofSeconds(10));
            // Add an input record
            streamManager.append("input", Record.of("foo", null));
            // wait
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // we expect a lag because the computation fails
            assertEquals(lag.toString(), 1, lag.lag());
        }
        // Policy no retry and continue on failure
        ComputationPolicy policy = new ComputationPolicyBuilder().retryPolicy(
                new RetryPolicy(ComputationPolicy.NO_RETRY)).continueOnFailure(true).build();
        try (LogManager manager = getSameLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            Settings settings = new Settings(1, 1, policy);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            processor.waitForAssignments(Duration.ofSeconds(10));
            // wait
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // this times the after failure we have checkpoint so no lag expected
            assertEquals(lag.toString(), 0, lag.lag());
        }
        // Policy with retries, abort on failure
        policy = new ComputationPolicyBuilder().retryPolicy(
                new RetryPolicy().withMaxRetries(ComputationFailureForward.FAILURE_COUNT)
                                 .retryOn(IllegalStateException.class)).continueOnFailure(false).build();
        try (LogManager manager = getSameLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            Settings settings = new Settings(1, 1, policy);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            processor.waitForAssignments(Duration.ofSeconds(10));
            // add a new input
            streamManager.append("input", Record.of("bar", null));
            // wait
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // no lag because last retry is expected to work
            assertEquals(lag.toString(), 0, lag.lag());
        }

    }


    @Test
    public void testPolicyBatchComputation() throws Exception {
        // Define a topology
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationBatchFailureForward("C1", 1),
                                            Arrays.asList("i1:input", "o1:output"))
                                    .build();
        int batchCapacity = 2;
        Duration batchThreshold = Duration.ofMillis(200);

        // Default no retry policy
        ComputationPolicy policy = new ComputationPolicyBuilder().batchPolicy(batchCapacity, batchThreshold).build();
        try (LogManager manager = getLogManager()) {
            // Run the processor
            StreamManager streamManager = new LogStreamManager(manager);
            Settings settings = new Settings(1, 1, policy);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            processor.waitForAssignments(Duration.ofSeconds(10));
            // Add an input records

            streamManager.append("input", Record.of("foo", null));
            streamManager.append("input", Record.of("bar", null));
            streamManager.append("input", Record.of("foo", null));
            // wait
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // wa expect a lag because the computation fails
            assertEquals(lag.toString(), 3, lag.lag());
        }

        // Policy with retries, abort on failure
        policy = new ComputationPolicyBuilder().batchPolicy(batchCapacity, batchThreshold)
                                               .retryPolicy(new RetryPolicy().withMaxRetries(
                                                       ComputationBatchFailureForward.FAILURE_COUNT)
                                                                             .retryOn(IllegalStateException.class))
                                               .continueOnFailure(false)
                                               .build();
        try (LogManager manager = getSameLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            Settings settings = new Settings(1, 1, policy);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            assertEquals(lag.toString(), 3, lag.lag());
            processor.waitForAssignments(Duration.ofSeconds(10));
            // wait
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // this times the after failure we have checkpoint so no lag expected
            assertEquals(lag.toString(), 0, lag.lag());
        }

    }

    @Test
    public void testRegisterWithoutExecution() throws Exception {
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationForward("C1", 1, 1),
                                            Arrays.asList("i1:input", "o1:output"))
                                    .build();
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            int concurrency = 0;
            Settings settings = new Settings(concurrency, 1);
            // Run the processor with 0 concurrency
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            assertTrue(processor.waitForAssignments(Duration.ofSeconds(10)));
            streamManager.append("input", Record.of("foo", null));
            // there is no consumer thread the processor is never started
            assertTrue(processor.isTerminated());
            assertTrue(processor.drainAndStop(Duration.ofSeconds(1)));
            processor.shutdown();
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // the record has not been processed
            assertEquals(lag.toString(), 1, lag.lag());
        }
    }

    // ---------------------------------
    // helpers
    protected int readOutputCounter(LogManager manager) throws InterruptedException {
        int partitions = manager.size(Name.ofUrn(OUTPUT_STREAM));
        int ret = 0;
        for (int i = 0; i < partitions; i++) {
            ret += readCounterFromPartition(manager, OUTPUT_STREAM, i);
        }
        return ret;
    }

    protected int readCounterFromPartition(LogManager manager, String stream, int partition)
            throws InterruptedException {
        LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test/results"),
                LogPartition.of(Name.ofUrn(stream), partition), codec);
        int result = 0;
        tailer.toStart();
        for (LogRecord<Record> logRecord = tailer.read(
                Duration.ofMillis(1000)); logRecord != null; logRecord = tailer.read(Duration.ofMillis(500))) {
            result += Integer.valueOf(logRecord.message().getKey());
        }
        tailer.commit();
        return result;
    }

    protected int countRecordIn(LogManager manager, String stream) throws Exception {
        int ret = 0;
        for (int i = 0; i < manager.size(Name.ofUrn(stream)); i++) {
            ret += countRecordInPartition(manager, stream, i);
        }
        return ret;
    }

    protected int countRecordInPartition(LogManager manager, String stream, int partition) throws Exception {
        try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test/results"),
                LogPartition.of(Name.ofUrn(stream), partition), codec)) {
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
