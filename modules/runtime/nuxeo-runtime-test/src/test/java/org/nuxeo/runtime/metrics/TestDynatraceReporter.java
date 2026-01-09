/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.Timer;

/**
 * Tests for the Dynatrace reporter.
 *
 * @since 2025.15
 */
@RunWith(FeaturesRunner.class)
@Features(DynatraceReporterFeature.class)
public class TestDynatraceReporter {

    @Inject
    protected DynatraceReporterFeature feature;

    @Test
    public void testCounterMetric() throws Exception {
        // Create a counter
        Counter counter = feature.getRegistry().counter(MetricName.build("test.counter").tagged("env", "test"));
        counter.inc(42);

        // Create reporter and report
        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        // Receive and verify
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        assertTrue("Should contain metric name", line.contains("nuxeo.test.counter:42|g"));
        assertTrue("Should contain host dimension", line.contains("host:testhost"));
        assertTrue("Should contain tag dimension", line.contains("env:test"));
    }

    @Test
    public void testTimerMetric() throws Exception {
        // Create a timer and record some values
        Timer timer = feature.getRegistry().timer(MetricName.build("test.timer").tagged("operation", "save"));
        timer.update(100, TimeUnit.MILLISECONDS);
        timer.update(200, TimeUnit.MILLISECONDS);
        timer.update(150, TimeUnit.MILLISECONDS);

        // Create reporter and report
        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        // Receive and verify
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        // Timer should produce multiple metrics
        boolean hasCount = receivedMessages.stream().anyMatch(l -> l.contains("test.timer.count:3|g"));
        boolean hasMean = receivedMessages.stream().anyMatch(l -> l.contains("test.timer.mean:"));
        boolean hasP95 = receivedMessages.stream().anyMatch(l -> l.contains("test.timer.p95:"));

        assertTrue("Should have count metric", hasCount);
        assertTrue("Should have mean metric", hasMean);
        assertTrue("Should have p95 metric", hasP95);

        // All metrics should have the operation tag
        assertTrue("All metrics should have operation tag",
                receivedMessages.stream().allMatch(l -> l.contains("operation:save")));
    }

    @Test
    public void testGaugeMetric() throws Exception {
        // Create a gauge
        feature.getRegistry().gauge(MetricName.build("test.gauge").tagged("cache", "default"), () -> () -> 123L);

        // Create reporter and report
        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        // Receive and verify
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        assertTrue("Should contain gauge value", line.contains("nuxeo.test.gauge:123|g"));
        assertTrue("Should contain cache dimension", line.contains("cache:default"));
    }

    @Test
    public void testDimensionSanitization() throws Exception {
        // Create counter with special characters in tag value
        Counter counter = feature.getRegistry().counter(MetricName.build("test.sanitize").tagged("path", "/api/v1:endpoint"));
        counter.inc();

        // Create reporter and report
        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        // Receive and verify
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        // Colon should be sanitized, slash is preserved (but leading slash replaced)
        assertTrue("Should not contain raw colon in dimension value", !line.contains("path:/api/v1:endpoint"));
        assertTrue("Should contain sanitized path with preserved slashes", line.contains("path:_api/v1_endpoint"));
    }

    @Test
    public void testGlobalDimensions() throws Exception {
        // Create counter
        Counter counter = feature.getRegistry().counter(MetricName.build("test.global"));
        counter.inc();

        // Create reporter with global dimensions
        try (var reporter = feature.createReporter(Map.of("env", "prod", "app", "nuxeo"))) {
            reporter.report();
        }

        // Receive and verify
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        assertTrue("Should contain env dimension", line.contains("env:prod"));
        assertTrue("Should contain app dimension", line.contains("app:nuxeo"));
    }

    @Test
    public void testDeniedExpansions() throws Exception {
        // Create a timer
        Timer timer = feature.getRegistry().timer(MetricName.build("test.denied"));
        timer.update(100, TimeUnit.MILLISECONDS);

        // Create reporter with denied expansions (no p99, p999)
        Set<MetricAttribute> denied = Set.of(MetricAttribute.P99, MetricAttribute.P999, MetricAttribute.P98,
                MetricAttribute.P75);

        try (var reporter = feature.createReporter(Collections.emptyMap(), denied, false)) {
            reporter.report();
        }

        // Receive and verify
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();

        // Should have some metrics but not p99, p999
        boolean hasP95 = receivedMessages.stream().anyMatch(l -> l.contains("test.denied.p95:"));
        boolean hasP99 = receivedMessages.stream().anyMatch(l -> l.contains("test.denied.p99:"));
        boolean hasP999 = receivedMessages.stream().anyMatch(l -> l.contains("test.denied.p999:"));

        assertTrue("Should have p95 (not denied)", hasP95);
        assertFalse("Should NOT have p99 (denied)", hasP99);
        assertFalse("Should NOT have p999 (denied)", hasP999);
    }

