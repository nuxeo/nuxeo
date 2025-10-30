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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.core.io.marshallers.d2.AbstractD2Writer;
import org.nuxeo.ecm.core.io.marshallers.d2.D2Printer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

/**
 * Converts stream introspection data to <a href="https://d2lang.com/">D2 diagram format</a>.
 *
 * @since 2025.12
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamIntrospectionD2Writer extends AbstractD2Writer<StreamIntrospection> {

    /** @since 2025.12 */
    public static final String PARAMETER_EXCLUDE_FILTER = "excludeFilter";

    /** @since 2025.12 */
    public static final String PARAMETER_EXCLUDE_INACTIVE = "excludeInactive";

    protected static final StreamIntrospectionToStreamFlattenMetrics TO_STREAM_FLATTEN_METRICS = new StreamIntrospectionToStreamFlattenMetrics();

    protected static final String D2_HEADER_TEMPLATE = """
            title: |md
              # %s
            | {near: top-center}

            classes: {
              stream: {
                shape: queue
                style: {
                  fill: lightyellow
                }
              }
              computation: {
                shape: rectangle
                style: {
                  fill: azure
                  shadow: true
                }
              }
              computation-failure: {
                shape: rectangle
                style: {
                  fill: yellow
                  stroke: black
                  shadow: true
                }
              }
              stream-empty: {
                shape: queue
                style: {
                  fill: lightgray
                  stroke: gray
                  opacity: 0.6
                }
              }
              computation-idle: {
                shape: rectangle
                style: {
                  fill: lightgray
                  stroke: gray
                  opacity: 0.6
                }
              }
              computation-group: {
                style: {
                  fill: lightcyan
                  stroke: darkblue
                  stroke-width: 2
                }
              }
            }
            """;

    @Override
    protected void write(StreamIntrospection entity, D2Printer d2Printer) throws IOException {
        var streamMetrics = TO_STREAM_FLATTEN_METRICS.apply(entity);

        d2Printer.writeHeader(D2_HEADER_TEMPLATE.formatted("Stream Introspection at " + streamMetrics.date()));

        // First, output all streams (outside containers)
        entity.streams()
              .stream()
              .filter(stream -> !shouldExcludeByPattern(stream.name()))
              .filter(stream -> !shouldExcludeStreamByInactivity(stream.name(), streamMetrics))
              .forEach(ThrowableConsumer.asConsumer(stream -> write(stream, streamMetrics, d2Printer)));

        var computations = collectComputations(entity, streamMetrics);
        // Create computation-group containers for each unique computation name
        var computationNames = computations.stream().map(pair -> pair.getLeft().name()).collect(Collectors.toSet());
        computationNames.forEach(ThrowableConsumer.asConsumer(computationName -> d2Printer.writeContainerWithClass(
                "group_" + computationName, computationName, "computation-group")));

        // Process each processor and place computations in their respective containers
        computations.forEach(
                ThrowableConsumer.asConsumer(pair -> write(pair.getLeft(), pair.getRight(), streamMetrics, d2Printer)));

        // Now output connections between streams and computations
        for (var processor : entity.processors()) {
            String host = processor.metadata().nodeId();
            for (var topo : processor.topology()) {
                String source = topo.source();
                String target = topo.target();

                // Apply filtering for connections
                boolean shouldSkipConnection = false;
                if (source.startsWith("stream:")) {
                    String stream = source.replace("stream:", "");
                    if (shouldExcludeByPattern(stream) || shouldExcludeStreamByInactivity(stream, streamMetrics)) {
                        shouldSkipConnection = true;
                    }
                }
                if (target.startsWith("computation:")) {
                    String computation = target.replace("computation:", "");
                    if (shouldExcludeByPattern(computation)
                            || shouldExcludeByInactivity(computation, host, streamMetrics)) {
                        shouldSkipConnection = true;
                    }
                }
                // Also check if source computation should be filtered
                if (source.startsWith("computation:")) {
                    String computation = source.replace("computation:", "");
                    if (shouldExcludeByPattern(computation)
                            || shouldExcludeByInactivity(computation, host, streamMetrics)) {
                        shouldSkipConnection = true;
                    }
                }
                // Check if target stream should be filtered
                if (target.startsWith("stream:")) {
                    String stream = target.replace("stream:", "");
                    if (shouldExcludeByPattern(stream) || shouldExcludeStreamByInactivity(stream, streamMetrics)) {
                        shouldSkipConnection = true;
                    }
                }

                if (shouldSkipConnection) {
                    continue;
                }
                if (topo instanceof StreamIntrospection.ProcessorStreamConsumerTopology consumer) {
                    write(consumer, processor.metadata(), streamMetrics, d2Printer);
                } else if (topo instanceof StreamIntrospection.ProcessorStreamProducerTopology producer) {
                    write(producer, processor.metadata(), d2Printer);
                }
            }
        }
    }

    protected void write(StreamIntrospection.ProcessorStreamConsumerTopology topology,
            StreamIntrospection.ProcessorMetadata metadata, StreamFlattenMetrics streamMetrics, D2Printer d2Printer)
            throws IOException {
        var streamIdentifier = List.of(topology.source());
        var computationIdentifier = List.of("group_" + topology.consumer(), "comp_" + metadata.nodeId());
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
                    d2Printer.writeConnectionRight(streamIdentifier, computationIdentifier,
                            "%s/%s lag: %s, latency: %ss".formatted(pos, end, lag, latency));
                    return;
                }
            }
        }
        d2Printer.writeConnectionRight(streamIdentifier, computationIdentifier);
    }

    protected void write(StreamIntrospection.ProcessorStreamProducerTopology topology,
            StreamIntrospection.ProcessorMetadata metadata, D2Printer d2Printer) throws IOException {
        d2Printer.writeConnectionRight(List.of("group_" + topology.producer(), "comp_" + metadata.nodeId()),
                List.of(topology.target()));
    }

    /**
     * Checks if a name (stream or computation) should be excluded based on patterns.
     */
    protected boolean shouldExcludeByPattern(String name) {
        var excludePatterns = ctx.<String> getParameters(PARAMETER_EXCLUDE_FILTER)
                                 .stream()
                                 .flatMap(parameter -> Stream.of(parameter.split(",")))
                                 .toList();
        if (excludePatterns.isEmpty()) {
            return false;
        }
        for (String pattern : excludePatterns) {
            if (name.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a computation should be excluded based on inactivity filter.
     */
    protected boolean shouldExcludeByInactivity(String name, String host, StreamFlattenMetrics metrics) {
        if (!ctx.getBooleanParameter(PARAMETER_EXCLUDE_INACTIVE)) {
            return false;
        }
        // A computation is considered inactive if it has no metrics (computation-idle class)
        var computationMetric = metrics.computationMetric(host, name);
        return computationMetric == null || (computationMetric.record() == null && computationMetric.timer() == null);
    }

    /**
     * Checks if a stream should be excluded based on inactivity filter.
     */
    protected boolean shouldExcludeStreamByInactivity(String streamName, StreamFlattenMetrics metrics) {
        if (!ctx.getBooleanParameter(PARAMETER_EXCLUDE_INACTIVE)) {
            return false;
        }
        // A stream is considered inactive if it has no records (stream-empty class)
        return getStreamEnd(metrics, streamName) == 0;
    }

    private void write(StreamIntrospection.Stream stream, StreamFlattenMetrics streamMetrics, D2Printer d2Printer) {
        String name = stream.name();

        int partitions = stream.partitions();
        String codec = stream.codec();
        long records = getStreamEnd(streamMetrics, name);

        String streamId = "stream:" + name;
        String streamClass = records == 0 ? "stream-empty" : "stream";

        d2Printer.writeMarkdownShapeWithClass(streamId, streamClass, """
                #### %s
                - partitions: %s
                - codec: %s
                - records: %s
                """.formatted(name, partitions, codec, records));
    }

    protected long getStreamEnd(StreamFlattenMetrics metrics, String name) {
        var streamMetric = metrics.streamMetric(name);
        if (streamMetric != null && streamMetric.end() != null) {
            return streamMetric.end().longValue();
        }
        return 0;
    }

    protected String getComputationMetrics(String host, StreamIntrospection.ProcessorComputation computation,
            StreamFlattenMetrics streamMetrics) {
        var computationMetric = streamMetrics.computationMetric(host, computation.name());
        if (computationMetric == null || (computationMetric.record() == null && computationMetric.timer() == null)) {
            return "";
        }
        StringBuilder ret = new StringBuilder();
        if (computationMetric.failure() != null) {
            ret.append("#### FAILURE: ").append(computationMetric.failure()).append("\n");
        }
        var recordMetric = computationMetric.record();
        if (recordMetric != null) {
            ret.append(getTimerAsJson("record", computationMetric, recordMetric));
        }
        var timerMetric = computationMetric.timer();
        if (timerMetric != null) {
            ret.append(getTimerAsJson("timer", computationMetric, timerMetric));
        }
        return ret.toString();
    }

    protected String getTimerAsJson(String title, StreamFlattenMetrics.ComputationMetric computationMetric,
            StreamIntrospection.TimerMetric timerMetric) {
        StringBuilder ret = new StringBuilder();
        ret.append("#### ").append(title).append("\n");
        ret.append("- count: ").append(timerMetric.count());
        if ("record".equals(title) && computationMetric.skipped() != null) {
            ret.append(", skipped: ").append(computationMetric.skipped());
        }
        ret.append(", sum: %.3f".formatted(timerMetric.sum())).append("s\n");
        ret.append("- duration: %.3fs (+/- %.3fs)\n".formatted(timerMetric.mean(), timerMetric.stddev()))
           .append("  -  min: %.3fs, max: %.3fs\n".formatted(timerMetric.min(), timerMetric.max()))
           .append("  -  p50: %.3fs, p99: %.3fs\n".formatted(timerMetric.p50(), timerMetric.p99()));
        ret.append("- rate 1min: %.2fops/s, 5min: %.2fops/s\n".formatted(timerMetric.rate1m(), timerMetric.rate5m()));
        return ret.toString();
    }

    protected String getBatchInfo(StreamIntrospection.ProcessorComputation computation) {
        StringBuilder ret = new StringBuilder();
        int retry = computation.maxRetries();
        if (retry > 0) {
            ret.append("- retry: ")
               .append(computation.maxRetries())
               .append(", delay: ")
               .append(computation.retryDelay().toMillis())
               .append("ms, ");
        } else {
            ret.append("- no retry, ");
        }
        int batchCapacity = computation.batchCapacity();
        if (batchCapacity > 1) {
            long batchThresholdMs = computation.batchThreshold().toMillis();
            ret.append("batch: ")
               .append(computation.batchCapacity())
               .append(", ")
               .append(batchThresholdMs)
               .append("ms");
        } else {
            ret.append("no batch");
        }
        return ret.toString();
    }

    protected List<Pair<StreamIntrospection.ProcessorComputation, StreamIntrospection.ProcessorMetadata>> collectComputations(
            StreamIntrospection streamIntrospection, StreamFlattenMetrics streamMetrics) {
        var result = new ArrayList<Pair<StreamIntrospection.ProcessorComputation, StreamIntrospection.ProcessorMetadata>>();
        for (var processor : streamIntrospection.processors()) {
            String host = processor.metadata().nodeId();
            for (var computation : processor.computations()) {
                String name = computation.name();
                if (!name.isEmpty() && !shouldExcludeByPattern(name)
                        && !shouldExcludeByInactivity(name, host, streamMetrics)) {
                    result.add(Pair.of(computation, processor.metadata()));
                }
            }
        }
        return result;
    }

    protected void write(StreamIntrospection.ProcessorComputation computation,
            StreamIntrospection.ProcessorMetadata metadata, StreamFlattenMetrics streamMetrics, D2Printer d2Printer) {
        String host = metadata.nodeId();
        var created = metadata.created();

        String name = computation.name();
        int threads = computation.threads();
        boolean continueOnFailure = computation.continueOnFailure();
        var computationMetric = streamMetrics.computationMetric(host, name);
        boolean hasFailure = computationMetric != null && computationMetric.failure() != null;
        String computationClass;
        if (hasFailure) {
            computationClass = "computation-failure";
        } else if (computationMetric == null) {
            computationClass = "computation-idle";
        } else {
            computationClass = "computation";
        }

        var content = """
                ### %s
                - created: %s
                - threads: %s
                %s
                - continue on failure: %s
                """.formatted(host, created, threads, getBatchInfo(computation), continueOnFailure);
        String computationMetrics = getComputationMetrics(host, computation, streamMetrics);
        if (!computationMetrics.isEmpty()) {
            content += computationMetrics;
        }
        d2Printer.writeMarkdownShapeWithClass(List.of("group_" + name, "comp_" + host), computationClass, content);
    }
}
