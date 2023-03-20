/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.computation.log;

import static java.lang.Math.min;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 9.3
 */
public class LogStreamProcessor implements StreamProcessor {
    private static final Log log = LogFactory.getLog(LogStreamProcessor.class);

    protected final LogManager manager;

    protected Topology topology;

    protected Settings settings;

    protected List<ComputationPool> pools;

    protected LogStreamManager streamManager;

    protected final boolean needRegister;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Deprecated
    public LogStreamProcessor(LogManager manager) {
        needRegister = true;
        this.manager = manager;
        this.streamManager = new LogStreamManager(manager);
    }

    public LogStreamProcessor(LogStreamManager streamManager) {
        needRegister = false;
        this.streamManager = streamManager;
        this.manager = streamManager.getLogManager();
    }

    @Override
    public StreamProcessor init(Topology topology, Settings settings) {
        log.debug("Initializing ...");
        this.topology = topology;
        this.settings = settings;
        if (needRegister) {
            // backward compat when using a LogManager instead of StreamManager
            streamManager.register("_", topology, settings);
        }
        return this;
    }

    @Override
    public void start() {
        log.debug("Starting ...");
        this.pools = initPools();
        Objects.requireNonNull(pools);
        pools.forEach(ComputationPool::start);
    }

    @Override
    public boolean waitForAssignments(Duration timeout) throws InterruptedException {
        for (ComputationPool pool : pools) {
            // TODO: consider decreasing timeout
            if (!pool.waitForAssignments(timeout)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        return pools.stream().allMatch(ComputationPool::isTerminated);
    }

    @Override
    public String toJson(Map<String, String> meta) {
        try {
            ObjectNode ret = OBJECT_MAPPER.createObjectNode();
            ObjectNode metaNode = OBJECT_MAPPER.createObjectNode();
            meta.forEach((key, value) -> metaNode.put(key, value));
            ret.set("metadata", metaNode);
            // list streams with settings
            ArrayNode streamsNode = OBJECT_MAPPER.createArrayNode();
            topology.streamsSet()
                    .stream()
                    .filter(stream -> !settings.isExternal(Name.ofUrn(stream)))
                    .forEach(stream -> {
                        ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("name", stream);
                item.put("partitions", settings.getPartitions(stream));
                item.put("codec", settings.getCodec(stream).getName());
                streamsNode.add(item);
            });
            ret.set("streams", streamsNode);
            // list computations with settings
            ArrayNode computationsNode = OBJECT_MAPPER.createArrayNode();
            topology.metadataList().forEach(comp -> {
                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("name", comp.name());
                item.put("threads", settings.getConcurrency(comp.name()));
                item.put("continueOnFailure", settings.getPolicy(comp.name()).continueOnFailure());
                item.put("batchCapacity", settings.getPolicy(comp.name()).getBatchCapacity());
                item.put("batchThresholdMs", settings.getPolicy(comp.name()).getBatchThreshold().toMillis());
                item.put("maxRetries", settings.getPolicy(comp.name()).getRetryPolicy().getMaxRetries());
                item.put("retryDelayMs", settings.getPolicy(comp.name()).getRetryPolicy().getDelay().toMillis());
                computationsNode.add(item);
            });
            ret.set("computations", computationsNode);
            // list DAG edges
            ArrayNode topologyNode = OBJECT_MAPPER.createArrayNode();
            DirectedAcyclicGraph<Topology.Vertex, DefaultEdge> dag = topology.getDag();
            for (DefaultEdge edge : dag.edgeSet()) {
                ArrayNode edgeNode = OBJECT_MAPPER.createArrayNode();
                edgeNode.add(getEdgeName(dag.getEdgeSource(edge)));
                edgeNode.add(getEdgeName(dag.getEdgeTarget(edge)));
                topologyNode.add(edgeNode);
            }
            ret.set("topology", topologyNode);
            String json = OBJECT_MAPPER.writer().writeValueAsString(ret);
            if (log.isDebugEnabled()) {
                log.debug("Starting processor: " + json);
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new StreamRuntimeException("Fail to dump processor as JSON", e);
        }
    }

    @Override
    public boolean stopComputation(Name computation) {
        if (pools == null) {
            log.debug("Processor not started, nothing to stop");
            return false;
        }
        ComputationPool pool = pools.stream()
                                    .filter(comp -> computation.getUrn().equals(comp.getComputationName()))
                                    .findFirst()
                                    .orElse(null);
        if (pool == null) {
            log.debug("Unknown computation, nothing to stop");
            return false;
        }
        if (pool.isTerminated()) {
            log.debug("Computation pool already terminated");
            return false;
        }
        log.warn("Stopping computation thread pool: " + computation);
        pool.stop(Duration.ofSeconds(1));
        return true;
    }

    @Override
    public boolean startComputation(Name computation) {
        if (pools == null) {
            log.debug("Processor not started, nothing to start");
            return false;
        }
        synchronized (pools) {
            ComputationPool pool = pools.stream()
                                        .filter(comp -> computation.getUrn().equals(comp.getComputationName()))
                                        .findFirst()
                                        .orElse(null);
            if (pool == null) {
                log.debug("Unknown computation, nothing to restart");
                return false;
            }
            if (!pool.isTerminated()) {
                log.debug("Computation is already started");
                return false;
            }

            pools.remove(pool);
            log.warn("Starting computation thread pool: " + computation);
            ComputationMetadataMapping meta = topology.metadataList()
                                                      .stream()
                                                      .filter(m -> m.name().equals(computation.getUrn()))
                                                      .findFirst()
                                                      .orElseThrow();
            pool = new ComputationPool(topology.getSupplier(meta.name()), meta, getDefaultAssignments(meta),
                    streamManager, settings.getPolicy(meta.name()));
            pools.add(pool);
            pool.start();
        }
        return true;
    }

    protected String getEdgeName(Topology.Vertex edge) {
        return (edge.getType().equals(Topology.VertexType.COMPUTATION) ? "computation:" : "stream:") + edge.getName();
    }

    @Override
    public boolean stop(Duration timeout) {
        log.debug("Stopping ...");
        if (pools == null) {
            return true;
        }
        long failures = pools.parallelStream().filter(comp -> !comp.stop(timeout)).count();
        log.debug(String.format("Stopped %d failure", failures));
        return failures == 0;
    }

    @Override
    public boolean drainAndStop(Duration timeout) {
        // here the order matters, this must be done sequentially
        log.debug("Drain and stop");
        if (pools == null) {
            return true;
        }
        long failures = pools.stream().filter(comp -> !comp.drainAndStop(timeout)).count();
        log.debug(String.format("Drained and stopped %d failure", failures));
        return failures == 0;
    }

    @Override
    public void shutdown() {
        log.debug("Shutdown ...");
        if (pools == null) {
            return;
        }
        pools.parallelStream().forEach(ComputationPool::shutdown);
        log.debug("Shutdown done");
    }

    @Override
    public long getLowWatermark() {
        Map<String, Long> watermarks = new HashMap<>(pools.size());
        Set<String> roots = topology.getRoots();
        Map<String, Long> watermarkTrees = new HashMap<>(roots.size());
        // compute low watermark for each tree of computation
        pools.forEach(pool -> watermarks.put(pool.getComputationName(), pool.getLowWatermark()));
        for (String root : roots) {
            watermarkTrees.put(root,
                    topology.getDescendantComputationNames(root).stream().mapToLong(watermarks::get).min().orElse(0));
        }
        // return the minimum wm for all trees that are not 0
        long ret = watermarkTrees.values().stream().filter(wm -> wm > 1).mapToLong(Long::valueOf).min().orElse(0);
        if (log.isTraceEnabled()) {
            log.trace("lowWatermark: " + ret);
            watermarkTrees.forEach((k, v) -> log.trace("tree " + k + ": " + v));
        }
        return ret;
    }

    @Override
    public Latency getLatency(String computationName) {
        Set<String> ancestorsComputations = topology.getAncestorComputationNames(computationName);
        ancestorsComputations.add(computationName);
        List<Latency> latencies = new ArrayList<>();
        ancestorsComputations.forEach(
                comp -> topology.getMetadata(comp)
                                .inputStreams()
                                .forEach(stream -> latencies.add(manager.getLatency(Name.ofUrn(stream),
                                        Name.ofUrn(comp), settings.getCodec(comp),
                                        (rec -> Watermark.ofValue(rec.getWatermark()).getTimestamp()),
                                        (Record::getKey)))));
        return Latency.of(latencies);
    }

    @Override
    public long getLowWatermark(String computationName) {
        Objects.requireNonNull(computationName);
        // the low wm for a computation is the minimum watermark for all its ancestors
        Map<String, Long> watermarks = new HashMap<>(pools.size());
        pools.forEach(pool -> watermarks.put(pool.getComputationName(), pool.getLowWatermark()));
        long ret = topology.getAncestorComputationNames(computationName)
                           .stream()
                           .mapToLong(watermarks::get)
                           .min()
                           .orElse(0);
        ret = min(ret, watermarks.get(computationName));
        return ret;
    }

    @Override
    public boolean isDone(long timestamp) {
        return Watermark.ofValue(getLowWatermark()).isDone(timestamp);
    }

    protected List<ComputationPool> initPools() {
        log.debug("Initializing pools");
        return topology.metadataList()
                       .stream()
                       .map(meta -> new ComputationPool(topology.getSupplier(meta.name()), meta,
                               getDefaultAssignments(meta), streamManager, settings.getPolicy(meta.name())))
                       .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected Codec<Record> getCodecForStreams(String name, Set<String> streams) {
        Codec<Record> codec = null;
        Set<String> codecNames = new HashSet<>();
        for (String stream : streams) {
            codec = settings.getCodec(stream);
            codecNames.add(codec == null ? "none" : codec.getName());
        }
        if (codecNames.size() > 1) {
            throw new IllegalArgumentException(String.format("Different codecs for computation %s: %s", name,
                    Arrays.toString(codecNames.toArray())));
        }
        if (codec == null) {
            codec = NO_CODEC;
        }
        return codec;
    }

    protected List<List<LogPartition>> getDefaultAssignments(ComputationMetadataMapping meta) {
        int threads = settings.getConcurrency(meta.name());
        if (threads == 0) {
            return Collections.emptyList();
        }
        Map<String, Integer> streams = new HashMap<>();
        meta.inputStreams().forEach(streamName -> streams.put(streamName, settings.getPartitions(streamName)));
        return KafkaUtils.roundRobinAssignments(threads, streams);
    }
}
