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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A computation that reads processor and metrics streams to build a representation of stream activities in the cluster.
 * The representation is pushed to the KV Store.
 *
 * @since 11.5
 */
public class StreamIntrospectionComputation extends AbstractComputation {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(
            StreamIntrospectionComputation.class);

    public static final String NAME = "stream/introspection";

    public static final String INTROSPECTION_KV_STORE = "introspection";

    public static final String INTROSPECTION_KEY = "streamIntrospection";

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected final Map<String, JsonNode> streams = new HashMap<>();

    protected final Map<String, JsonNode> processors = new HashMap<>();

    protected final Map<String, JsonNode> metrics = new HashMap<>();

    protected static final long TTL_SECONDS = 300;

    protected String model;

    public StreamIntrospectionComputation() {
        super(NAME, 2, 0);
    }

    @Override
    public void init(ComputationContext context) {
        if (context.isSpareComputation()) {
            log.info("Spare instance nothing to report");
        } else {
            log.warn("Instance elected to introspect Nuxeo Stream activity");
        }
        loadModel(getKvStore().getString(INTROSPECTION_KEY));
    }

    protected void loadModel(String modelJson) {
        streams.clear();
        processors.clear();
        metrics.clear();
        if (isBlank(modelJson)) {
            model = null;
            return;
        }
        try {
            JsonNode modelNode = OBJECT_MAPPER.readTree(modelJson);
            JsonNode node = modelNode.get("streams");
            if (node.isArray()) {
                for (JsonNode item : node) {
                    streams.put(item.get("name").asText(), item);
                }
            }
            node = modelNode.get("processors");
            if (node.isArray()) {
                for (JsonNode item : node) {
                    processors.put(getProcessorKey(item), item);
                }
            }
            node = modelNode.get("metrics");
            if (node.isArray()) {
                for (JsonNode item : node) {
                    metrics.put(item.get("ip").asText(), item);
                }
            }
            model = modelJson;
        } catch (JsonProcessingException e) {
            log.error("Unable to parse KV model as JSON {}", modelJson, e);
            model = null;
        }
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        JsonNode json = getJson(record);
        if (json != null) {
            if (INPUT_1.equals(inputStreamName)) {
                updateStreamsAndProcessors(json);
            } else if (INPUT_2.equals(inputStreamName)) {
                if (json.has("ip")) {
                    metrics.put(json.get("ip").asText(), json);
                }
            }
        }
        removeOldNodes();
        buildModel();
        updateModel();
        context.askForCheckpoint();
    }

    protected void updateStreamsAndProcessors(JsonNode node) {
        JsonNode streamsNode = node.get("streams");
        if (streamsNode == null) {
            log.warn("Invalid metric without streams field: {}", node);
            return;
        }
        if (streamsNode.isArray()) {
            for (JsonNode item : streamsNode) {
                streams.put(item.get("name").asText(), item);
            }
        }
        ((ObjectNode) node).remove("streams");
        processors.put(getProcessorKey(node), node);
    }

    protected String getProcessorKey(JsonNode json) {
        return json.at("/metadata/ip").asText() + ":" + json.at("/metadata/processorName").asText();
    }

    protected void updateModel() {
        KeyValueStore kv = getKvStore();
        kv.put(INTROSPECTION_KEY, model);
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(INTROSPECTION_KV_STORE);
    }

    protected void buildModel() {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        ArrayNode streamsNode = OBJECT_MAPPER.createArrayNode();
        streamsNode.addAll(streams.values());
        node.set("streams", streamsNode);
        ArrayNode processorsNode = OBJECT_MAPPER.createArrayNode();
        processorsNode.addAll(processors.values());
        node.set("processors", processorsNode);
        ArrayNode metricsNode = OBJECT_MAPPER.createArrayNode();
        metricsNode.addAll(metrics.values());
        node.set("metrics", metricsNode);
        try {
            model = OBJECT_MAPPER.writer().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Cannot build JSON model", e);
            model = "{}";
        }
    }

    protected void removeOldNodes() {
        // Remove all nodes with metrics older than TTL
        long now = System.currentTimeMillis() / 1000;
        List<String> toRemove = metrics.values()
                                       .stream()
                                       .filter(json -> (now - json.get("timestamp").asLong()) > TTL_SECONDS)
                                       .map(json -> json.get("ip").asText())
                                       .collect(Collectors.toList());
        log.debug("Removing nodes: {}", toRemove);
        toRemove.forEach(metrics::remove);
        toRemove.forEach(ip -> {
            List<String> toRemoveProcessors = processors.keySet()
                                                        .stream()
                                                        .filter(key -> key.startsWith(ip))
                                                        .collect(Collectors.toList());
            toRemoveProcessors.forEach(processors::remove);
        });
    }

    protected JsonNode getJson(Record record) {
        String json = new String(record.getData(), UTF_8);
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON from record {}: {}", record, json, e);
            return null;
        }
    }
}
