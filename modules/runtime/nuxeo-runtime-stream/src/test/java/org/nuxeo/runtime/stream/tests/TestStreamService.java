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
package org.nuxeo.runtime.stream.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.time.Duration;
import java.util.Collections;
import java.util.SortedMap;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.management.api.ProbeStatus;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.stream.StreamMetricsComputation;
import org.nuxeo.runtime.stream.StreamProbe;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeStreamFeature.class)
@Deploy("org.nuxeo.runtime.stream:test-stream-contrib.xml")
public class TestStreamService {

    @Inject
    public StreamService service;

    @Test
    public void testLogManagerAccess() {
        assertNotNull(service);
        LogManager manager = service.getLogManager();
        assertNotNull(manager);
        assertNotNull(service.getStreamManager());
        assertTrue(manager.exists(Name.ofUrn("input")));
        assertTrue(manager.exists(Name.ofUrn("output")));
        assertTrue(manager.exists(Name.ofUrn("myLog")));
        assertTrue(manager.exists(Name.ofUrn("cq/cq-foo")));
        assertFalse(manager.exists(Name.ofUrn("aLogThatShouldNotBeCreated")));
        assertFalse(manager.exists(Name.ofUrn("unexisting")));
    }

    @Test
    public void testBasicLogUsage() throws Exception {
        @SuppressWarnings("resource")
        LogManager manager = service.getLogManager();
        Name logName = Name.ofUrn("myLog");
        String key = "a key";
        String value = "a value";

        LogAppender<Record> appender = manager.getAppender(logName);
        appender.append(key, Record.of(key, value.getBytes("UTF-8")));

        try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("myGroup"), logName)) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
            assertNotNull(logRecord);
            assertEquals(key, logRecord.message().getKey());
            assertEquals(value, new String(logRecord.message().getData(), "UTF-8"));
        }
        // never close the manager this is done by the service
    }

    @Test
    public void testStreamProcessor() throws Exception {
        @SuppressWarnings("resource")
        LogManager manager = service.getLogManager();
        StreamManager streamManager = service.getStreamManager();

        @SuppressWarnings("resource") // not ours to close
        LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("counter"), Name.ofUrn("output"));

        // add an input message
        String key = "a key";
        String value = "a value";
        streamManager.append("input", Record.of(key, value.getBytes("UTF-8")));
        streamManager.append("input", Record.of("skipMeNow", null));
        streamManager.append("input", Record.of("changeMeNow", null));

        // the computation should forward the first record as it is to the output
        LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
        assertNotNull("Record not found in output stream", logRecord);
        assertEquals(key, logRecord.message().getKey());
        assertEquals(value, new String(logRecord.message().getData(), "UTF-8"));

        // the second record should be skipped
        // the third record should have a modified key
        logRecord = tailer.read(Duration.ofSeconds(1));
        assertNotNull("Record not found in output stream", logRecord);
        assertEquals("changedNow", logRecord.message().getKey());

    }

    @Test
    public void testDisabledStreamProcessor() throws Exception {
        @SuppressWarnings("resource")
        StreamManager streamManager = service.getStreamManager();

        try {
            streamManager.append("streamThatDoesNotExist", Record.of("key", null));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected because stream does not exist
        }

        try {
            streamManager.append("input2", Record.of("key", null));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected because processor is disabled so its input streams are not initialized
        }
    }

    @Test
    public void testDefaultPartitions() throws Exception {
        @SuppressWarnings("resource")
        LogManager manager = service.getLogManager();
        Name streamName = Name.ofUrn("s1");
        assertTrue(manager.exists(streamName));
        assertEquals(2, manager.size(streamName));
    }

    @Test
    public void testRegisterAndExternalStream() throws Exception {
        // make sure the processor is initialized
        LogManager manager = service.getLogManager();
        assertTrue(manager.exists(Name.ofUrn("input3")));
        assertTrue(manager.exists(Name.ofUrn("output3")));
        assertTrue(manager.exists(Name.ofUrn("registerInput")));
        assertFalse(manager.exists(Name.ofUrn("externalOutput")));

        LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test"), Name.ofUrn("output3"));
        tailer.toEnd();

        // make sure the processor is not started
        @SuppressWarnings("resource")
        StreamManager streamManager = service.getStreamManager();
        streamManager.append("input3", Record.of("key", null));
        assertNull(tailer.read(Duration.ofSeconds(1)));

        // We can get the processor
        StreamProcessor processor = streamManager.createStreamProcessor("registerProcessor");
        assertNotNull(processor);

        // and manage it
        processor.start();
        processor.waitForAssignments(Duration.ofSeconds(10));
        processor.drainAndStop(Duration.ofSeconds(5));
        assertEquals("key", tailer.read(Duration.ofSeconds(1)).message().getKey());
    }

    @Test
    public void testProbe() throws Exception {
        @SuppressWarnings("resource")
        StreamManager streamManager = service.getStreamManager();
        StreamProbe probe = new StreamProbe();
        probe.reset();
        ProbeStatus status = probe.run();
        assertFalse(status.isFailure());
        // generate a failure
        try {
            streamManager.append("inputFailure", Record.of("key", null));
            Thread.sleep(500);
            // the probe failure is delayed by 1s after the first probe run
            status = probe.run();
            assertFalse(status.toString(), status.isFailure());

            Thread.sleep(1500);
            status = probe.run();
            assertTrue(status.toString(), status.isFailure());
        } finally {
            probe.reset();
        }
    }

    @Test
    public void testStreamMetrics() {
        // before running the stream metrics computation no stream metrics are registered
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);
        SortedMap<MetricName, Gauge<?>> gauges = registry.getGauges((name, metric) -> name.getKey().startsWith("nuxeo.streams.global"));
        assertTrue(gauges.isEmpty());

        // create a stream metrics computation
        StreamMetricsComputation comp = new StreamMetricsComputation(Duration.ofMinutes(1), null);
        ComputationContext context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        assertFalse(context.isSpareComputation());
        comp.init(context);
        // run the timer that is supposed to register the stream metrics
        comp.processTimer(context, "tracker", 0);

        // we have a gauges per existing computations
        gauges = registry.getGauges((name, metric) -> name.getKey().startsWith("nuxeo.streams.global"));
        assertFalse(gauges.isEmpty());
        Gauge<?> gauge = gauges.get(MetricName.build("nuxeo.streams.global.stream.group.end")
                                              .tagged("stream", "input")
                                              .tagged("group", "myComputation"));
        assertNotNull(gauge);
        assertNotNull(gauge.getValue());

        // stop the computation should unregister the metrics
        comp.destroy();
        gauges = registry.getGauges((name, metric) -> name.getKey().startsWith("nuxeo.streams.global"));
        assertTrue(gauges.isEmpty());
    }
}
