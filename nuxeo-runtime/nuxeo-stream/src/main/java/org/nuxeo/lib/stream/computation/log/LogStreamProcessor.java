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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;

/**
 * @since 9.3
 */
public class LogStreamProcessor implements StreamProcessor {
    private static final Log log = LogFactory.getLog(LogStreamProcessor.class);

    protected final LogManager manager;

    protected Topology topology;

    protected Settings settings;

    protected List<ComputationPool> pools;

    public LogStreamProcessor(LogManager manager) {
        this.manager = manager;
    }

    @Override
    public StreamProcessor init(Topology topology, Settings settings) {
        log.debug("Initializing ...");
        this.topology = topology;
        this.settings = settings;
        initStreams();
        initSourceAppenders();
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
        long ret = watermarkTrees.values().stream().filter(wm -> wm > 1).mapToLong(Long::new).min().orElse(0);
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
                                .forEach(stream -> latencies.add(
                                        manager.getLatency(stream, comp, settings.getCodec(comp),
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
                               getDefaultAssignments(meta), manager,
                               getCodecForStreams(meta.name(), meta.inputStreams()),
                               getCodecForStreams(meta.name(), meta.outputStreams())))
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
        Map<String, Integer> streams = new HashMap<>();
        meta.inputStreams().forEach(streamName -> streams.put(streamName, settings.getPartitions(streamName)));
        return KafkaUtils.roundRobinAssignments(threads, streams);
    }

    protected void initStreams() {
        log.debug("Initializing streams");
        topology.streamsSet().forEach(streamName -> {
            if (manager.exists(streamName)) {
                int size = manager.size(streamName);
                if (settings.getPartitions(streamName) != size) {
                    log.debug(String.format(
                            "Update settings for stream: %s defined with %d partitions but exists with %d partitions",
                            streamName, settings.getPartitions(streamName), size));
                    settings.setPartitions(streamName, size);
                }
            } else {
                manager.createIfNotExists(streamName, settings.getPartitions(streamName));
            }
        });
    }

    protected void initSourceAppenders() {
        log.debug("Initializing source appenders so we ensure they use codec defined in the processor");
        topology.streamsSet()
                .forEach(sourceStream -> manager.getAppender(sourceStream, settings.getCodec(sourceStream)));
    }

}
