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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts stream introspection JSON data to D2 diagram format (https://d2lang.com/)
 * 
 * @since 2025.10
 */
public class StreamIntrospectionD2Converter extends StreamIntrospectionBaseConverter {

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

    protected List<String> excludePatterns;

    protected boolean excludeInactive;

    public StreamIntrospectionD2Converter(String json) {
        super(json);
        this.excludePatterns = java.util.Collections.emptyList();
        this.excludeInactive = false;
    }

    public String getD2() {
        StringBuilder ret = new StringBuilder();
        Map<String, String> streamMetrics = parseMetrics();
        ret.append(String.format(D2_HEADER_TEMPLATE, "Stream Introspection at " + streamMetrics.get("date")));

        // First, output all streams (outside containers)
        JsonNode node = root.get("streams");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                dumpStream(ret, item, streamMetrics);
            }
        }

        // Create computation-group containers for each unique computation name
        Set<String> computationNames = collectComputationNames();
        for (String computationName : computationNames) {
            ret.append(String.format("%s: {\n", getD2Identifier("group_" + computationName.replace("/", "_"))));
            ret.append(String.format("  label: \"%s\"\n", computationName));
            ret.append("  class: computation-group\n");
            ret.append("}\n\n");
        }

        // Process each processor and place computations in their respective containers
        node = root.get("processors");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String host = item.at("/metadata/nodeId").asText();
                String created = Instant.ofEpochSecond(item.at("/metadata/created").asLong()).toString();
                JsonNode computations = item.get("computations");
                if (computations.isArray()) {
                    for (JsonNode computation : computations) {
                        // Filter is already applied inside dumpComputationInGroup method
                        dumpComputationInGroup(host, ret, computation, streamMetrics, created);
                    }
                }
            }
        }

        // Now output connections between streams and computations
        node = root.get("processors");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String host = item.at("/metadata/nodeId").asText();
                JsonNode topologies = item.get("topology");
                if (topologies.isArray()) {
                    for (JsonNode topo : topologies) {
                        String comment = "";
                        String source = topo.get(0).asText();
                        String target = topo.get(1).asText();

                        // Apply filtering for connections
                        boolean shouldSkipConnection = false;
                        if (source.startsWith("stream:")) {
                            String stream = source.replace("stream:", "");
                            if (shouldExcludeByPattern(stream)
                                    || shouldExcludeStreamByInactivity(stream, streamMetrics)) {
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
                            if (shouldExcludeByPattern(stream)
                                    || shouldExcludeStreamByInactivity(stream, streamMetrics)) {
                                shouldSkipConnection = true;
                            }
                        }

                        if (shouldSkipConnection) {
                            continue;
                        }

                        if (target.startsWith("computation:")) {
                            String stream = source.replace("stream:", "");
                            String computation = target.replace("computation:", "");
                            String lag = streamMetrics.get(stream + ":" + computation + ":lag");
                            String latency = streamMetrics.get(stream + ":" + computation + ":latency");
                            String pos = streamMetrics.get(stream + ":" + computation + ":pos");
                            String end = getStreamEnd(streamMetrics, stream);
                            // provide info only when there is a lag
                            if (lag != null && !"0".equals(lag)) {
                                comment = String.format("%s/%s lag: %s, latency: %ss", pos, end, lag, latency);
                            }
                        }
                        ret.append(String.format("%s -> %s", getD2IdentifierForConnection(host, source),
                                getD2IdentifierForConnection(host, target)));
                        if (!comment.isEmpty()) {
                            ret.append(String.format(": \"%s\"", comment));
                        }
                        ret.append("\n");
                    }
                }
            }
        }

        return ret.toString();
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
        this.excludePatterns = excludePatterns != null ? excludePatterns : java.util.Collections.emptyList();
        this.excludeInactive = excludeInactive;
        return getD2();
    }

    /**
     * Checks if a name (stream or computation) should be excluded based on patterns.
     */
    protected boolean shouldExcludeByPattern(String name) {
        if (excludePatterns == null || excludePatterns.isEmpty()) {
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
    protected boolean shouldExcludeByInactivity(String name, String host, Map<String, String> metrics) {
        if (!excludeInactive) {
            return false;
        }
        // A computation is considered inactive if it has no metrics (computation-idle class)
        String baseKey = name + ":" + host;
        return !metrics.containsKey(baseKey + ":count") && !metrics.containsKey(baseKey + "timer:count");
    }

    /**
     * Checks if a stream should be excluded based on inactivity filter.
     */
    protected boolean shouldExcludeStreamByInactivity(String streamName, Map<String, String> metrics) {
        if (!excludeInactive) {
            return false;
        }
        // A stream is considered inactive if it has no records (stream-empty class)
        String records = getStreamEnd(metrics, streamName);
        return "0".equals(records);
    }

    protected void dumpStream(StringBuilder ret, JsonNode item, Map<String, String> metrics) {
        String name = item.get("name").asText();

        // Apply pattern filtering for streams
        if (shouldExcludeByPattern(name)) {
            return;
        }

        // Apply inactivity filtering for streams
        if (shouldExcludeStreamByInactivity(name, metrics)) {
            return;
        }

        String partitions = item.get("partitions").asText();
        String codec = item.get("codec").asText();
        String records = getStreamEnd(metrics, name);

        String streamId = getD2Identifier("stream:" + name);
        String streamClass = "0".equals(records) ? "stream-empty" : "stream";

        ret.append(String.format("%s: |md\n", streamId));
        ret.append(String.format("  #### %s\n", name));
        ret.append(String.format("  - partitions: %s\n", partitions));
        ret.append(String.format("  - codec: %s\n", codec));
        ret.append(String.format("  - records: %s\n", records));
        ret.append("|\n");
        ret.append(String.format("%s.class: %s\n\n", streamId, streamClass));
    }

    protected String getComputationMetrics(String host, String name, JsonNode item, Map<String, String> metrics) {
        StringBuilder ret = new StringBuilder();
        String baseKey = name + ":" + host;
        if (!metrics.containsKey(baseKey + ":count") && !metrics.containsKey(baseKey + ":timer:count")) {
            return ret.toString();
        }
        if (metrics.containsKey(baseKey + ":failure")) {
            ret.append("  #### FAILURE: ").append(metrics.get(baseKey + ":failure")).append("\n");
        }
        if (metrics.containsKey(baseKey + ":count")) {
            ret.append(getTimerAsJson("record", baseKey, metrics));
        }
        if (metrics.containsKey(baseKey + ":timer:count")) {
            ret.append(getTimerAsJson("timer", baseKey + ":timer", metrics));
        }
        return ret.toString();
    }

    protected String getTimerAsJson(String title, String baseKey, Map<String, String> metrics) {
        StringBuilder ret = new StringBuilder();
        ret.append("  #### ").append(title).append("\n").append("  - count: ").append(metrics.get(baseKey + ":count"));
        if (metrics.containsKey(baseKey + ":skipped")) {
            ret.append(", skipped: ").append(metrics.get(baseKey + ":skipped"));
        }
        ret.append(", sum: ").append(metrics.get(baseKey + ":sum")).append("s\n");
        ret.append("  - duration: ")
           .append(metrics.get(baseKey + ":mean"))
           .append("s (+/- ")
           .append(metrics.get(baseKey + ":stddev"))
           .append("s)\n    -  min: ")
           .append(metrics.get(baseKey + ":min"))
           .append("s, max: ")
           .append(metrics.get(baseKey + ":max"))
           .append("s\n    -  p50: ")
           .append(metrics.get(baseKey + ":p50"))
           .append("s, p99: ")
           .append(metrics.get(baseKey + ":p99"))
           .append("s\n");
        ret.append("  - rate 1min: ")
           .append(metrics.get(baseKey + ":rate1m"))
           .append("ops/s, 5min: ")
           .append(metrics.get(baseKey + ":rate5m"))
           .append("ops/s\n");
        return ret.toString();
    }

    @Override
    protected String getBatchInfo(JsonNode item) {
        StringBuilder ret = new StringBuilder();
        int retry = item.get("maxRetries").asInt();
        if (retry > 0) {
            ret.append("  - retry: ")
               .append(item.get("maxRetries").asText())
               .append(", delay: ")
               .append(item.get("retryDelayMs").asText())
               .append("ms, ");
        } else {
            ret.append("  - no retry, ");
        }
        int batchCapacity = item.get("batchCapacity").asInt();
        if (batchCapacity > 1) {
            int batchThresholdMs = item.get("batchThresholdMs").asInt();
            ret.append("batch: ")
               .append(item.get("batchCapacity").asText())
               .append(", ")
               .append(batchThresholdMs)
               .append("ms\n");
        } else {
            ret.append("no batch\n");
        }
        return ret.toString();
    }

    protected String getD2Identifier(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    protected Set<String> collectComputationNames() {
        Set<String> computationNames = new HashSet<>();
        Map<String, String> streamMetrics = parseMetrics();
        JsonNode node = root.get("processors");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String host = item.at("/metadata/nodeId").asText();
                JsonNode computations = item.get("computations");
                if (computations != null && computations.isArray()) {
                    for (JsonNode computation : computations) {
                        String name = computation.get("name").asText();
                        if (!name.isEmpty() && !shouldExcludeByPattern(name)
                                && !shouldExcludeByInactivity(name, host, streamMetrics)) {
                            computationNames.add(name);
                        }
                    }
                }
            }
        }
        return computationNames;
    }

    protected void dumpComputationInGroup(String host, StringBuilder ret, JsonNode item, Map<String, String> metrics,
            String created) {
        String name = item.get("name").asText();

        // Apply pattern filtering for computations
        if (shouldExcludeByPattern(name)) {
            return;
        }

        // Apply inactivity filtering for computations
        if (shouldExcludeByInactivity(name, host, metrics)) {
            return;
        }

        String threads = item.get("threads").asText();
        String continueOnFailure = item.get("continueOnFailure").asText();
        boolean hasFailure = metrics.containsKey(name + ":" + host + ":failure");

        String groupContainerId = getD2Identifier("group_" + name.replace("/", "_"));
        String computationId = getD2Identifier("comp_" + host);

        ret.append(String.format("%s.%s: |md\n", groupContainerId, computationId));
        ret.append(String.format("  ### %s\n", host));
        ret.append(String.format("  - created: %s\n", created));
        ret.append(String.format("  - threads: %s\n", threads));
        ret.append(getBatchInfo(item));
        ret.append(String.format("  - continue on failure: %s\n", continueOnFailure));
        String computationMetrics = getComputationMetrics(host, name, item, metrics);
        if (!computationMetrics.isEmpty()) {
            ret.append(computationMetrics);
        }
        ret.append("|\n");

        if (hasFailure) {
            ret.append(String.format("%s.%s.class: computation-failure\n\n", groupContainerId, computationId));
        } else if (computationMetrics.isEmpty()) {
            ret.append(String.format("%s.%s.class: computation-idle\n\n", groupContainerId, computationId));
        } else {
            ret.append(String.format("%s.%s.class: computation\n\n", groupContainerId, computationId));
        }
    }

    protected String getD2IdentifierForConnection(String host, String id) {
        if (id.startsWith("computation:")) {
            String computationName = id.replace("computation:", "");
            String groupContainerId = getD2Identifier("group_" + computationName.replace("/", "_"));
            String computationId = getD2Identifier("comp_" + host);
            return groupContainerId + "." + computationId;
        }
        return getD2Identifier(id);
    }
}
