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

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2025.12
 */
@Deploy("org.nuxeo.ecm.core.bulk:OSGI-INF/bulk-introspection-io-contrib.xml")
public class StreamIntrospectionStreamListJsonWriterTest extends
        AbstractJsonWriterTest.Local<StreamIntrospectionStreamJsonWriter.ListJsonWriter, List<StreamIntrospection.Stream>> {

    public StreamIntrospectionStreamListJsonWriterTest() {
        super(StreamIntrospectionStreamJsonWriter.ListJsonWriter.class, List.class,
                TypeUtils.parameterize(List.class, StreamIntrospection.Stream.class));
    }

    @Test
    public void testEmptyIntrospection() throws IOException {
        var streamIntrospection = getJsonAsStreamIntrospection("{}");
        String streams = asJson(streamIntrospection.streams());
        assertEquals("[]", streams);
    }

    @Test
    public void testStreams() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection.json");
        String streams = asJson(streamIntrospection.streams());
        assertTrue(streams, streams.contains("bulk/command"));
    }

    @Test
    public void testStreamsContent() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection.json");
        var jsonAssert = jsonAssert(streamIntrospection.streams());
        jsonAssert.isArray().length(34);
        var firstStreamAssert = jsonAssert.get(0);
        firstStreamAssert.has("name").isEquals("bulk/recomputeThumbnails");
        firstStreamAssert.has("partitions").isEquals(1);
        firstStreamAssert.has("codec").isEquals("avro");
    }
}
