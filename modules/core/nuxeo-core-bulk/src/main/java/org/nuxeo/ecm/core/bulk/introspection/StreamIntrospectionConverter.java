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

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
                for (JsonNode topo : topologies) {
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
        return consumers.stream()
                        .map(consumer -> "{\"stream\":\"" + stream + "\",\"consumer\":\"" + consumer + "\"}")
                        .collect(Collectors.joining(",", "[", "]"));
    }

    public String getPuml() {
        StringBuilder ret = new StringBuilder();
        ret.append("@startuml\n");
        Map<String, String> streamMetrics = parseMetrics();
        ret.append(getPumlHeader("Stream Introspection at " + streamMetrics.get("date")));
        JsonNode node = root.get("streams");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                dumpStream(ret, item, streamMetrics);
            }
        }

        node = root.get("processors");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String host = item.at("/metadata/nodeId").asText();
                String created = Instant.ofEpochSecond(item.at("/metadata/created").asLong()).toString();
                // ret.append("rectangle node." + host + " {\n");
                JsonNode computations = item.get("computations");
                if (computations.isArray()) {
                    for (JsonNode computation : computations) {
                        dumpComputation(host, ret, computation, streamMetrics, created);
                    }
                }
                // ret.append("}\n");
                JsonNode topologies = item.get("topology");
                if (topologies.isArray()) {
                    for (JsonNode topo : topologies) {
                        String comment = "";
                        String source = topo.get(0).asText();
                        String target = topo.get(1).asText();
                        if (target.startsWith("computation:")) {
                            String stream = source.replace("stream:", "");
                            String computation = target.replace("computation:", "");
                            String lag = streamMetrics.get(stream + ":" + computation + ":lag");
                            String latency = streamMetrics.get(stream + ":" + computation + ":latency");
                            String pos = streamMetrics.get(stream + ":" + computation + ":pos");
                            String end = getStreamEnd(streamMetrics, stream);
                            // provide info only when there is a lag
                            if (lag != null && !"0".equals(lag)) {
                                comment = String.format(": %s/%s lag: %s, latency: %ss", pos, end, lag, latency);
                            }
                        }
                        ret.append(String.format("%s==>%s%s%n", getPumlIdentifierForHost(host, source),
                                getPumlIdentifierForHost(host, target), comment));
                    }
                }

            }
        }

        ret.append("@enduml\n");
        return ret.toString();
    }

    protected Map<String, String> parseMetrics() {
        Map<String, String> streamMetrics = new HashMap<>();
        JsonNode node = root.get("metrics");
        long timestamp = 0;
        if (node != null && node.isArray()) {
            for (JsonNode host : node) {
                String nodeId = host.get("nodeId").asText();
                long metricTimestamp = host.get("timestamp").asLong();
                if (metricTimestamp > timestamp) {
                    timestamp = metricTimestamp;
                }
                JsonNode hostMetrics = host.get("metrics");
                if (hostMetrics.isArray()) {
                    for (JsonNode metric : hostMetrics) {
                        if (metric.has("stream")) {
                            String key = metric.get("k").asText();
                            String streamName = Name.urnOfId(metric.get("stream").asText());
                            String computationName = Name.urnOfId(metric.get("group").asText());
                            if ("nuxeo.streams.global.stream.group.end".equals(key)) {
                                streamMetrics.put(streamName + ":end", metric.get("v").asText());
                            } else if ("nuxeo.streams.global.stream.group.lag".equals(key)) {
                                streamMetrics.put(streamName + ":" + computationName + ":lag",
                                        metric.get("v").asText());
                            } else if ("nuxeo.streams.global.stream.group.latency".equals(key)) {
                                streamMetrics.put(streamName + ":" + computationName + ":latency",
                                        getNiceDouble(metric.get("v").asDouble() / 1000.0));
                            } else if ("nuxeo.streams.global.stream.group.pos".equals(key)) {
                                streamMetrics.put(streamName + ":" + computationName + ":pos",
                                        metric.get("v").asText());
                            }
                        } else if (metric.get("k").asText().endsWith("processRecord")) {
                            int count = metric.get("count").asInt();
                            if (count == 0) {
                                continue;
                            }
                            String computationName = Name.urnOfId(metric.get("computation").asText());
                            streamMetrics.put(computationName + ":" + nodeId + ":count", metric.get("count").asText());
                            streamMetrics.put(computationName + ":" + nodeId + ":sum",
                                    getNiceDouble3(metric.get("sum").asDouble() / 1000000000));
                            streamMetrics.put(computationName + ":" + nodeId + ":p50",
                                    getNiceDouble3(metric.get("p50").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":mean",
                                    getNiceDouble3(metric.get("mean").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":p99",
                                    getNiceDouble3(metric.get("p99").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":rate1m",
                                    getNiceDouble(metric.get("rate1m").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":rate5m",
                                    getNiceDouble(metric.get("rate5m").asDouble()));
                        } else if (metric.get("k").asText().endsWith("processTimer")) {
                            int count = metric.get("count").asInt();
                            if (count == 0) {
                                continue;
                            }
                            String computationName = Name.urnOfId(metric.get("computation").asText());
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:count",
                                    metric.get("count").asText());
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:sum",
                                    getNiceDouble3(metric.get("sum").asDouble() / 1000000000));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:p50",
                                    getNiceDouble3(metric.get("p50").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:mean",
                                    getNiceDouble3(metric.get("mean").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:p99",
                                    getNiceDouble3(metric.get("p99").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:rate1m",
                                    getNiceDouble(metric.get("rate1m").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:rate5m",
                                    getNiceDouble(metric.get("rate5m").asDouble()));
                        } else if (metric.get("k").asText().endsWith("computation.failure")) {
                            int failure = metric.get("v").asInt();
                            if (failure > 0) {
                                String computationName = Name.urnOfId(metric.get("computation").asText()) + ":"
                                        + nodeId;
                                streamMetrics.put(computationName + ":failure", metric.get("v").asText());
                            }
                        } else if (metric.get("k").asText().endsWith("stream.failure")) {
                            int value = metric.get("v").asInt();
                            if (value > 0) {
                                streamMetrics.put(nodeId + ":failure", metric.get("v").asText());
                            }
                        } else if (metric.get("k").asText().endsWith("computation.skippedRecord")) {
                            int value = metric.get("v").asInt();
                            if (value > 0) {
                                String computationName = Name.urnOfId(metric.get("computation").asText()) + ":"
                                        + nodeId;
                                streamMetrics.put(computationName + ":skipped", metric.get("v").asText());
                            }
                        }
                    }
                }
            }
        }
        streamMetrics.put("timestamp", String.valueOf(timestamp));
        streamMetrics.put("date", Instant.ofEpochSecond(timestamp).toString());
        return streamMetrics;
    }

    protected String getNiceDouble(Double number) {
        return String.format("%.2f", number);
    }

    protected String getNiceDouble3(Double number) {
        return String.format("%.3f", number);
    }

    protected String getPumlHeader(String title) {
        return "title " + title + "\n\n" //
                + "skinparam defaultFontName Courier\n" + "skinparam handwritten false\n" //
                + "skinparam queueBackgroundColor LightYellow\n" //
                + "skinparam nodeBackgroundColor Azure\n" //
                + "skinparam componentBackgroundColor Azure\n" //
                + "skinparam nodebackgroundColor<<failure>> Yellow\n" //
                + "skinparam componentbackgroundColor<<failure>> Yellow\n" //
                + "skinparam component {\n" + "  BorderColor black\n" + "  ArrowColor #CC6655\n" + "}\n";
    }

    protected String getPumlIdentifierForHost(String host, String id) {
        if (id.startsWith("computation:")) {
            return getPumlIdentifier(id + ":" + host);
        }
        return getPumlIdentifier(id);
    }

    protected void dumpStream(StringBuilder ret, JsonNode item, Map<String, String> metrics) {
        String name = item.get("name").asText();
        String partitions = item.get("partitions").asText();
        String codec = item.get("codec").asText();
        ret.append(String.format("queue %s [%s%n----%npartitions: %s%ncodec: %s%n-----%nrecords: %s]%n",
                getPumlIdentifier("stream:" + name), name, partitions, codec, getStreamEnd(metrics, name)));
    }

    protected String getStreamEnd(Map<String, String> metrics, String name) {
        String ret = metrics.get(name + ":end");
        return ret == null ? "0" : ret;
    }

    protected void dumpComputation(String host, StringBuilder ret, JsonNode item, Map<String, String> metrics,
            String created) {
        String name = item.get("name").asText();
        String threads = item.get("threads").asText();
        String continueOnFailure = item.get("continueOnFailure").asText();
        String failure = "";
        if (metrics.containsKey(name + ":" + host + ":failure")) {
            failure = " <<failure>>";
        }
        ret.append(String.format("component %s %s[%s%n----%ncreated: %s%nthreads: %s%ncontinue on failure: %s%n%s%s]%n",
                getPumlIdentifier("computation:" + name + ":" + host), failure, name + " on " + host, created, threads,
                continueOnFailure, getBatchInfo(item), getComputationMetrics(host, name, item, metrics)));
    }

    protected String getComputationMetrics(String host, String name, JsonNode item, Map<String, String> metrics) {
        String ret = "";
        String baseKey = name + ":" + host;
        if (!metrics.containsKey(baseKey + ":count")) {
            return ret;
        }
        ret += "\n----\n";
        if (metrics.containsKey(baseKey + ":failure")) {
            ret += "FAILURE: " + metrics.get(baseKey + ":failure") + "\n";
        }
        ret += "record count: " + metrics.get(baseKey + ":count") + ", total: " + metrics.get(baseKey + ":sum") + "s\n";
        if (metrics.containsKey(baseKey + ":skipped")) {
            ret += "record skipped: " + metrics.get(baseKey + ":skipped") + "\n";
        }
        ret += "mean: " + metrics.get(baseKey + ":mean") + "s, p50: " + metrics.get(baseKey + ":p50") + "s, p99: "
                + metrics.get(baseKey + ":p99") + "s\n";
        ret += "rate 1min: " + metrics.get(baseKey + ":rate1m") + "op/s, 5min: " + metrics.get(baseKey + ":rate5m")
                + "op/s";
        if (!metrics.containsKey(baseKey + ":timer:count")) {
            return ret;
        }
        ret += "\n----\n";
        baseKey = baseKey + ":timer";
        ret += "timer count: " + metrics.get(baseKey + ":count") + ", total: " + metrics.get(baseKey + ":sum") + "s\n";
        ret += "mean: " + metrics.get(baseKey + ":mean") + "s, p50: " + metrics.get(baseKey + ":p50") + "s, p99: "
                + metrics.get(baseKey + ":p99") + "s\n";
        ret += "rate 5min: " + metrics.get(baseKey + ":rate5m") + "op/s";
        return ret;
    }

    protected String getBatchInfo(JsonNode item) {
        String ret = "";
        int batchCapacity = item.get("batchCapacity").asInt();
        if (batchCapacity > 1) {
            int batchThresholdMs = item.get("batchCapacity").asInt();
            ret += "batch " + item.get("batchCapacity").asText() + " " + batchThresholdMs + "ms\n";
        } else {
            ret += "no batch\n";
        }
        int retry = item.get("maxRetries").asInt();
        if (retry > 1) {
            ret += "max retry: " + item.get("maxRetries").asText() + ", delay: " + item.get("retryDelayMs").asText()
                    + "ms";
        } else {
            ret += "no retry";
        }
        return ret;
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
        int current = workerCount > 0 ? workerCount : -1;
        int bestNodes = workerCount > 0 ? 1 : -1;
        int optimalNodes = bestNodes;
        for (JsonNode computation : computations) {
            if ("bulk-scroller".equals(computation.at("/computation").asText())) {
                // don't take into account a computation that is also running on front nodes
                continue;
            }
            int nodes = computation.at("/current/nodes").asInt();
            if (nodes > current) {
                current = nodes;
            }
            int bNodes = computation.at("/best/nodes").asInt();
            if (bNodes > bestNodes) {
                bestNodes = bNodes;
                if (computation.at("/best/relevant").asBoolean()) {
                    optimalNodes = bestNodes;
                }
            }
        }
        ObjectNode ret = new ObjectMapper().createObjectNode();
        ret.put("currentNodes", current);
        ret.put("bestNodes", bestNodes);
        ret.put("optimalNodes", optimalNodes);
        ret.put("metric", optimalNodes - current);
        return ret;
    }

    protected JsonNode getActiveComputations(long atTimestamp) {
        ArrayNode ret = OBJECT_MAPPER.createArrayNode();
        Map<JsonNode, ObjectNode> computations = new HashMap<>();
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
        for (JsonNode stream : root.get("streams")) {
            partitions.put(Name.ofUrn(stream.get("name").asText()).getId(), stream.get("partitions").asInt());
        }
        // create a map of computation/threads
        Map<String, Integer> threads = new HashMap<>();
        for (JsonNode item : processors) {
            ArrayNode comps = (ArrayNode) item.get("computations");
            for (JsonNode comp : comps) {
                String name = Name.ofUrn(comp.get("name").asText()).getId();
                threads.put(name, comp.get("threads").asInt());
            }
        }
        // find active computations
        for (JsonNode node : metrics) {
            long ts = node.get("timestamp").asLong();
            if (atTimestamp - ts > ACTIVE_THRESHOLD_SECONDS) {
                continue;
            }
            // select computations with a significant rate
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.computation.processRecord".equals(metric.get("k").asText())
                        && (metric.get("count").asInt() > 0)) {
                    // ex: { "k": "nuxeo.streams.computation.processRecord",
                    // "computation": "audit-writer", "count": 32, "rate1m": 0.07875646231106845, "mean": ...}
                    boolean knownComputation = threads.containsKey(metric.get("computation").asText());
                    ObjectNode comp = computations.get(metric.get("computation"));
                    if (knownComputation && comp == null && metric.get("mean").asDouble() > 0) {
                        double rate1m = metric.get("rate1m").asDouble();
                        double mean = metric.get("mean").asDouble();
                        double maxRateByThread = 1 / mean;
                        // assume a rate is significant if one thread is busy at 50%
                        if (rate1m > maxRateByThread / 2) {
                            computations.put(metric.get("computation"), initComputation(metric.get("computation")));
                        }
                    }
                }
            }
            // select computation with lag, populate lag
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.global.stream.group.lag".equals(metric.get("k").asText())) {
                    // ex: { "k": "nuxeo.streams.global.stream.group.lag",
                    // "group": "bulk-csvExport", "stream": "bulk-csvExport", "v": 46 }
                    boolean knownComputation = threads.containsKey(metric.get("group").asText());
                    ObjectNode comp = computations.get(metric.get("group"));
                    if (knownComputation && (comp != null || metric.get("v").asInt() > 0)) {
                        comp = computations.computeIfAbsent(metric.get("group"), this::initComputation);
                        // populate computation lag for its streams
                        ObjectNode streams = (ObjectNode) comp.get("streams");
                        ObjectNode stream = OBJECT_MAPPER.createObjectNode();
                        stream.set("stream", metric.get("stream"));
                        stream.put("partitions", partitions.get(metric.get("stream").asText()));
                        stream.set("lag", metric.get("v"));
                        streams.set(metric.get("stream").asText(), stream);
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
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.global.stream.group.latency".equals(metric.get("k").asText())
                        && (metric.get("v").asInt() > 0)) {
                    // ex {"k": "nuxeo.streams.global.stream.group.latency",
                    // "group": "bulk-exposeBlob", "stream": "bulk-exposeBlob", "v": 123}
                    ObjectNode comp = computations.get(metric.get("group"));
                    if (comp != null) {
                        ObjectNode stream = (ObjectNode) comp.get("streams").get(metric.get("stream").asText());
                        if (stream != null) {
                            stream.set("latency", metric.get("v"));
                        }
                    }
                } else if ("nuxeo.streams.global.stream.group.end".equals(metric.get("k").asText())) {
                    ObjectNode comp = computations.get(metric.get("group"));
                    // ex {"k": "nuxeo.streams.global.stream.group.end",
                    // "group": "StreamImporter-runDocumentConsumers", "stream": "import-doc", "v": 10000}
                    if (comp != null) {
                        ObjectNode stream = (ObjectNode) comp.get("streams").get(metric.get("stream").asText());
                        if (stream != null) {
                            stream.set("end", metric.get("v"));
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
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.computation.processRecord".equals(metric.get("k").asText())
                        && (metric.get("count").asInt() > 0)) {
                    ObjectNode comp = computations.get(metric.get("computation"));
                    if (comp != null) {
                        ObjectNode compInstance = OBJECT_MAPPER.createObjectNode();
                        compInstance.set("nodeId", nodeId);
                        compInstance.put("threads", threads.getOrDefault(metric.get("computation").asText(), 1));
                        compInstance.set("timestamp", ts);
                        compInstance.set("count", metric.get("count"));
                        compInstance.set("sum", metric.get("sum"));
                        compInstance.set("rate1m", metric.get("rate1m"));
                        compInstance.set("rate5m", metric.get("rate5m"));
                        compInstance.set("min", metric.get("min"));
                        compInstance.set("p50", metric.get("p50"));
                        compInstance.set("mean", metric.get("mean"));
                        compInstance.set("p95", metric.get("p95"));
                        compInstance.set("max", metric.get("max"));
                        compInstance.set("stddev", metric.get("stddev"));
                        ((ArrayNode) comp.get("nodes")).add(compInstance);
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
            for (Iterator<JsonNode> iter = comp.get("streams").elements(); iter.hasNext();) {
                JsonNode stream = iter.next();
                if (stream.get("lag").asInt() > lag) {
                    lag = stream.get("lag").asInt();
                    part = partitions.get(stream.get("stream").asText());
                }
            }
            if (count == 0) {
                continue;
            }
            int eta = (int) (lag / rate1m);
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
                int bestNodes = (int) Math.ceil((double) part / (double) threadsPerNode);
                float bestRate = rate1m * part / threadsCount;
                int bestEta = (int) (lag / bestRate);
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

    protected ObjectNode initComputation(JsonNode key) {
        var active = OBJECT_MAPPER.createObjectNode();
        active.set("computation", key);
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

    protected String getPumlIdentifier(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", ".");
    }

}
