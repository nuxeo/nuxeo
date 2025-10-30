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

import static java.lang.Math.max;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.function.BinaryOperator.minBy;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.nuxeo.common.stream.MapMultis;
import org.nuxeo.lib.stream.log.Name;

/**
 * @since 2025.12
 */
public final class StreamIntrospectionToScaleActivity implements Function<StreamIntrospection, ScaleActivity> {

    // if node's metrics are older than this threshold, don't take into account this node
    protected static final Duration ACTIVE_THRESHOLD = Duration.ofMinutes(5);

    // if there is less than this threshold of estimated processing with the current nodes, don't scale up
    protected static final Duration ETA_THRESHOLD = Duration.ofMinutes(10);

    // if there is less than this threshold of estimated processing with more nodes, don't scale up
    protected static final Duration BEST_ETA_THRESHOLD = Duration.ofMinutes(5);

    protected final Instant activeInstant;

    public StreamIntrospectionToScaleActivity() {
        this(Instant.now());
    }

    public StreamIntrospectionToScaleActivity(Instant atInstant) {
        this.activeInstant = atInstant.minus(ACTIVE_THRESHOLD);
    }

    @Override
    public ScaleActivity apply(StreamIntrospection streamIntrospection) {
        var clusterNodes = toClusterNodes(streamIntrospection);
        int workerCount = (int) clusterNodes.stream().filter(node -> "worker".equals(node.type())).count();
        var activeComputations = toActiveComputation(streamIntrospection);
        var scale = getScaleMetrics(workerCount, activeComputations);
        return new ScaleActivity(scale, clusterNodes, activeComputations);
    }

    protected ScaleActivity.Scale getScaleMetrics(int workerCount, List<ScaleActivity.ActiveComputation> computations) {
        // Worker nodes detected in the cluster
        int currentNodes = workerCount > 0 ? workerCount : -1;
        // Best number of nodes to get the max throughput
        int bestNodes = workerCount > 0 ? 1 : -1;
        // Relevant best number of nodes
        int optimalNodes = bestNodes;
        for (var computation : computations) {
            String compName = computation.computation();
            if ("bulk-scroller".equals(compName) || "bulk-status".equals(compName)) {
                // don't take into account computations that can run on front nodes
                continue;
            }
            var currentComputationRecommendation = computation.current();
            if (currentComputationRecommendation != null) {
                currentNodes = max(currentNodes, currentComputationRecommendation.nodes());
            }
            var bestComputationRecommendation = computation.best();
            if (bestComputationRecommendation != null) {
                int bn = bestComputationRecommendation.nodes();
                bestNodes = max(bestNodes, bn);
                if (bestComputationRecommendation.relevant()) {
                    optimalNodes = max(optimalNodes, bn);
                }
            }
        }
        return new ScaleActivity.Scale(currentNodes, bestNodes, optimalNodes, optimalNodes - currentNodes);
    }

    protected List<ScaleActivity.ClusterNode> toClusterNodes(StreamIntrospection streamIntrospection) {
        var processorsMetadata = streamIntrospection.processors()
                                                    .stream()
                                                    .map(StreamIntrospection.Processor::metadata)
                                                    .collect(groupingBy(StreamIntrospection.ProcessorMetadata::nodeId,
                                                            reducing(null, minBy(nullsLast(comparing(
                                                                    StreamIntrospection.ProcessorMetadata::created))))));
        return streamIntrospection.metrics()
                                  .stream()
                                  .filter(metrics -> processorsMetadata.containsKey(metrics.nodeId()))
                                  .filter(metrics -> metrics.timestamp().isAfter(activeInstant))
                                  .collect(
                                          collectingAndThen(
                                                  groupingBy(StreamIntrospection.Metrics::nodeId,
                                                          reducing(maxBy(
                                                                  comparing(StreamIntrospection.Metrics::timestamp)))),
                                                  Map::values))
                                  .stream()
                                  .flatMap(Optional::stream)
                                  .map(metrics -> toClusterNode(processorsMetadata.get(metrics.nodeId()), metrics))
                                  .toList();
    }

    protected ScaleActivity.ClusterNode toClusterNode(StreamIntrospection.ProcessorMetadata processorMetadata,
            StreamIntrospection.Metrics metrics) {
        String nodeId = metrics.nodeId();
        var nodeType = metrics.metrics()
                              .stream()
                              .filter(metric -> "nuxeo.streams.computation.running".equals(metric.key()))
                              // assume that a node with a work/common computation is a worker node
                              .anyMatch(metric -> "work-common".equals(metric.tag("computation"))) ? "worker" : "front";
        return new ScaleActivity.ClusterNode(processorMetadata.hostname(), nodeId, nodeType, processorMetadata.ip(),
                processorMetadata.cpuCores(), processorMetadata.jvmHeapSize(), processorMetadata.created(),
                metrics.timestamp());
    }

