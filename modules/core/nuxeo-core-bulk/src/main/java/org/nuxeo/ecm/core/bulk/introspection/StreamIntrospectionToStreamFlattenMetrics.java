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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.lib.stream.log.Name;

/**
 * @since 2025.12
 */
public final class StreamIntrospectionToStreamFlattenMetrics
        implements Function<StreamIntrospection, StreamFlattenMetrics> {

    @Override
    public StreamFlattenMetrics apply(StreamIntrospection streamIntrospection) {
        var builder = new StreamFlattenMetricsBuilder();
        for (var metrics : streamIntrospection.metrics()) {
            builder.dateIfAfter(metrics.timestamp());
            String nodeId = metrics.nodeId();
            for (var hostMetric : metrics.metrics()) {
                String key = hostMetric.key();
                if (hostMetric.hasTag("stream")) {
                    String streamName = Name.urnOfId(hostMetric.tag("stream"));
                    String computationName = Name.urnOfId(hostMetric.tag("group"));
                    if ("nuxeo.streams.global.stream.group.end".equals(key)) {
                        // TODO give directly hostMetric?
                        builder.getOrComputeStreamMetricBuilder(streamName).end(hostMetric.valueOrCount());
                    } else if ("nuxeo.streams.global.stream.group.lag".equals(key)) {
                        builder.getOrComputeStreamComputationMetricBuilder(streamName, computationName)
                               .lag(hostMetric.valueOrCount());
                    } else if ("nuxeo.streams.global.stream.group.latency".equals(key)) {
                        builder.getOrComputeStreamComputationMetricBuilder(streamName, computationName)
                               .latency(hostMetric.valueOrCount());
                    } else if ("nuxeo.streams.global.stream.group.pos".equals(key)) {
                        builder.getOrComputeStreamComputationMetricBuilder(streamName, computationName)
                               .pos(hostMetric.valueOrCount());
                    }
                } else if (hostMetric.valueOrCount().intValue() > 0) {
                    if (key.endsWith("processRecord")
                            && hostMetric instanceof StreamIntrospection.TimerMetric timerMetric) {
                        String computationName = Name.urnOfId(hostMetric.tag("computation"));
                        builder.getOrComputeComputationMetricBuilder(nodeId, computationName).record(timerMetric);
                    } else if (key.endsWith("processTimer")
                            && hostMetric instanceof StreamIntrospection.TimerMetric timerMetric) {
                        String computationName = Name.urnOfId(hostMetric.tag("computation"));
                        builder.getOrComputeComputationMetricBuilder(nodeId, computationName).timer(timerMetric);
                    } else if (key.endsWith("computation.failure")) {
                        String computationName = Name.urnOfId(hostMetric.tag("computation"));
                        builder.getOrComputeComputationMetricBuilder(nodeId, computationName)
                               .failure(hostMetric.valueOrCount());
                    } else if (key.endsWith("stream.failure")) {
                        builder.getOrComputeNodeMetricBuilder(nodeId).failure(hostMetric.valueOrCount());
                    } else if (key.endsWith("computation.skippedRecord")) {
                        String computationName = Name.urnOfId(hostMetric.tag("computation"));
                        builder.getOrComputeComputationMetricBuilder(nodeId, computationName)
                               .skipped(hostMetric.valueOrCount());
                    }
                }
            }
        }
        return builder.build();
    }

    protected static class StreamFlattenMetricsBuilder {

        protected final Map<String, StreamMetricBuilder> streamMetrics = new HashMap<>();

        protected final Map<Pair<String, String>, ComputationMetricBuilder> computationMetrics = new HashMap<>();

        protected final Map<String, NodeMetricBuilder> nodeMetrics = new HashMap<>();

        protected Instant date = Instant.EPOCH;

        public StreamFlattenMetricsBuilder dateIfAfter(Instant date) {
            this.date = date.isAfter(this.date) ? date : this.date;
            return this;
        }

        public StreamMetricBuilder getOrComputeStreamMetricBuilder(String streamName) {
            return streamMetrics.computeIfAbsent(streamName, k -> new StreamMetricBuilder());
        }

        public StreamComputationMetricBuilder getOrComputeStreamComputationMetricBuilder(String streamName,
                String computationName) {
            return streamMetrics.computeIfAbsent(streamName, k -> new StreamMetricBuilder())
                                .getOrComputeStreamComputationMetricsBuilder(computationName);
        }

        public ComputationMetricBuilder getOrComputeComputationMetricBuilder(String nodeId, String computationName) {
            return computationMetrics.computeIfAbsent(Pair.of(nodeId, computationName),
                    k -> new ComputationMetricBuilder());
        }

        public NodeMetricBuilder getOrComputeNodeMetricBuilder(String nodeId) {
            return nodeMetrics.computeIfAbsent(nodeId, k -> new NodeMetricBuilder());
        }

        public StreamFlattenMetrics build() {
            var streamMetrics = this.streamMetrics.entrySet()
                                                  .stream()
                                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                          entry -> entry.getValue().build()));
            var computationMetrics = this.computationMetrics.entrySet()
                                                            .stream()
                                                            .collect(Collectors.toMap(Map.Entry::getKey,
                                                                    entry -> entry.getValue().build()));
            var nodeMetrics = this.nodeMetrics.entrySet()
                                              .stream()
                                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                      entry -> entry.getValue().build()));
            return new StreamFlattenMetrics(streamMetrics, computationMetrics, nodeMetrics, date);
        }
    }

    protected static class StreamMetricBuilder {

        protected final Map<String, StreamComputationMetricBuilder> computationMetrics = new HashMap<>();

        protected Number end;

        public StreamMetricBuilder end(Number end) {
            this.end = end;
            return this;
        }

        public StreamComputationMetricBuilder getOrComputeStreamComputationMetricsBuilder(String computationName) {
            return computationMetrics.computeIfAbsent(computationName, k -> new StreamComputationMetricBuilder());
        }

        public StreamFlattenMetrics.StreamMetric build() {
            var computationMetrics = this.computationMetrics.entrySet()
                                                            .stream()
                                                            .collect(Collectors.toMap(Map.Entry::getKey,
                                                                    entry -> entry.getValue().build()));
            return new StreamFlattenMetrics.StreamMetric(end, computationMetrics);
        }
    }

    protected static class StreamComputationMetricBuilder {

        protected Number lag;

        protected Number latency;

        protected Number pos;

        public StreamComputationMetricBuilder lag(Number lag) {
            this.lag = lag;
            return this;
        }

        public StreamComputationMetricBuilder latency(Number latency) {
            this.latency = latency;
            return this;
        }

        public StreamComputationMetricBuilder pos(Number pos) {
            this.pos = pos;
            return this;
        }

        public StreamFlattenMetrics.StreamComputationMetric build() {
            return new StreamFlattenMetrics.StreamComputationMetric(lag, latency, pos);
        }
    }

    protected static class ComputationMetricBuilder {

        protected StreamIntrospection.TimerMetric record;

        protected StreamIntrospection.TimerMetric timer;

        protected Number failure;

        protected Number skipped;

        public ComputationMetricBuilder record(StreamIntrospection.TimerMetric record) {
            this.record = record;
            return this;
        }

        public ComputationMetricBuilder timer(StreamIntrospection.TimerMetric timer) {
            this.timer = timer;
            return this;
        }

        public ComputationMetricBuilder failure(Number failure) {
            this.failure = failure;
            return this;
        }

        public ComputationMetricBuilder skipped(Number skipped) {
            this.skipped = skipped;
            return this;
        }

        public StreamFlattenMetrics.ComputationMetric build() {
            return new StreamFlattenMetrics.ComputationMetric(record, timer, failure, skipped);
        }
    }

    protected static class NodeMetricBuilder {

        protected Number failure;

        public NodeMetricBuilder failure(Number failure) {
            this.failure = failure;
            return this;
        }

        public StreamFlattenMetrics.NodeMetric build() {
            return new StreamFlattenMetrics.NodeMetric(failure);
        }
    }
}
