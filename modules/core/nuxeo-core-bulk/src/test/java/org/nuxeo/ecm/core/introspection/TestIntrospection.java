/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.introspection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 11.5
 */
public class TestIntrospection {

    @Test
    public void testPumlConversion() throws Exception {
        String json = readFile("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String puml = convert.getPuml();
        assertTrue(puml.contains("@startuml"));
        assertTrue(puml.contains("@enduml"));
    }

    @Test
    public void testPumlConversionSimple() throws Exception {
        String json = readFile("data/simple.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String puml = convert.getPuml();
        assertEquals(readFile("data/simple.puml"), puml);
    }

    @Test
    public void testScaleUp() throws Exception {
        String in = readFile("data/introspection-cluster.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1678439100);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(readFile("data/scale-up.json")), mapper.readTree(out));
    }

    @Test
    public void testScaleWithoutMetrics() throws Exception {
        String in = readFile("data/introspection-cluster.json");
        // rename the metrics array so no metrics will be found
        in = in.replace("\"metrics\": [", "\"no-metrics\": [");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1678439100);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(readFile("data/scale-no-data.json")), mapper.readTree(out));
    }

    @Test
    public void testScaleIdle() throws Exception {
        String in = readFile("data/introspection-cluster-idle.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1678439100);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(readFile("data/scale-idle.json")), mapper.readTree(out));
        // activity for a given timestamp in the future discards old metrics
        out = convert.getActivity(1778439100);
        assertEquals(mapper.readTree(readFile("data/scale-no-data.json")), mapper.readTree(out));
    }

    @Test
    public void testScaleConstantLoad() throws Exception {
        String in = readFile("data/introspection-cluster-constant.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1709562437);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(readFile("data/scale-constant.json")), mapper.readTree(out));
    }

    @Test
    public void testScaleConstantLoad2() throws Exception {
        String json = readFile("data/introspection-cluster-constant2.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String out = convert.getActivity(1756647244);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(readFile("data/scale-constant2.json")), mapper.readTree(out));
    }

    @Test
    public void testStreams() throws Exception {
        String json = readFile("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String streams = convert.getStreams();
        assertTrue(streams, streams.contains("bulk/command"));
    }

    @Test
    public void testEmptyJson() {
        assertThrows(IllegalArgumentException.class, () -> new StreamIntrospectionConverter(null));
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter("{}");
        assertEquals("[]", convert.getStreams());
        assertEquals("[]", convert.getConsumers("bulk/whatever"));
        String activity = convert.getActivity();
        assertTrue(activity, activity.contains("scale"));
        String puml = convert.getPuml();
        assertTrue(puml, puml.contains("@startuml"));
    }

    @Test
    public void testConsumers() throws Exception {
        String json = readFile("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String consumers = convert.getConsumers("bulk/command");
        assertTrue(consumers.contains("bulk/scroller"));

        consumers = convert.getConsumers("unknown");
        assertEquals("[]", consumers);
    }

    protected String readFile(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            return IOUtils.toString(is, UTF_8);
        }
    }

}