    public List<ScaleActivity.ActiveComputation> toActiveComputation(StreamIntrospection streamIntrospection) {
        if (streamIntrospection.metrics().isEmpty()) {
            // no data available ?
            return List.of();
        }
        if (streamIntrospection.processors().isEmpty()) {
            // no data available
            return List.of();
        }
        // create a map of stream/partitions
        var partitions = streamIntrospection.streams()
                                            .stream()
                                            .collect(toMap(stream -> Name.ofUrn(stream.name()).getId(),
                                                    StreamIntrospection.Stream::partitions, (p1, p2) -> p1));
        // create a map of computation/threads
        var threads = streamIntrospection.processors()
                                         .stream()
                                         .mapMulti(MapMultis.each(StreamIntrospection.Processor::computations))
                                         .collect(toMap(computation -> Name.ofUrn(computation.name()).getId(),
                                                 StreamIntrospection.ProcessorComputation::threads, (t1, t2) -> t1));
        var computationBuilder = new ActiveComputationBuilder();
        for (var metrics : streamIntrospection.metrics()) {
            if (metrics.timestamp().isBefore(activeInstant)) {
                continue;
            }
            // select computations with a significant rate
            for (var metric : metrics.metrics()) {
                if ("nuxeo.streams.computation.processRecord".equals(metric.key())
                        && metric.valueOrCount().intValue() > 0) {
                    // ex: { "k": "nuxeo.streams.computation.processRecord",
                    // "computation": "audit-writer", "count": 32, "rate1m": 0.07875646231106845, "mean": ...}
                    String computationName = metric.tag("computation");
                    boolean knownComputation = threads.containsKey(computationName);
                    if (knownComputation && !computationBuilder.hasComputation(computationName)
                            && metric instanceof StreamIntrospection.TimerMetric timerMetric
                            && timerMetric.mean() > 0) {
                        double maxRateByThread = 1 / timerMetric.mean();
                        // assume a rate is significant if one thread is busy at 50%
                        if (timerMetric.rate1m() > maxRateByThread / 2) {
                            computationBuilder.initComputation(computationName);
                        }
                    }

                }
            }
            // select computation with lag, populate lag
            for (var metric : metrics.metrics()) {
                if ("nuxeo.streams.global.stream.group.lag".equals(metric.key())) {
                    // ex: { "k": "nuxeo.streams.global.stream.group.lag",
                    // "group": "bulk-csvExport", "stream": "bulk-csvExport", "v": 46 }
                    String computationName = metric.tag("group");
                    boolean knownComputation = threads.containsKey(computationName);
                    if (knownComputation && (computationBuilder.hasComputation(computationName)
                            || metric.valueOrCount().intValue() > 0)) {
                        computationBuilder.initComputation(computationName);
                        // populate computation lag for its streams
                        String streamName = metric.tag("stream");
                        computationBuilder.addStream(computationName, streamName, partitions.get(streamName),
                                metric.valueOrCount().intValue());
                    }
                }
            }
        }
        // populate latency, end
        for (var metrics : streamIntrospection.metrics()) {
            if (metrics.timestamp().isBefore(activeInstant)) {
                continue;
            }
            for (var metric : metrics.metrics()) {
                if ("nuxeo.streams.global.stream.group.latency".equals(metric.key())
                        && metric instanceof StreamIntrospection.ValueMetric valueMetric) {
                    // ex {"k": "nuxeo.streams.global.stream.group.latency",
                    // "group": "bulk-exposeBlob", "stream": "bulk-exposeBlob", "v": 123}
                    computationBuilder.populateStreamLatency(valueMetric);
                } else if ("nuxeo.streams.global.stream.group.end".equals(metric.key())
                        && metric instanceof StreamIntrospection.ValueMetric valueMetric) {
                    // ex {"k": "nuxeo.streams.global.stream.group.end",
                    // "group": "StreamImporter-runDocumentConsumers", "stream": "import-doc", "v": 10000}
                    computationBuilder.populateStreamEnd(valueMetric);
                }
            }
        }
        // populate metrics per node for active computations
        for (var metrics : streamIntrospection.metrics()) {
            if (metrics.timestamp().isBefore(activeInstant)) {
                continue;
            }
            for (var metric : metrics.metrics()) {
                if ("nuxeo.streams.computation.processRecord".equals(metric.key())
                        && metric instanceof StreamIntrospection.TimerMetric timerMetric && timerMetric.count() > 0) {
                    String computationName = metric.tag("computation");
                    computationBuilder.addNode(metrics.nodeId(), threads.get(computationName), metrics.timestamp(),
                            timerMetric);
                }
            }
        }
        return computationBuilder.build();
    }

    protected static class ActiveComputationBuilder {

        protected final Set<String> computations = new LinkedHashSet<>();

        protected final Map<String, Map<String, ActiveComputationStreamBuilder>> streams = new HashMap<>();

        protected final Map<String, List<ScaleActivity.ActiveComputationNode>> nodes = new HashMap<>();

