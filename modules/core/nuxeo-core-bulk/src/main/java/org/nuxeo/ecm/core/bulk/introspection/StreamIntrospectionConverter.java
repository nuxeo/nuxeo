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
import java.util.Map;

import org.nuxeo.lib.stream.log.Name;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                // ret.append("rectangle node." + host + " {\n");
                JsonNode computations = item.get("computations");
                if (computations.isArray()) {
                    for (JsonNode computation : computations) {
                        dumpComputation(host, ret, computation, streamMetrics);
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
                + "skinparam component {\n"
                + "  BorderColor black\n" + "  ArrowColor #CC6655\n" + "}\n";
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

    protected void dumpComputation(String host, StringBuilder ret, JsonNode item, Map<String, String> metrics) {
        String name = item.get("name").asText();
        String threads = item.get("threads").asText();
        String continueOnFailure = item.get("continueOnFailure").asText();
        String failure = "";
        if (metrics.containsKey(name + ":" + host + ":failure")) {
            failure = " <<failure>>";
        }
        ret.append(String.format("component %s %s[%s%n----%nthreads: %s%ncontinue on failure: %s%n%s%s]%n",
                getPumlIdentifier("computation:" + name + ":" + host), failure, name + " on " + host, threads, continueOnFailure,
                getBatchInfo(item), getComputationMetrics(host, name, item, metrics)));
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

    protected String getPumlIdentifier(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", ".");
    }

}
