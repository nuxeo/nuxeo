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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.bulk.introspection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospections.getClassLoaderResourceAsStreamIntrospection;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospections.getJsonAsStreamIntrospection;

import java.io.IOException;
import java.time.Instant;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.runtime.test.runner.Deploy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 2025.12
 */
@Deploy("org.nuxeo.ecm.core.bulk:OSGI-INF/bulk-introspection-io-contrib.xml")
public class ScaleActivityJsonWriterTest extends AbstractJsonWriterTest.Local<ScaleActivityJsonWriter, ScaleActivity> {

    public ScaleActivityJsonWriterTest() {
        super(ScaleActivityJsonWriter.class, ScaleActivity.class);
    }

    @Test
    public void testEmptyIntrospection() throws IOException {
        var streamIntrospection = getJsonAsStreamIntrospection("{}");
        var scaleActivity = new StreamIntrospectionToScaleActivity().apply(streamIntrospection);
        String activity = asJson(scaleActivity);
        assertTrue(activity, activity.contains("scale"));
    }

    @Test
    public void testScaleUp() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection-cluster.json");
        var scaleActivity = new StreamIntrospectionToScaleActivity(Instant.ofEpochSecond(1678439100)).apply(
                streamIntrospection);
        String out = asJson(scaleActivity);
        assertJsonEquals("data/scale-up.json", out);
    }

    @Test
    public void testScaleWithoutMetrics() throws IOException {
        String in = getClassLoaderResourceAsString("data/introspection-cluster.json");
        // rename the metrics array so no metrics will be found
        in = in.replace("\"metrics\": [", "\"no-metrics\": [");
        var streamIntrospection = getJsonAsStreamIntrospection(in);
        var scaleActivity = new StreamIntrospectionToScaleActivity(Instant.ofEpochSecond(1678439100)).apply(
                streamIntrospection);
        String out = asJson(scaleActivity);
        assertJsonEquals("data/scale-no-data.json", out);
    }

    @Test
    public void testScaleIdle() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection-cluster-idle.json");
        var scaleActivity = new StreamIntrospectionToScaleActivity(Instant.ofEpochSecond(1678439100)).apply(
                streamIntrospection);
        String out = asJson(scaleActivity);
        assertJsonEquals("data/scale-idle.json", out);
        // activity for a given timestamp in the future discards old metrics
        scaleActivity = new StreamIntrospectionToScaleActivity(Instant.ofEpochSecond(1778439100)).apply(
                streamIntrospection);
        out = asJson(scaleActivity);
        assertJsonEquals("data/scale-no-data.json", out);
    }

    @Test
    public void testScaleConstantLoad() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection(
                "data/introspection-cluster-constant.json");
        var scaleActivity = new StreamIntrospectionToScaleActivity(Instant.ofEpochSecond(1709562437)).apply(
                streamIntrospection);
        String out = asJson(scaleActivity);
        assertJsonEquals("data/scale-constant.json", out);
    }

    @Test
    public void testScaleConstantLoad2() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection(
                "data/introspection-cluster-constant2.json");
        var scaleActivity = new StreamIntrospectionToScaleActivity(Instant.ofEpochSecond(1756647244)).apply(
                streamIntrospection);
        String out = asJson(scaleActivity);
        assertJsonEquals("data/scale-constant2.json", out);
    }

    protected static void assertJsonEquals(String resourcePath, String actual) {
        ObjectMapper mapper = new ObjectMapper();
        String expected = getClassLoaderResourceAsString(resourcePath);
        try {
            String prettyExpected = mapper.readTree(expected).toPrettyString();
            String prettyActual = mapper.readTree(actual).toPrettyString();
            assertEquals(prettyExpected, prettyActual);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to prettify JSON", e);
        }
    }
}