        public ActiveComputationBuilder initComputation(String computation) {
            computations.add(computation);
            streams.computeIfAbsent(computation, k -> new LinkedHashMap<>());
            nodes.computeIfAbsent(computation, k -> new ArrayList<>());
            return this;
        }

        public ActiveComputationBuilder addStream(String computation, String stream, Integer partitions, long lag) {
            streams.get(computation).put(stream, new ActiveComputationStreamBuilder(stream, partitions, lag));
            return this;
        }

        public ActiveComputationBuilder populateStreamLatency(StreamIntrospection.ValueMetric metric) {
            return populateStream(metric,
                    computationStream -> computationStream.latency(Duration.ofMillis(metric.value().longValue())));
        }

        public ActiveComputationBuilder populateStreamEnd(StreamIntrospection.ValueMetric metric) {
            return populateStream(metric, computationStream -> computationStream.end(metric.value().longValue()));
        }

        protected ActiveComputationBuilder populateStream(StreamIntrospection.ValueMetric metric,
                Consumer<ActiveComputationStreamBuilder> consumer) {
            String computationName = metric.tag("group");
            if (computations.contains(computationName)) {
                Map<String, ActiveComputationStreamBuilder> computationStream = streams.get(computationName);
                String streamName = metric.tag("stream");
                if (computationStream.containsKey(streamName)) {
                    consumer.accept(computationStream.get(streamName));
                }
            }
            return this;
        }

        public ActiveComputationBuilder addNode(String nodeId, int threads, Instant timestamp,
                StreamIntrospection.TimerMetric timerMetric) {
            String computationName = timerMetric.tag("computation");
            if (computations.contains(computationName)) {
                nodes.get(computationName)
                     .add(new ScaleActivity.ActiveComputationNode(nodeId, threads, timestamp, timerMetric.count(),
                             timerMetric.sum(), timerMetric.rate1m(), timerMetric.rate5m(), timerMetric.min(),
                             timerMetric.p50(), timerMetric.mean(), timerMetric.p95(), timerMetric.max(),
                             timerMetric.stddev()));
            }
            return this;
        }

        public boolean hasComputation(String computation) {
            return computations.contains(computation);
        }

        public List<ScaleActivity.ActiveComputation> build() {
            // compute cluster metrics
            return computations.stream().map(computationName -> {
                int count = 0;
                int threadsCount = 0;
                float rate1m = 0;
                int threadsPerNode = 0;
                var computationNodes = this.nodes.get(computationName);
                for (var node : computationNodes) {
                    count += 1;
                    threadsPerNode = node.threads();
                    threadsCount += threadsPerNode;
                    rate1m += (float) node.rate1m();
                }
                long lag = 0;
                int part = 0;
                for (var computationStream : streams.get(computationName).values()) {
                    var streamLag = computationStream.lag;
                    if (streamLag > lag) {
                        lag = streamLag;
                        part = computationStream.partitions;
                    }
                }
                var computationStreams = streams.get(computationName)
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
                if (count == 0) {
                    return new ScaleActivity.ActiveComputation(computationName, computationStreams, computationNodes);
                }
                int eta = (int) (lag / rate1m);
                var current = new ScaleActivity.ActiveComputationRecommendation(count, threadsCount, rate1m, eta, true);
                ScaleActivity.ActiveComputationRecommendation best;
                if (lag == 0) {
                    // active computation that copes with the load, stay conservative best = current
                    best = current;
                } else {
                    // active computation with lag, best nb of nodes depends on stream partitions
                    int bestNodes = (int) Math.ceil((double) part / (double) threadsPerNode);
                    float bestRate = rate1m * part / threadsCount;
                    int bestEta = (int) (lag / bestRate);
                    best = new ScaleActivity.ActiveComputationRecommendation(bestNodes, part, bestRate, bestEta,
                            // best is not relevant to scale out when there is not enough estimated processing
                            bestNodes <= count
                                    || (eta >= ETA_THRESHOLD.toSeconds() && bestEta >= BEST_ETA_THRESHOLD.toSeconds()));
                }
                return new ScaleActivity.ActiveComputation(computationName, computationStreams, computationNodes,
                        current, best);
            }).toList();
        }
    }

    protected static class ActiveComputationStreamBuilder {

        protected final String stream;

        protected final int partitions;

        protected final long lag;

        protected Long end;

        protected Duration latency;

        public ActiveComputationStreamBuilder(String stream, int partitions, long lag) {
            this.stream = stream;
            this.partitions = partitions;
            this.lag = lag;
        }

        public ActiveComputationStreamBuilder end(Long end) {
            this.end = end;
            return this;
        }

        public ActiveComputationStreamBuilder latency(Duration latency) {
            this.latency = latency;
            return this;
        }

        public ScaleActivity.ActiveComputationStream build() {
            return new ScaleActivity.ActiveComputationStream(stream, partitions, lag, end, latency);
        }
    }
}
