/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.metrics.reporter.patch;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/**
 * Tests for {@link StatsDPayloadBuilder}.
 *
 * @since 2025.19
 */
public class TestStatsDPayloadBuilder {

    /**
     * With an empty prefix, metric names are sent as-is, matching the documented names.
     */
    @Test
    public void testEmptyPrefixSendsNameAsIs() {
        var builder = new StatsDPayloadBuilder("", "myhost", Map.of());
        var sb = new StringBuilder();

        builder.appendMetricLine(sb, "nuxeo.streams.failure", null, 1.0);
        builder.appendMetricLine(sb, "jvm.memory.heap.max", null, 512.0);
        builder.appendMetricLine(sb, "tomcat.activeSessions", null, 10.0);
        String output = sb.toString();
        assertTrue("nuxeo metric should be sent as-is: " + output, output.contains("nuxeo.streams.failure:"));
        assertTrue("jvm metric should be sent as-is: " + output, output.contains("jvm.memory.heap.max:"));
        assertTrue("tomcat metric should be sent as-is: " + output, output.contains("tomcat.activeSessions:"));
    }

    /**
     * With a null prefix, metric names are sent as-is (same behavior as empty).
     */
    @Test
    public void testNullPrefixSendsNameAsIs() {
        var builder = new StatsDPayloadBuilder(null, "myhost", Map.of());
        var sb = new StringBuilder();

        builder.appendMetricLine(sb, "jvm.memory.heap.max", null, 512.0);
        String line = sb.toString();
        assertTrue("jvm metric should be sent as-is: " + line, line.startsWith("jvm.memory.heap.max:"));
    }

    /**
     * With a non-empty prefix, it is prepended to all metric names.
     */
    @Test
    public void testNonEmptyPrefixIsPrepended() {
        var builder = new StatsDPayloadBuilder("nuxeo", "myhost", Map.of());
        var sb = new StringBuilder();

        builder.appendMetricLine(sb, "jvm.memory.heap.max", null, 512.0);
        builder.appendMetricLine(sb, "nuxeo.streams.failure", null, 1.0);
        String output = sb.toString();
        assertTrue("jvm metric should be prefixed: " + output, output.contains("nuxeo.jvm.memory.heap.max:"));
        assertTrue("nuxeo metric should be prefixed: " + output, output.contains("nuxeo.nuxeo.streams.failure:"));
    }

    /**
     * Tags and dimensions are appended correctly in DogStatsD format.
     */
    @Test
    public void testTagsInPayload() {
        var builder = new StatsDPayloadBuilder("", "myhost", Map.of("env", "prod"));
        var sb = new StringBuilder();

        builder.appendMetricLine(sb, "nuxeo.streams.failure", Map.of("computation", "myComp"), 3.0);
        String line = sb.toString();
        assertTrue("Expected host tag: " + line, line.contains("host:myhost"));
        assertTrue("Expected env tag: " + line, line.contains("env:prod"));
        assertTrue("Expected computation tag: " + line, line.contains("computation:myComp"));
    }
}
