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

import static java.util.stream.Collectors.toMap;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 2025.11
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamIntrospectionJsonReader extends AbstractJsonReader<StreamIntrospection> {

    private static final Logger log = LogManager.getLogger(StreamIntrospectionJsonReader.class);

    @Override
    public StreamIntrospection read(JsonNode jn) throws IOException {
        var streams = readStreams(jn.withArray("streams"));
        var processors = readProcessors(jn.withArray("processors"));
        var metrics = readMetrics(jn.withArray("metrics"));
        return new StreamIntrospection(streams, processors, metrics);
    }

    protected List<StreamIntrospection.Stream> readStreams(@Nonnull ArrayNode streamsJn) {
        return streamsJn.valueStream()
                        .map(streamJn -> new StreamIntrospection.Stream(streamJn.get("name").textValue(),
                                streamJn.get("partitions").asInt(), streamJn.get("codec").asText()))
                        .toList();
    }

    protected List<StreamIntrospection.Processor> readProcessors(ArrayNode processorsJn) {
        return processorsJn.valueStream()
                           .map(processorJn -> new StreamIntrospection.Processor(
                                   readProcessorMetadata(processorJn.withObject("metadata")),
                                   readProcessorComputations(processorJn.withArray("computations")),
                                   readProcessorTopology(processorJn.withArray("topology"))))
                           .toList();
    }

    protected StreamIntrospection.ProcessorMetadata readProcessorMetadata(ObjectNode metadataJn) {
        return new StreamIntrospection.ProcessorMetadata( //
                metadataJn.get("processorName").asText(), //
                metadataJn.optional("nodeId").map(JsonNode::asText).orElse(null), //
                metadataJn.get("hostname").asText(), //
                metadataJn.get("ip").asText(), //
                Integer.parseInt(metadataJn.get("cpuCores").asText()), //
                ByteSize.parse(metadataJn.get("jvmHeapSize").asText()), //
                metadataJn.optional("created")
                          .map(JsonNode::asText)
                          .map(Long::parseLong)
                          .map(Instant::ofEpochSecond)
                          .orElseGet(Instant::now));
    }

    protected List<StreamIntrospection.ProcessorComputation> readProcessorComputations(ArrayNode computationsJn) {
        return computationsJn.valueStream()
                             .map(computationJn -> new StreamIntrospection.ProcessorComputation(
                                     computationJn.get("name").asText(), //
                                     computationJn.get("threads").asInt(), //
                                     computationJn.get("continueOnFailure").asBoolean(), //
                                     computationJn.get("batchCapacity").asInt(), //
                                     Duration.ofMillis(computationJn.get("batchThresholdMs").asLong()), //
                                     computationJn.get("maxRetries").asInt(), //
                                     Duration.ofMillis(computationJn.get("retryDelayMs").asLong())))
                             .toList();
    }

    protected List<StreamIntrospection.ProcessorTopology> readProcessorTopology(ArrayNode topologyJn) {
        return topologyJn.valueStream().map(this::readInnerTopology).filter(Objects::nonNull).toList();
    }

    protected StreamIntrospection.ProcessorTopology readInnerTopology(JsonNode innerTopologyJn) {
        if (innerTopologyJn.isArray() && innerTopologyJn.size() == 2) {
            String source = innerTopologyJn.get(0).asText();
            String target = innerTopologyJn.get(1).asText();
            return StreamIntrospection.ProcessorTopology.valueOf(source, target);
        }
        log.warn("Ignoring unknown topology: {}", innerTopologyJn::toPrettyString);
        return null;
    }

    protected List<StreamIntrospection.Metrics> readMetrics(ArrayNode metricsJn) {
        return metricsJn.valueStream()
                        .map(metricJn -> new StreamIntrospection.Metrics( //
                                Instant.ofEpochSecond(Long.parseLong(metricJn.get("timestamp").asText())), //
                                metricJn.get("hostname").asText(), //
                                metricJn.get("ip").asText(), //
                                metricJn.get("nodeId").asText(), //
                                metricJn.withArray("metrics")
                                        .valueStream()
                                        .map(this::readInnerMetric)
                                        .filter(Objects::nonNull)
                                        .toList()))
                        .toList();
    }

    protected StreamIntrospection.Metric readInnerMetric(JsonNode innerMetricJn) {
        var innerMetricsByProperty = innerMetricJn.propertyStream()
                                                  .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        String key = innerMetricsByProperty.remove("k").asText();
        if (innerMetricsByProperty.containsKey("v")) {
            Number value = innerMetricsByProperty.remove("v").numberValue();
            return new StreamIntrospection.ValueMetric(key, value, valuesJsonNodeToString(innerMetricsByProperty));
        } else if (innerMetricsByProperty.containsKey("count")) {
            long count = innerMetricsByProperty.remove("count").asLong();
            if (count == 0) {
                return new StreamIntrospection.EmptyTimerMetric(key, valuesJsonNodeToString(innerMetricsByProperty));
            } else {
                return new StreamIntrospection.TimerMetric( //
                        key, //
                        count, //
                        innerMetricsByProperty.remove("rate1m").asDouble(), //
                        innerMetricsByProperty.remove("rate5m").asDouble(), //
                        innerMetricsByProperty.remove("sum").asDouble(), //
                        innerMetricsByProperty.remove("max").asDouble(), //
                        innerMetricsByProperty.remove("mean").asDouble(), //
                        innerMetricsByProperty.remove("min").asDouble(), //
                        innerMetricsByProperty.remove("stddev").asDouble(), //
                        innerMetricsByProperty.remove("p50").asDouble(), //
                        innerMetricsByProperty.remove("p95").asDouble(), //
                        innerMetricsByProperty.remove("p99").asDouble(), //
                        valuesJsonNodeToString(innerMetricsByProperty));
            }
        }
        log.warn("Ignoring unknown metric: {}", innerMetricJn::toPrettyString);
        return null;
    }

    protected Map<String, String> valuesJsonNodeToString(Map<String, JsonNode> innerMetricsByProperty) {
        return innerMetricsByProperty.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().asText()));
    }
}
