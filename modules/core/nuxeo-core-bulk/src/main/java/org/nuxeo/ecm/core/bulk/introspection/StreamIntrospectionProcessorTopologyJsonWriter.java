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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.12
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamIntrospectionProcessorTopologyJsonWriter
        extends AbstractJsonWriter<StreamIntrospection.ProcessorTopology> {

    public static final String FORMAT_PARAMETER = "topologyFormat";

    @Override
    public void write(StreamIntrospection.ProcessorTopology topology, JsonGenerator jg) throws IOException {
        if (ctx.getParameter(FORMAT_PARAMETER) == OutputFormat.PRETTIER) {
            if (topology instanceof StreamIntrospection.ProcessorStreamConsumerTopology consumerTopology) {
                jg.writeStringField("stream", consumerTopology.stream());
                jg.writeStringField("consumer", consumerTopology.consumer());
            } else if (topology instanceof StreamIntrospection.ProcessorStreamProducerTopology producerTopology) {
                jg.writeStringField("stream", producerTopology.stream());
                jg.writeStringField("producer", producerTopology.producer());
            }
        } else {
            jg.writeStartArray();
            jg.writeString(topology.source());
            jg.writeString(topology.target());
            jg.writeEndArray();
        }
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class ListJsonWriter extends AbstractJsonWriter<List<StreamIntrospection.ProcessorTopology>> {

        @Override
        public void write(List<StreamIntrospection.ProcessorTopology> topologies, JsonGenerator jg) throws IOException {
            jg.writeStartArray();
            var prettier = ctx.getParameter(FORMAT_PARAMETER) == OutputFormat.PRETTIER;
            topologies.forEach(ThrowableConsumer.asConsumer(topology -> {
                if (prettier) {
                    jg.writeStartObject();
                } else {
                    jg.writeStartArray();
                }
                writeEntity(topology, jg);
                if (prettier) {
                    jg.writeEndObject();
                } else {
                    jg.writeEndArray();
                }
            }));
            jg.writeEndArray();
        }
    }

    public enum OutputFormat {

        /**
         * Prints the topology into the following formats:
         * <ul>
         * <li>for {@link StreamIntrospection.ProcessorStreamConsumerTopology consumer}:
         *
         * <pre>
         * {@code
         * {
         *     "stream": "STREAM_NAME",
         *     "consumer": "CONSUMER_NAME"
         * }
         * }
         * </pre>
         *
         * </li>
         * <li>for {@link StreamIntrospection.ProcessorStreamProducerTopology producer}:
         *
         * <pre>
         * {@code
         * {
         *     "stream": "STREAM_NAME",
         *     "producer": "PRODUCER_NAME"
         * }
         * }
         * </pre>
         *
         * </li>
         * </ul>
         */
        PRETTIER,

        /**
         * Prints the topology into the following format:
         *
         * <pre>
         * {@code
         * [
         *     "SOURCE_NAME",
         *     "TARGET_NAME"
         * ]
         * }
         * </pre>
         */
        DEFAULT
    }
}
