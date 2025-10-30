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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.bulk.introspection;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.core.io.marshallers.puml.AbstractPlantUMLWriter;
import org.nuxeo.ecm.core.io.marshallers.puml.PlantUMLPrinter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

/**
 * Converts stream introspection data to PlantUML diagram format.
 *
 * @since 2025.12
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamIntrospectionPlantUMLWriter extends AbstractPlantUMLWriter<StreamIntrospection> {

    protected static final StreamIntrospectionToStreamFlattenMetrics TO_STREAM_FLATTEN_METRICS = new StreamIntrospectionToStreamFlattenMetrics();

    @Override
    protected void write(StreamIntrospection entity, PlantUMLPrinter pumlPrinter) throws IOException {
        var streamMetrics = TO_STREAM_FLATTEN_METRICS.apply(entity);

        pumlPrinter.writeStartDocument();
        pumlPrinter.writeTitle("Stream Introspection at " + streamMetrics.date());

        pumlPrinter.writeSkinparam("defaultFontName", "Courier");
        pumlPrinter.writeSkinparam("handwritten", "false");
        pumlPrinter.writeSkinparam("queueBackgroundColor", "LightYellow");
        pumlPrinter.writeSkinparam("nodeBackgroundColor", "Azure");
        pumlPrinter.writeSkinparam("componentBackgroundColor", "Azure");
        pumlPrinter.writeSkinparam("nodebackgroundColor<<failure>>", "Yellow");
        pumlPrinter.writeSkinparam("componentbackgroundColor<<failure>>", "Yellow");
        pumlPrinter.writeSkinparam("component", "BorderColor", "black", "ArrowColor", "#CC6655");

        // write streams
        entity.streams().forEach(ThrowableConsumer.asConsumer(stream -> write(stream, streamMetrics, pumlPrinter)));

        // write computations/topology
        entity.processors()
              .forEach(ThrowableConsumer.asConsumer(processor -> write(processor, streamMetrics, pumlPrinter)));

        pumlPrinter.writeEndDocument();
    }

    protected void write(StreamIntrospection.Stream stream, StreamFlattenMetrics streamMetrics,
            PlantUMLPrinter pumlPrinter) throws IOException {
        pumlPrinter.writeStartQueue("stream." + stream.name());
        pumlPrinter.writeFreeText(stream.name());
        pumlPrinter.writeTextSeparator();
        pumlPrinter.writeFreeText("partitions: " + stream.partitions());
        pumlPrinter.writeFreeText("codec: " + stream.codec());
        pumlPrinter.writeTextSeparator();
        long records = 0;
        var streamMetric = streamMetrics.streamMetric(stream.name());
        if (streamMetric != null && streamMetric.end() != null) {
            records = streamMetric.end().longValue();
        }
        pumlPrinter.writeFreeText("records: " + records);
        pumlPrinter.writeEndQueue();
    }

    protected void write(StreamIntrospection.Processor processor, StreamFlattenMetrics streamMetrics,
            PlantUMLPrinter pumlPrinter) throws IOException {
        StreamIntrospection.ProcessorMetadata metadata = processor.metadata();
        // write computations
        // ret.append("rectangle node." + host + " {\n");
        processor.computations()
                 .forEach(ThrowableConsumer.asConsumer(
                         computation -> write(computation, metadata, streamMetrics, pumlPrinter)));
        // ret.append("}\n");

        // write topology
        processor.topology()
                 .forEach(ThrowableConsumer.asConsumer(
                         topology -> write(topology, metadata, streamMetrics, pumlPrinter)));
    }

    protected void write(StreamIntrospection.ProcessorComputation computation,
            StreamIntrospection.ProcessorMetadata metadata, StreamFlattenMetrics streamMetrics,
            PlantUMLPrinter pumlPrinter) throws IOException {
        var computationMetric = streamMetrics.computationMetric(metadata.nodeId(), computation.name());
        String computationIdentifier = "computation.%s.%s".formatted(computation.name(), metadata.nodeId());
        if (computationMetric == null || computationMetric.failure() == null) {
            pumlPrinter.writeStartComponent(computationIdentifier);
        } else {
            pumlPrinter.writeStartComponent(computationIdentifier, "failure");
        }
        pumlPrinter.writeFreeText("%s on %s".formatted(computation.name(), metadata.nodeId()));
        pumlPrinter.writeTextSeparator();
        pumlPrinter.writeFreeText("created: " + metadata.created().toString());
        pumlPrinter.writeFreeText("threads: " + computation.threads());
        pumlPrinter.writeFreeText("continue on failure: " + computation.continueOnFailure());
        // batch info
        if (computation.batchCapacity() > 1) {
            pumlPrinter.writeFreeText(
                    "batch %s %sms".formatted(computation.batchCapacity(), computation.batchThreshold().toMillis()));
        } else {
            pumlPrinter.writeFreeText("no batch");
        }
        // retry
        if (computation.maxRetries() > 0) {
            pumlPrinter.writeFreeText("max retry: %s, delay: %sms".formatted(computation.maxRetries(),
                    computation.retryDelay().toMillis()));
        } else {
            pumlPrinter.writeFreeText("no retry");
        }
        // metrics
        if (computationMetric != null && computationMetric.record() != null) {
            // record
            pumlPrinter.writeTextSeparator();
            if (computationMetric.failure() != null) {
                pumlPrinter.writeFreeText("FAILURE: " + computationMetric.failure());
            }
            StreamIntrospection.TimerMetric computationMetricRecord = computationMetric.record();
            pumlPrinter.writeFreeText("record count: %s, total: %.3fs".formatted(computationMetricRecord.count(),
                    computationMetricRecord.sum()));
            if (computationMetric.skipped() != null) {
                pumlPrinter.writeFreeText("record skipped: " + computationMetric.skipped());
            }
            pumlPrinter.writeFreeText("mean: %.3fs, p50: %.3fs, p99: %.3fs".formatted(computationMetricRecord.mean(),
                    computationMetricRecord.p50(), computationMetricRecord.p99()));
            pumlPrinter.writeFreeText("rate 1min: %.2fop/s, 5min: %.2fop/s".formatted(computationMetricRecord.rate1m(),
                    computationMetricRecord.rate5m()));
            // timer
            StreamIntrospection.TimerMetric computationMetricTimer = computationMetric.timer();
            if (computationMetricTimer != null) {
                pumlPrinter.writeTextSeparator();
                pumlPrinter.writeFreeText("timer count: %s, total: %.3fs".formatted(computationMetricTimer.count(),
                        computationMetricTimer.sum()));
                pumlPrinter.writeFreeText("mean: %.3fs, p50: %.3fs, p99: %.3fs".formatted(computationMetricTimer.mean(),
                        computationMetricTimer.p50(), computationMetricTimer.p99()));
                pumlPrinter.writeFreeText("rate 5min: %.2fop/s".formatted(computationMetricTimer.rate5m()));
            }
        }
        pumlPrinter.writeEndComponent();
    }

    protected void write(StreamIntrospection.ProcessorTopology topology, StreamIntrospection.ProcessorMetadata metadata,
            StreamFlattenMetrics streamMetrics, PlantUMLPrinter pumlPrinter) throws IOException {
        if (topology instanceof StreamIntrospection.ProcessorStreamConsumerTopology consumer) {
            write(consumer, metadata, streamMetrics, pumlPrinter);
        } else if (topology instanceof StreamIntrospection.ProcessorStreamProducerTopology producer) {
            write(producer, metadata, pumlPrinter);
        }
    }

    protected void write(StreamIntrospection.ProcessorStreamConsumerTopology topology,
            StreamIntrospection.ProcessorMetadata metadata, StreamFlattenMetrics streamMetrics,
            PlantUMLPrinter pumlPrinter) throws IOException {
        String computationIdentifier = topology.target() + "." + metadata.nodeId();
        var streamMetric = streamMetrics.streamMetric(topology.stream());
        if (streamMetric != null) {
            var streamComputationMetric = streamMetric.computationMetric(topology.consumer());
            if (streamComputationMetric != null) {
                var lag = streamComputationMetric.lag();
                var latency = streamComputationMetric.latency();
                var pos = streamComputationMetric.pos();
                var end = streamMetric.end();
                // provide info only when there is a lag
                if (lag != null && lag.intValue() != 0) {
                    pumlPrinter.writeArrow(topology.source(), computationIdentifier,
                            "%s/%s lag: %s, latency: %ss".formatted(pos, end, lag, latency));
                    return;
                }
            }
        }
        pumlPrinter.writeArrow(topology.source(), computationIdentifier);
    }

    protected void write(StreamIntrospection.ProcessorStreamProducerTopology topology,
            StreamIntrospection.ProcessorMetadata metadata, PlantUMLPrinter pumlPrinter) throws IOException {
        pumlPrinter.writeArrow(topology.source() + "." + metadata.nodeId(), topology.target());
    }
}
