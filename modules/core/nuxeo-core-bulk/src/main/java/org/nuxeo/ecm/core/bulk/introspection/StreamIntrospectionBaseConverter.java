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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.lib.stream.log.Name;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for stream introspection converters containing shared functionality.
 * 
 * @since 2025.10
 */
public abstract class StreamIntrospectionBaseConverter {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected final String json;

    protected final JsonNode root;

    public StreamIntrospectionBaseConverter(String json) {
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
                                    getNiceDouble3(metric.get("sum").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":min",
                                    getNiceDouble3(metric.get("min").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":p50",
                                    getNiceDouble3(metric.get("p50").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":mean",
                                    getNiceDouble3(metric.get("mean").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":p99",
                                    getNiceDouble3(metric.get("p99").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":stddev",
                                    getNiceDouble3(metric.get("stddev").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":max",
                                    getNiceDouble3(metric.get("max").asDouble()));
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
                                    getNiceDouble3(metric.get("sum").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:min",
                                    getNiceDouble3(metric.get("min").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:p50",
                                    getNiceDouble3(metric.get("p50").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:mean",
                                    getNiceDouble3(metric.get("mean").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:p99",
                                    getNiceDouble3(metric.get("p99").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:max",
                                    getNiceDouble3(metric.get("max").asDouble()));
                            streamMetrics.put(computationName + ":" + nodeId + ":timer:stddev",
                                    getNiceDouble3(metric.get("stddev").asDouble()));
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

    protected String getStreamEnd(Map<String, String> metrics, String name) {
        String ret = metrics.get(name + ":end");
        return ret == null ? "0" : ret;
    }

    protected String getBatchInfo(JsonNode item) {
        StringBuilder ret = new StringBuilder();
        int batchCapacity = item.get("batchCapacity").asInt();
        if (batchCapacity > 1) {
            int batchThresholdMs = item.get("batchThresholdMs").asInt();
            ret.append("  - batch: ")
               .append(item.get("batchCapacity").asText())
               .append(", ")
               .append(batchThresholdMs)
               .append("ms\n");
        } else {
            ret.append("  - no batch\n");
        }
        int retry = item.get("maxRetries").asInt();
        if (retry > 0) {
            ret.append("  - max retry: ")
               .append(item.get("maxRetries").asText())
               .append(", delay: ")
               .append(item.get("retryDelayMs").asText())
               .append("ms\n");
        } else {
            ret.append("  - no retry\n");
        }
        return ret.toString();
    }
}
