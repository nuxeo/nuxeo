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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.RecordFilter;
import org.nuxeo.lib.stream.computation.RecordFilterChain;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.internals.RecordFilterChainImpl;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;

/**
 * StreamManager based on a LogManager
 *
 * @since 11.1
 */
public class LogStreamManager implements StreamManager {
    private static final Log log = LogFactory.getLog(LogStreamManager.class);

    // Internal stream to describe started processors
    // @since 11.5
    public static final String PROCESSORS_STREAM = "internal/processors";

    public static final String METRICS_STREAM = "internal/metrics";

    public static final Codec<Record> INTERNAL_CODEC = new AvroMessageCodec<>(Record.class);

    protected final LogManager logManager;

    public LogStreamManager(LogManager logManager) {
        this.logManager = logManager;
        initInternalStreams();
    }

    protected void initInternalStreams() {
        initInternalStream(Name.ofUrn(PROCESSORS_STREAM));
        initInternalStream(Name.ofUrn(METRICS_STREAM));
    }

    protected void initInternalStream(Name stream) {
        logManager.createIfNotExists(stream, 1);
        logManager.getAppender(stream, INTERNAL_CODEC);
        filters.put(stream, RecordFilterChainImpl.NONE);
    }

    // processorName -> processor
    protected final Map<String, StreamProcessor> processors = new HashMap<>();

    // processorName -> topology
    protected final Map<String, Topology> topologies = new HashMap<>();

    // processorName -> settings
    protected final Map<String, Settings> settings = new HashMap<>();

    // stream -> filter
    protected final Map<Name, RecordFilterChain> filters = new HashMap<>();

    protected final Set<Name> streams = new HashSet<>();

    @Override
    public void register(String processorName, Topology topology, Settings settings) {
        log.debug("Register processor: " + processorName);
        topologies.put(processorName, topology);
        this.settings.put(processorName, settings);
        initStreams(topology, settings);
        initAppenders(topology.streamsSet(), settings);
        registerFilters(topology.streamsSet(), settings);
    }

    @Override
    public void register(List<String> streams, Settings settings) {
        streams.forEach(stream -> initStream(stream, settings));
        initAppenders(streams, settings);
        registerFilters(streams, settings);
    }

    @Override
    public StreamProcessor createStreamProcessor(String processorName) {
        if (!topologies.containsKey(processorName)) {
            throw new IllegalArgumentException("Unregistered processor name: " + processorName);
        }
        LogStreamProcessor processor = new LogStreamProcessor(this);
        processor.init(topologies.get(processorName), settings.get(processorName));
        processors.put(processorName, processor);
        Map<String, String> meta = new HashMap<>();
        meta.put("processorName",  processorName);
        meta.putAll(getSystemMetadata());
        append(PROCESSORS_STREAM, Record.of(meta.get("ip"), processor.toJson(meta).getBytes(UTF_8)));
        return processor;
    }

