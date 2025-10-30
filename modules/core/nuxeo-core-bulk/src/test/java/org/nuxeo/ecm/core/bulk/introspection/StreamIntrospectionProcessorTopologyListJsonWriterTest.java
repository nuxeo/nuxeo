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
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospections.getClassLoaderResourceAsStreamIntrospection;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospections.getJsonAsStreamIntrospection;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2025.12
 */
@Deploy("org.nuxeo.ecm.core.bulk:OSGI-INF/bulk-introspection-io-contrib.xml")
public class StreamIntrospectionProcessorTopologyListJsonWriterTest extends
        AbstractJsonWriterTest.Local<StreamIntrospectionProcessorTopologyJsonWriter.ListJsonWriter, List<StreamIntrospection.ProcessorTopology>> {

    public StreamIntrospectionProcessorTopologyListJsonWriterTest() {
        super(StreamIntrospectionProcessorTopologyJsonWriter.ListJsonWriter.class,
                StreamIntrospection.ProcessorTopology.class);
    }

    @Test
    public void testEmptyIntrospection() throws IOException {
        var streamIntrospection = getJsonAsStreamIntrospection("{}");
        String consumers = asJson(streamIntrospection.consumers("bulk/whatever"));
        assertEquals("[]", consumers);
    }

    @Test
    public void testConsumers() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection.json");
        String consumers = asJson(streamIntrospection.consumers("bulk/command"));
        assertTrue(consumers.contains("bulk/scroller"));

        consumers = asJson(streamIntrospection.consumers("unknown"));
        assertEquals("[]", consumers);
    }
}
