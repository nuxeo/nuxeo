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
package org.nuxeo.ecm.core.bulk.introspection;

import static java.lang.Math.max;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.lib.stream.log.Name;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 11.5
 */
public class StreamIntrospectionConverter {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // if node's metrics are older than this threshold, don't take into account this node
    protected static final long ACTIVE_THRESHOLD_SECONDS = 5 * 60L;

    // if there is less than this threshold of estimated processing with the current nodes, don't scale up
    protected static final int ETA_THRESHOLD_SECONDS = 10 * 60;

    // if there is less than this threshold of estimated processing with more nodes, don't scale up
    protected static final int BEST_ETA_THRESHOLD_SECONDS = 5 * 60;

    protected static final String EMPTY_JSON_ARRAY = "[]";

    protected final String json;

    protected final JsonNode root;

    public StreamIntrospectionConverter(String json) {
        if (StringUtils.isBlank(json)) {
            throw new IllegalArgumentException("Cannot convert blank JSON");
        }
        this.json = json;
        try {
            this.root = OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON: " + json, e);
        }
    }

    public String getStreams() {
        if (root.has("streams")) {
            return root.get("streams").toString();
        }
        return EMPTY_JSON_ARRAY;
    }

    public String getConsumers(String stream) {
        if (StringUtils.isBlank(stream)) {
            return EMPTY_JSON_ARRAY;
        }
        String match = "stream:" + stream;
        JsonNode node = root.get("processors");
        Set<String> consumers = new HashSet<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                JsonNode topologies = item.get("topology");
                if (topologies != null && topologies.isArray()) {
                    for (JsonNode topo : topologies) {
                        if (topo != null && topo.isArray() && topo.size() >= 2) {
                            String source = topo.get(0).asText();
                            if (match.equals(source)) {
                                String target = topo.get(1).asText();
                                if (target.startsWith("computation:")) {
                                    consumers.add(target.substring(12));
                                }
                            }
                        }
                    }
                }
            }
        }
        return consumers.stream()
                        .map(consumer -> "{\"stream\":\"" + stream + "\",\"consumer\":\"" + consumer + "\"}")
                        .collect(Collectors.joining(",", "[", "]"));
    }

    public String getPuml() {
        StreamIntrospectionPlantUMLConverter pumlConverter = new StreamIntrospectionPlantUMLConverter(json);
        return pumlConverter.getPuml();
    }

    public String getD2() {
        StreamIntrospectionD2Converter d2Converter = new StreamIntrospectionD2Converter(json);
        return d2Converter.getD2();
    }

    /**
     * Gets the D2 diagram format with filtering options.
     *
     * @param excludePatterns list of patterns to exclude (e.g., Arrays.asList("work/", "bulk/"))
     * @param excludeInactive whether to exclude inactive computations (computation-idle class) and empty streams
     *            (stream-empty class)
     * @return the D2 diagram as a string
     */
    public String getD2(List<String> excludePatterns, boolean excludeInactive) {
        StreamIntrospectionD2Converter d2Converter = new StreamIntrospectionD2Converter(json);
        return d2Converter.getD2(excludePatterns, excludeInactive);
    }

    public String getActivity() {
        return getActivity(System.currentTimeMillis() / 1000);
    }

    public String getActivity(long atTimestamp) {
        ObjectNode ret = OBJECT_MAPPER.createObjectNode();
        JsonNode nodes = getClusterNodes(atTimestamp);
        int workerCount = 0;
        for (JsonNode node : nodes) {
            if ("worker".equals(node.at("/type").asText())) {
                workerCount++;
            }
        }
        JsonNode computations = getActiveComputations(atTimestamp);
        JsonNode scale = getScaleMetrics(workerCount, (ArrayNode) computations);
        ret.set("scale", scale);
        ret.set("nodes", nodes);
        ret.set("computations", computations);
        return ret.toString();
    }

    protected JsonNode getScaleMetrics(int workerCount, ArrayNode computations) {
        // Worker nodes detected in the cluster
        int currentNodes = workerCount > 0 ? workerCount : -1;
        // Best number of nodes to get the max throughput
        int bestNodes = workerCount > 0 ? 1 : -1;
        // Relevant best number of nodes
        int optimalNodes = bestNodes;
        for (JsonNode computation : computations) {
            String compName = computation.at("/computation").asText();
            if ("bulk-scroller".equals(compName) || "bulk-status".equals(compName)) {
                // don't take into account computations that can run on front nodes
                continue;
            }
            currentNodes = max(currentNodes, computation.at("/current/nodes").asInt());
            int bn = computation.at("/best/nodes").asInt();
            bestNodes = max(bestNodes, bn);
            if (computation.at("/best/relevant").asBoolean()) {
                optimalNodes = max(optimalNodes, bn);
            }
        }
        ObjectNode ret = OBJECT_MAPPER.createObjectNode();
        ret.put("currentNodes", currentNodes);
        ret.put("bestNodes", bestNodes);
        ret.put("optimalNodes", optimalNodes);
        ret.put("metric", optimalNodes - currentNodes);
        return ret;
    }

    protected JsonNode getActiveComputations(long atTimestamp) {
        ArrayNode ret = OBJECT_MAPPER.createArrayNode();
        Map<String, ObjectNode> computations = new HashMap<>();
        JsonNode metrics = root.get("metrics");
        if (metrics == null || !metrics.isArray() || metrics.isEmpty()) {
            // no data available ?
            return ret;
        }
        JsonNode processors = root.get("processors");
        if (processors == null || !processors.isArray() || processors.isEmpty()) {
            // no data available
            return ret;
        }
        // create a map of stream/partitions
        Map<String, Integer> partitions = new HashMap<>();
        JsonNode streams = root.get("streams");
        if (streams != null && streams.isArray()) {
            for (JsonNode stream : streams) {
                if (stream != null && stream.has("name") && stream.has("partitions")) {
                    partitions.put(Name.ofUrn(stream.get("name").asText()).getId(), stream.get("partitions").asInt());
                }
            }
        }
        // create a map of computation/threads
        Map<String, Integer> threads = new HashMap<>();
        for (JsonNode item : processors) {
            JsonNode comps = item.get("computations");
            if (comps != null && comps.isArray()) {
                for (JsonNode comp : comps) {
                    if (comp != null && comp.has("name") && comp.has("threads")) {
                        String name = Name.ofUrn(comp.get("name").asText()).getId();
                        threads.put(name, comp.get("threads").asInt());
                    }
                }
            }
        }
        // find active computations
        for (JsonNode node : metrics) {
            long ts = node.get("timestamp").asLong();
            if (atTimestamp - ts > ACTIVE_THRESHOLD_SECONDS) {
                continue;
            }
            // select computations with a significant rate
            JsonNode metricsArray = node.get("metrics");
            if (metricsArray != null && metricsArray.isArray()) {
                for (JsonNode metric : metricsArray) {
                    if (metric != null && metric.has("k") && metric.has("computation") && metric.has("count")
                            && "nuxeo.streams.computation.processRecord".equals(metric.get("k").asText())
                            && (metric.get("count").asInt() > 0)) {
                        // ex: { "k": "nuxeo.streams.computation.processRecord",
                        // "computation": "audit-writer", "count": 32, "rate1m": 0.07875646231106845, "mean": ...}
                        String computationName = metric.get("computation").asText();
                        boolean knownComputation = threads.containsKey(computationName);
                        ObjectNode comp = computations.get(computationName);
                        if (knownComputation && comp == null && metric.has("mean")
                                && metric.get("mean").asDouble() > 0) {
                            if (metric.has("rate1m")) {
                                double rate1m = metric.get("rate1m").asDouble();
                                double mean = metric.get("mean").asDouble();
                                double maxRateByThread = 1 / mean;
                                // assume a rate is significant if one thread is busy at 50%
                                if (rate1m > maxRateByThread / 2) {
                                    computations.put(computationName, initComputation(computationName));
                                }
                            }
                        }
                    }
                }
            }
            // select computation with lag, populate lag
            if (metricsArray != null && metricsArray.isArray()) {
                for (JsonNode metric : metricsArray) {
                    if (metric != null && metric.has("k") && metric.has("group") && metric.has("stream")
                            && metric.has("v")
                            && "nuxeo.streams.global.stream.group.lag".equals(metric.get("k").asText())) {
                        // ex: { "k": "nuxeo.streams.global.stream.group.lag",
                        // "group": "bulk-csvExport", "stream": "bulk-csvExport", "v": 46 }
                        String groupName = metric.get("group").asText();
                        boolean knownComputation = threads.containsKey(groupName);
                        ObjectNode comp = computations.get(groupName);
                        if (knownComputation && (comp != null || metric.get("v").asInt() > 0)) {
                            comp = computations.computeIfAbsent(groupName, this::initComputation);
                            // populate computation lag for its streams
                            ObjectNode compStreams = (ObjectNode) comp.get("streams");
                            ObjectNode stream = OBJECT_MAPPER.createObjectNode();
                            stream.set("stream", metric.get("stream"));
                            String streamName = metric.get("stream").asText();
                            stream.put("partitions", partitions.getOrDefault(streamName, 0));
                            stream.set("lag", metric.get("v"));
                            compStreams.set(streamName, stream);
                        }
                    }
                }
            }
        }
        // populate latency, end
        for (JsonNode node : metrics) {
            long ts = node.get("timestamp").asLong();
            if (atTimestamp - ts > ACTIVE_THRESHOLD_SECONDS) {
                continue;
            }
            JsonNode latencyMetricsArray = node.get("metrics");
            if (latencyMetricsArray != null && latencyMetricsArray.isArray()) {
                for (JsonNode metric : latencyMetricsArray) {
                    if (metric != null && metric.has("k") && metric.has("group") && metric.has("stream")) {
                        if ("nuxeo.streams.global.stream.group.latency".equals(metric.get("k").asText())
                                && metric.has("v") && (metric.get("v").asInt() > 0)) {
                            // ex {"k": "nuxeo.streams.global.stream.group.latency",
                            // "group": "bulk-exposeBlob", "stream": "bulk-exposeBlob", "v": 123}
                            String groupName = metric.get("group").asText();
                            ObjectNode comp = computations.get(groupName);
                            if (comp != null) {
                                JsonNode streamsNode = comp.get("streams");
                                if (streamsNode != null) {
                                    String streamName = metric.get("stream").asText();
                                    ObjectNode stream = (ObjectNode) streamsNode.get(streamName);
                                    if (stream != null) {
                                        stream.set("latency", metric.get("v"));
                                    }
                                }
                            }
                        } else if ("nuxeo.streams.global.stream.group.end".equals(metric.get("k").asText())
                                && metric.has("v")) {
                            String groupName = metric.get("group").asText();
                            ObjectNode comp = computations.get(groupName);
                            // ex {"k": "nuxeo.streams.global.stream.group.end",
                            // "group": "StreamImporter-runDocumentConsumers", "stream": "import-doc", "v": 10000}
                            if (comp != null) {
                                JsonNode streamsNode = comp.get("streams");
                                if (streamsNode != null) {
                                    String streamName = metric.get("stream").asText();
                                    ObjectNode stream = (ObjectNode) streamsNode.get(streamName);
                                    if (stream != null) {
                                        stream.set("end", metric.get("v"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // populate metrics per node for active computations
        for (JsonNode node : metrics) {
            JsonNode ts = node.get("timestamp");
            if (atTimestamp - ts.asLong() > ACTIVE_THRESHOLD_SECONDS) {
                continue;
            }
            JsonNode nodeId = node.get("nodeId");
            JsonNode nodeMetricsArray = node.get("metrics");
            if (nodeMetricsArray != null && nodeMetricsArray.isArray()) {
                for (JsonNode metric : nodeMetricsArray) {
                    if (metric != null && metric.has("k") && metric.has("computation") && metric.has("count")
                            && "nuxeo.streams.computation.processRecord".equals(metric.get("k").asText())
                            && (metric.get("count").asInt() > 0)) {
                        String computationName = metric.get("computation").asText();
                        ObjectNode comp = computations.get(computationName);
                        if (comp != null) {
                            ObjectNode compInstance = OBJECT_MAPPER.createObjectNode();
                            compInstance.set("nodeId", nodeId);
                            compInstance.put("threads", threads.getOrDefault(computationName, 1));
                            compInstance.set("timestamp", ts);
                            compInstance.set("count", metric.get("count"));
                            if (metric.has("sum"))
                                compInstance.set("sum", metric.get("sum"));
                            if (metric.has("rate1m"))
                                compInstance.set("rate1m", metric.get("rate1m"));
                            if (metric.has("rate5m"))
                                compInstance.set("rate5m", metric.get("rate5m"));
                            if (metric.has("min"))
                                compInstance.set("min", metric.get("min"));
                            if (metric.has("p50"))
                                compInstance.set("p50", metric.get("p50"));
                            if (metric.has("mean"))
                                compInstance.set("mean", metric.get("mean"));
                            if (metric.has("p95"))
                                compInstance.set("p95", metric.get("p95"));
                            if (metric.has("max"))
                                compInstance.set("max", metric.get("max"));
                            if (metric.has("stddev"))
                                compInstance.set("stddev", metric.get("stddev"));
                            JsonNode nodesArray = comp.get("nodes");
                            if (nodesArray instanceof ArrayNode) {
                                ((ArrayNode) nodesArray).add(compInstance);
                            }
                        }
                    }
                }
            }
        }
        // create cluster metrics
        for (ObjectNode comp : computations.values()) {
            int count = 0;
            int threadsCount = 0;
            float rate1m = 0;
            int threadsPerNode = 0;
            for (JsonNode node : comp.get("nodes")) {
                count += 1;
                threadsPerNode = node.get("threads").asInt();
                threadsCount += threadsPerNode;
                rate1m += (float) node.get("rate1m").asDouble();
            }
            int lag = 0;
            int part = 0;
            JsonNode streamsNode = comp.get("streams");
            if (streamsNode != null) {
                for (Iterator<JsonNode> iter = streamsNode.elements(); iter.hasNext();) {
                    JsonNode stream = iter.next();
                    if (stream != null && stream.has("lag") && stream.has("stream")) {
                        int streamLag = stream.get("lag").asInt();
                        if (streamLag > lag) {
                            lag = streamLag;
                            String streamName = stream.get("stream").asText();
                            part = partitions.getOrDefault(streamName, 0);
                        }
                    }
                }
            }
            if (count == 0) {
                continue;
            }
            int eta = rate1m > 0 ? (int) (lag / rate1m) : Integer.MAX_VALUE;
            ObjectNode current = OBJECT_MAPPER.createObjectNode();
            current.put("nodes", count);
            current.put("threads", threadsCount);
            current.put("rate1m", rate1m);
            current.put("eta", eta);
            ObjectNode best = OBJECT_MAPPER.createObjectNode();
            if (lag == 0) {
                // active computation that copes with the load, stay conservative best = current
                best.set("nodes", current.get("nodes"));
                best.set("threads", current.get("threads"));
                best.set("rate1m", current.get("rate1m"));
                best.set("eta", current.get("eta"));
                best.put("relevant", true);
            } else {
                // active computation with lag, best nb of nodes depends on stream partitions
                int bestNodes = threadsPerNode > 0 ? (int) Math.ceil((double) part / (double) threadsPerNode) : 1;
                float bestRate = threadsCount > 0 ? rate1m * part / threadsCount : 0;
                int bestEta = bestRate > 0 ? (int) (lag / bestRate) : Integer.MAX_VALUE;
                best.put("nodes", bestNodes);
                best.put("threads", part);
                best.put("rate1m", bestRate);
                best.put("eta", bestEta);
                // best is not relevant to scale out when there is not enough estimated processing
                best.put("relevant",
                        bestNodes <= count || (eta >= ETA_THRESHOLD_SECONDS && bestEta >= BEST_ETA_THRESHOLD_SECONDS));
            }
            comp.set("current", current);
            comp.set("best", best);
        }
        computations.values().forEach(ret::add);
        return ret;
    }

    protected ObjectNode initComputation(String key) {
        var active = OBJECT_MAPPER.createObjectNode();
        active.put("computation", key);
        active.set("streams", OBJECT_MAPPER.createObjectNode());
        active.set("nodes", OBJECT_MAPPER.createArrayNode());
        return active;
    }

    protected JsonNode getClusterNodes(long atTimestamp) {
        Map<String, ObjectNode> nodes = new HashMap<>();
        ArrayNode ret = OBJECT_MAPPER.createArrayNode();
        JsonNode processors = root.get("processors");
        if (processors != null && processors.isArray()) {
            for (JsonNode item : processors) {
                ObjectNode node = item.at("/metadata").deepCopy();
                node.remove("processorName");
                node.put("created", Instant.ofEpochSecond(node.at("/created").asLong()).toString());
                nodes.put(item.at("/metadata/nodeId").asText(), node);
            }
        }
        JsonNode metrics = root.get("metrics");
        Set<String> activeNodes = new HashSet<>();
        if (metrics != null && metrics.isArray()) {
            for (JsonNode item : metrics) {
                ObjectNode node = nodes.get(item.at("/nodeId").asText());
                if (node == null) {
                    continue;
                }
                long timestamp = item.at("/timestamp").asLong();
                if (atTimestamp - timestamp <= ACTIVE_THRESHOLD_SECONDS) {
                    activeNodes.add(item.at("/nodeId").asText());
                }
                node.put("alive", Instant.ofEpochSecond(timestamp).toString());
                JsonNode hostMetrics = item.get("metrics");
                if (hostMetrics.isArray()) {
                    String nodeType = "front";
                    for (JsonNode it : hostMetrics) {
                        if ("nuxeo.streams.computation.running".equals(it.get("k").asText())) {
                            if ("work-common".equals(it.get("computation").asText())) {
                                // assume that a node with a work/common computation is a worker node
                                nodeType = "worker";
                                break;
                            }
                        }
                    }
                    node.put("type", nodeType);
                }
            }
        }
        nodes.forEach((nodeId, node) -> {
            if (activeNodes.contains(nodeId)) {
                ret.add(node);
            }
        });
        return ret;
    }

}