    protected Map<String, String> getSystemMetadata() {
        Map<String, String> systemMetadata = new HashMap<>();
        try {
            InetAddress host = InetAddress.getLocalHost();
            systemMetadata.put("ip", host.getHostAddress());
            systemMetadata.put("hostname", host.getHostName());
        } catch (UnknownHostException e) {
            systemMetadata.put("ip", "unknown");
            systemMetadata.put("hostname", "unknown");
        }
        systemMetadata.put("cpuCores", String.valueOf(Runtime.getRuntime().availableProcessors()));
        systemMetadata.put("jvmHeapSize", String.valueOf(Runtime.getRuntime().maxMemory()));
        return systemMetadata;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    @Override
    public LogOffset append(String streamUrn, Record record) {
        Name stream = Name.ofUrn(streamUrn);
        RecordFilterChain filter = filters.get(stream);
        if (filter == null) {
            throw new IllegalArgumentException("Unknown stream: " + stream);
        }
        record = filter.beforeAppend(record);
        if (record == null) {
            return new LogOffsetImpl(stream, 0, 0);
        }
        LogOffset offset = logManager.getAppender(stream).append(record.getKey(), record);
        filter.afterAppend(record, offset);
        return offset;
    }

    @Override
    public Set<String> getProcessorNames() {
        return Collections.unmodifiableSet(processors.keySet());
    }

    @Override
    public StreamProcessor getProcessor(String processorName) {
        return processors.get(processorName);
    }

    @Override
    public void close() {
        processors.values().forEach(StreamProcessor::shutdown);
        processors.clear();
    }

    /**
     * Returns {@code true} if the {@link #subscribe} method is supported for the specific stream.
     *
     * @since 2021.34
     */
    public boolean supportSubscribe(Name stream) {
        return logManager.supportSubscribe(stream);
    }

    /**
     * Returns {@code true} if the {@link #subscribe} method is supported.
     * Now deprecated because some implementations support subscribe only on specific streams.
     *
     * @deprecated since 2021.34 use {@link #supportSubscribe(Name)} instead
     */
    public boolean supportSubscribe() {
        return logManager.supportSubscribe();
    }

    public LogTailer<Record> subscribe(Name computationName, Collection<Name> streams, RebalanceListener listener) {
        Codec<Record> codec = getCodec(streams);
        return logManager.subscribe(computationName, streams, listener, codec);
    }

    public LogTailer<Record> createTailer(Name computationName, Collection<LogPartition> streamPartitions) {
        if (streamPartitions.isEmpty()) {
            return logManager.createTailer(computationName, streamPartitions);
        }
        Codec<Record> codec = getCodec(streamPartitions.stream().map(LogPartition::name).collect(Collectors.toList()));
        return logManager.createTailer(computationName, streamPartitions, codec);
    }

    public RecordFilter getFilter(Name stream) {
        return filters.get(stream);
    }

    protected Codec<Record> getCodec(Collection<Name> streams) {
        Codec<Record> codec = null;
        for (Name stream : streams) {
            Codec<Record> sCodec = logManager.<Record> getAppender(stream).getCodec();
            if (codec == null) {
                codec = sCodec;
            } else if (!codec.getName().equals(sCodec.getName())) {
                throw new IllegalArgumentException("Different codec on input streams are not supported " + streams);
            }
        }
        return codec;
    }

    protected void initStreams(Topology topology, Settings settings) {
        log.debug("Initializing streams");
        topology.streamsSet().forEach(streamName -> initStream(streamName, settings));
    }

    protected void initStream(String streamName, Settings settings) {
        Name stream = Name.ofUrn(streamName);
        if (settings.isExternal(stream)) {
            return;
        }
        if (!logManager.exists(stream)) {
            logManager.createIfNotExists(stream, settings.getPartitions(streamName));
        } else {
            int size = logManager.size(stream);
            if (settings.getPartitions(streamName) != size) {
                log.debug(String.format(
                        "Update settings for stream: %s defined with %d partitions but exists with %d partitions",
                        streamName, settings.getPartitions(streamName), size));
                settings.setPartitions(streamName, size);
            }
        }
        streams.add(stream);
    }

    protected void initAppenders(Collection<String> streams, Settings settings) {
        log.debug("Initializing source appenders so we ensure they use codec defined in the processor:");
        streams.forEach(stream -> log.debug(stream));
        streams.stream()
               .filter(stream -> !settings.isExternal(Name.ofUrn(stream)))
               .forEach(stream -> logManager.getAppender(Name.ofUrn(stream), settings.getCodec(stream)));
    }

    protected void registerFilters(Collection<String> streams, Settings settings) {
        streams.stream()
               .filter(stream -> !settings.isExternal(Name.ofUrn(stream)))
               .forEach(stream -> filters.put(Name.ofUrn(stream), settings.getFilterChain(stream)));
    }

}
