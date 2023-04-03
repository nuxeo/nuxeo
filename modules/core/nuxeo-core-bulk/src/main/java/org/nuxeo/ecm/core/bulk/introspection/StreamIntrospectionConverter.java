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

    protected final String json;

    protected final JsonNode root;

    public StreamIntrospectionConverter(String json) {
        this.json = json;
        try {
            this.root = OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON: " + json, e);
        }
    }

    public String getStreams() {
        return root.get("streams").toString();
    }

    public String getConsumers(String stream) {
        if (StringUtils.isBlank(stream)) {
            return "[]";
        }
        String match = "stream:" + stream;
        JsonNode node = root.get("processors");
        Set<String> consumers = new HashSet<>();
        if (node.isArray()) {
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
        if (node.isArray()) {
            for (JsonNode item : node) {
                dumpStream(ret, item, streamMetrics);
            }
        }

        node = root.get("processors");
        if (node.isArray()) {
            for (JsonNode item : node) {
                String host = item.at("/metadata/ip").asText();
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
        if (node.isArray()) {
            for (JsonNode host : node) {
                String hostIp = host.get("ip").asText();
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
                            streamMetrics.put(computationName + ":" + hostIp + ":count", metric.get("count").asText());
                            streamMetrics.put(computationName + ":" + hostIp + ":sum",
                                    getNiceDouble3(metric.get("sum").asDouble() / 1000000000));
                            streamMetrics.put(computationName + ":" + hostIp + ":p50",
                                    getNiceDouble3(metric.get("p50").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":mean",
                                    getNiceDouble3(metric.get("mean").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":p99",
                                    getNiceDouble3(metric.get("p99").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":rate1m",
                                    getNiceDouble(metric.get("rate1m").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":rate5m",
                                    getNiceDouble(metric.get("rate5m").asDouble()));
                        } else if (metric.get("k").asText().endsWith("processTimer")) {
                            int count = metric.get("count").asInt();
                            if (count == 0) {
                                continue;
                            }
                            String computationName = Name.urnOfId(metric.get("computation").asText());
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:count",
                                    metric.get("count").asText());
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:sum",
                                    getNiceDouble3(metric.get("sum").asDouble() / 1000000000));
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:p50",
                                    getNiceDouble3(metric.get("p50").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:mean",
                                    getNiceDouble3(metric.get("mean").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:p99",
                                    getNiceDouble3(metric.get("p99").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:rate1m",
                                    getNiceDouble(metric.get("rate1m").asDouble()));
                            streamMetrics.put(computationName + ":" + hostIp + ":timer:rate5m",
                                    getNiceDouble(metric.get("rate5m").asDouble()));
                        } else if (metric.get("k").asText().endsWith("computation.failure")) {
                            int failure = metric.get("v").asInt();
                            if (failure > 0) {
                                String computationName = Name.urnOfId(metric.get("computation").asText()) + ":"
                                        + hostIp;
                                streamMetrics.put(computationName + ":failure", metric.get("v").asText());
                            }
                        } else if (metric.get("k").asText().endsWith("stream.failure")) {
                            int value = metric.get("v").asInt();
                            if (value > 0) {
                                streamMetrics.put(hostIp + ":failure", metric.get("v").asText());
                            }
                        } else if (metric.get("k").asText().endsWith("computation.skippedRecord")) {
                            int value = metric.get("v").asInt();
                            if (value > 0) {
                                String computationName = Name.urnOfId(metric.get("computation").asText()) + ":"
                                        + hostIp;
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode ret = mapper.getNodeFactory().objectNode();
        JsonNode nodes = getClusterNodes();
        int workerCount = 0;
        for (JsonNode node : nodes) {
            if ("worker".equals(node.at("/type").asText())) {
                workerCount++;
            }
        }
        JsonNode computations = getActiveComputations();
        JsonNode scale = getScaleMetrics(workerCount, (ArrayNode) computations);
        ret.set("scale", scale);
        ret.set("nodes", nodes);
        ret.set("computations", computations);
        return ret.toString();
    }

    protected JsonNode getScaleMetrics(int workerCount, ArrayNode computations) {
        int scale = 1; // always keep a worker nodes
        int current = workerCount > 0 ? workerCount : 1;
        for (JsonNode computation : computations) {
            int nodes = computation.at("/current/nodes").asInt();
            int bNodes = computation.at("/best/nodes").asInt();
            if (bNodes > nodes && bNodes > scale) {
                current = nodes;
                scale = bNodes;
            }
        }
        ObjectNode ret = new ObjectMapper().getNodeFactory().objectNode();
        ret.put("currentNodes", current);
        ret.put("bestNodes", scale);
        ret.put("metric", scale - current);
        return ret;
    }

    protected JsonNode getActiveComputations() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode ret = mapper.getNodeFactory().arrayNode();
        Map<JsonNode, ObjectNode> computations = new HashMap<>();
        JsonNode metrics = root.get("metrics");
        if (!metrics.isArray()) {
            // no data available ?
            return ret;
        }
        JsonNode processors = root.get("processors");
        if (!processors.isArray()) {
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
        // find computation with lag
        for (JsonNode node : metrics) {
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.global.stream.group.lag".equals(metric.get("k").asText())
                        && (metric.get("v").asInt() > 0)) {
                    ObjectNode comp = computations.get(metric.get("group"));
                    if (comp == null) {
                        comp = mapper.getNodeFactory().objectNode();
                        comp.set("computation", metric.get("group"));
                        comp.set("streams", mapper.getNodeFactory().objectNode());
                    }
                    ObjectNode streams = (ObjectNode) comp.get("streams");
                    ObjectNode stream = mapper.getNodeFactory().objectNode();
                    stream.set("stream", metric.get("stream"));
                    stream.put("partitions", partitions.get(metric.get("stream").asText()));
                    stream.set("lag", metric.get("v"));
                    streams.set(metric.get("stream").asText(), stream);
                    comp.set("nodes", mapper.getNodeFactory().arrayNode());
                    computations.put(metric.get("group"), comp);
                }
            }
        }
        // get latency, end
        for (JsonNode node : metrics) {
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.global.stream.group.latency".equals(metric.get("k").asText())
                        && (metric.get("v").asInt() > 0)) {
                    ObjectNode comp = computations.get(metric.get("group"));
                    if (comp != null) {
                        ObjectNode stream = (ObjectNode) comp.get("streams").get(metric.get("stream").asText());
                        if (stream != null) {
                            stream.set("latency", metric.get("v"));
                        }
                    }
                } else if ("nuxeo.streams.global.stream.group.end".equals(metric.get("k").asText())) {
                    ObjectNode comp = computations.get(metric.get("group"));
                    if (comp != null) {
                        ObjectNode stream = (ObjectNode) comp.get("streams").get(metric.get("stream").asText());
                        if (stream != null) {
                            stream.set("end", metric.get("v"));
                        }
                    }
                }
            }
        }
        // then metrics per node
        for (JsonNode node : metrics) {
            JsonNode ip = node.get("ip");
            JsonNode ts = node.get("timestamp");
            for (JsonNode metric : node.at("/metrics")) {
                if ("nuxeo.streams.computation.processRecord".equals(metric.get("k").asText())
                        && (metric.get("count").asInt() > 0)) {
                    ObjectNode comp = computations.get(metric.get("computation"));
                    if (comp != null) {
                        ObjectNode compInstance = mapper.getNodeFactory().objectNode();
                        compInstance.set("ip", ip);
                        compInstance.put("threads", threads.get(metric.get("computation").asText()));
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
            if (count == 0 || lag == 0) {
                continue;
            }
            int eta = (int) (lag / rate1m);
            ObjectNode current = mapper.getNodeFactory().objectNode();
            current.put("nodes", count);
            current.put("threads", threadsCount);
            current.put("rate1m", rate1m);
            current.put("eta", eta);
            ObjectNode best = mapper.getNodeFactory().objectNode();
            int bestNodes = (int) Math.ceil((double) part / (double) threadsPerNode);
            int bestThreads = part;
            int bestEta = (int) (lag / (rate1m * bestThreads / threadsCount));
            best.put("nodes", bestNodes);
            best.put("threads", bestThreads);
            best.put("rate1m", rate1m * bestThreads / threadsCount);
            best.put("eta", bestEta);
            comp.set("current", current);
            comp.set("best", best);
        }
        computations.values().forEach(ret::add);
        return ret;
    }

    protected JsonNode getClusterNodes() {
        Map<JsonNode, ObjectNode> nodes = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode ret = mapper.getNodeFactory().arrayNode();
        JsonNode processors = root.get("processors");
        if (processors.isArray()) {
            for (JsonNode item : processors) {
                nodes.put(item.at("/metadata/ip"), item.at("/metadata").deepCopy());
            }
        }
        JsonNode metrics = root.get("metrics");
        if (processors.isArray()) {
            for (JsonNode item : metrics) {
                ObjectNode node = nodes.get(item.at("/ip"));
                if (node == null) {
                    continue;
                }
                node.set("nodeId", item.at("/nodeId"));
                node.put("alive", Instant.ofEpochSecond(item.at("/timestamp").asLong()).toString());
                node.put("created", Instant.ofEpochSecond(node.at("/created").asLong()).toString());
                node.remove("processorName");
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
        nodes.values().forEach(ret::add);
        return ret;
    }

    protected String getPumlIdentifier(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", ".");
    }

}
