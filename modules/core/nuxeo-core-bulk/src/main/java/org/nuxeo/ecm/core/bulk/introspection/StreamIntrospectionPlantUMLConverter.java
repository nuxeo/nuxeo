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
 */
package org.nuxeo.ecm.core.bulk.introspection;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts stream introspection JSON data to PlantUML diagram format.
 * 
 * @since 2025.10
 */
public class StreamIntrospectionPlantUMLConverter extends StreamIntrospectionBaseConverter {

    public StreamIntrospectionPlantUMLConverter(String json) {
        super(json);
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
                continueOnFailure, getBatchInfo(item), getComputationMetricsForPuml(host, name, item, metrics)));
    }

    protected String getComputationMetricsForPuml(String host, String name, JsonNode item,
            Map<String, String> metrics) {
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

    @Override
    protected String getBatchInfo(JsonNode item) {
        String ret = "";
        int batchCapacity = item.get("batchCapacity").asInt();
        if (batchCapacity > 1) {
            int batchThresholdMs = item.get("batchThresholdMs").asInt();
            ret += "batch " + item.get("batchCapacity").asText() + " " + batchThresholdMs + "ms\n";
        } else {
            ret += "no batch\n";
        }
        int retry = item.get("maxRetries").asInt();
        if (retry > 0) {
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