    @Test
    public void testEmptyTimerSkipped() throws Exception {
        // Create a timer but don't record any values
        feature.getRegistry().timer(MetricName.build("test.empty.timer"));

        // Create reporter and report
        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        // Receive - should get nothing for empty timer
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();

        boolean hasEmptyTimer = receivedMessages.stream().anyMatch(l -> l.contains("test.empty.timer"));
        assertFalse("Empty timer should not be reported", hasEmptyTimer);
    }

    @Test
    public void testEmptyTimerAsCountEnabled() throws Exception {
        // Create a timer but don't record any values
        feature.getRegistry().timer(MetricName.build("test.empty.timer.ascount"));

        // Create reporter with emptyTimerAsCount enabled
        try (var reporter = feature.createReporter(Collections.emptyMap(), Collections.emptySet(), true)) {
            reporter.report();
        }

        // Receive - should get only count metric for empty timer
        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();

        boolean hasCount = receivedMessages.stream().anyMatch(l -> l.contains("test.empty.timer.ascount.count:0|g"));
        boolean hasMean = receivedMessages.stream().anyMatch(l -> l.contains("test.empty.timer.ascount.mean:"));
        boolean hasP95 = receivedMessages.stream().anyMatch(l -> l.contains("test.empty.timer.ascount.p95:"));

        assertTrue("Empty timer should report count=0 when emptyTimerAsCount is enabled", hasCount);
        assertFalse("Empty timer should NOT report mean when emptyTimerAsCount is enabled", hasMean);
        assertFalse("Empty timer should NOT report p95 when emptyTimerAsCount is enabled", hasP95);
    }

    @Test
    public void testMetricFormat() throws Exception {
        // Create a simple counter to verify exact format
        Counter counter = feature.getRegistry().counter(MetricName.build("format.test").tagged("key", "value"));
        counter.inc(100);

        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertEquals("Should have exactly one message", 1, receivedMessages.size());

        String line = receivedMessages.get(0);
        // Format should be: prefix.metric.name:value|g|#dim1:val1,dim2:val2
        assertTrue("Should start with prefix", line.startsWith("nuxeo.format.test:"));
        assertTrue("Should have gauge type", line.contains("|g|"));
        assertTrue("Should have dimensions section", line.contains("|#"));
    }

    @Test
    public void testNaNAndInfinitySkipped() throws Exception {
        // Create gauges with NaN and Infinity values
        feature.getRegistry().gauge(MetricName.build("test.nan"), () -> () -> Double.NaN);
        feature.getRegistry().gauge(MetricName.build("test.infinity"), () -> () -> Double.POSITIVE_INFINITY);
        feature.getRegistry().gauge(MetricName.build("test.neg.infinity"), () -> () -> Double.NEGATIVE_INFINITY);
        feature.getRegistry().gauge(MetricName.build("test.valid"), () -> () -> 42.0);

        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();

        // Only the valid gauge should be reported
        boolean hasNan = receivedMessages.stream().anyMatch(l -> l.contains("test.nan:"));
        boolean hasInfinity = receivedMessages.stream().anyMatch(l -> l.contains("test.infinity:"));
        boolean hasNegInfinity = receivedMessages.stream().anyMatch(l -> l.contains("test.neg.infinity:"));
        boolean hasValid = receivedMessages.stream().anyMatch(l -> l.contains("test.valid:42|g"));

        assertFalse("NaN gauge should be skipped", hasNan);
        assertFalse("Infinity gauge should be skipped", hasInfinity);
        assertFalse("Negative Infinity gauge should be skipped", hasNegInfinity);
        assertTrue("Valid gauge should be reported", hasValid);
    }

    @Test
    public void testMetricNameSanitization() throws Exception {
        // Create counter with special characters in metric name
        Counter counter = feature.getRegistry().counter(MetricName.build("test:metric/with spaces!"));
        counter.inc(1);

        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        // Special characters should be replaced with underscore
        assertTrue("Metric name should be sanitized", line.contains("nuxeo.test_metric_with_spaces_:1|g"));
    }

    @Test
    public void testSpecialCharsDimensionSanitization() throws Exception {
        // Create counter with tag that has only special characters
        Counter counter = feature.getRegistry().counter(MetricName.build("test.special.dim").tagged("key", ":::"));
        counter.inc(1);

        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        // Special characters become underscores and collapse to single underscore
        assertTrue("Special chars should become underscore", line.contains("key:_"));
    }

    @Test
    public void testEmptyDimensionValueSanitization() throws Exception {
        // Create counter with empty tag value
        Counter counter = feature.getRegistry().counter(MetricName.build("test.empty.dim").tagged("key", ""));
        counter.inc(1);

        try (var reporter = feature.createReporter()) {
            reporter.report();
        }

        feature.receiveMessages();
        List<String> receivedMessages = feature.getReceivedMessages();
        assertFalse("Should have received messages", receivedMessages.isEmpty());

        String line = receivedMessages.get(0);
        // Empty value should become "unknown"
        assertTrue("Empty dimension value should become 'unknown'", line.contains("key:unknown"));
    }
}
