/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.stream;

import java.io.Externalizable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.LogConfig;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.UnifiedLogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogConfig;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;
import org.nuxeo.lib.stream.log.kafka.KafkaLogConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kafka.KafkaConfigService;
import org.nuxeo.runtime.kafka.KafkaConfigServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 9.3
 */
public class StreamServiceImpl extends DefaultComponent implements StreamService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(StreamServiceImpl.class);

    public static final String NUXEO_STREAM_DIR_PROP = "nuxeo.stream.chronicle.dir";

    public static final String NUXEO_STREAM_RET_DURATION_PROP = "nuxeo.stream.chronicle.retention.duration";

    public static final String DEFAULT_CODEC = "avro";

    protected static final String XP_LOG_CONFIG = "logConfig";

    protected static final String XP_STREAM_PROCESSOR = "streamProcessor";

    protected LogManager logManager;

    protected StreamManager streamManager;

    /**
     * @since 11.2
     */
    public static final String STREAM_PROCESSING_ENABLED = "nuxeo.stream.processing.enabled";

    protected Boolean isStreamProcessingDisabled;

    @Override
    public int getApplicationStartedOrder() {
        // start after kafka config service
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER + 10;
    }

    @Override
    public LogManager getLogManager() {
        // TODO: returns a wrapper that don't expose the LogManager#close
        return logManager;
    }

    @Override
    public StreamManager getStreamManager() {
        return streamManager;
    }

    protected String getChronicleRetention(String retention) {
        return retention != null ? retention : Framework.getProperty(NUXEO_STREAM_RET_DURATION_PROP, "4d");
    }

    protected Path getChroniclePath(String basePath) {
        if (basePath != null) {
            return Paths.get(basePath).toAbsolutePath();
        }
        basePath = Framework.getProperty(NUXEO_STREAM_DIR_PROP);
        if (basePath != null) {
            return Paths.get(basePath).toAbsolutePath();
        }
        basePath = Framework.getProperty(Environment.NUXEO_DATA_DIR);
        if (basePath != null) {
            return Paths.get(basePath, "stream").toAbsolutePath();
        }
        return Paths.get(Framework.getRuntime().getHome().getAbsolutePath(), "data", "stream").toAbsolutePath();
    }

    protected void createLogIfNotExists(LogConfigDescriptor config) {
        if (!config.isEnabled() || config.logs.isEmpty()) {
            return;
        }
        config.logs.forEach(l -> {
            log.info("Create if not exists stream: {} with manager: {}", l.getId(), config.getId());
            logManager.createIfNotExists(Name.ofUrn(l.getId()), l.size);
        });
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<LogConfig> configs = getLogConfigs();
        logManager = new UnifiedLogManager(configs);
        streamManager = new LogStreamManager(getNodeId(), logManager);
        List<LogConfigDescriptor> logDescs = getDescriptors(XP_LOG_CONFIG);
        logDescs.forEach(this::createLogIfNotExists);
        List<StreamProcessorDescriptor> streamDescs = getDescriptors(XP_STREAM_PROCESSOR);
        streamDescs.forEach(this::initProcessor);
        new ComponentsLifeCycleListener().install();
    }

    protected String getNodeId() {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService != null && clusterService.isEnabled()) {
            return clusterService.getNodeId();
        }
        return null;
    }

    protected List<LogConfig> getLogConfigs() {
        List<LogConfigDescriptor> logDescs = getDescriptors(XP_LOG_CONFIG);
        List<LogConfig> ret = new ArrayList<>(logDescs.size());
        for (LogConfigDescriptor desc : logDescs) {
            if (!desc.isEnabled() || desc.onlyLogDeclaration()) {
                continue;
            }
            if ("kafka".equalsIgnoreCase(desc.type)) {
                ret.add(createKafkaLogConfig(desc));
            } else {
                ret.add(createChronicleLogConfig(desc));
            }
        }
        return ret;
    }

    protected LogConfig createKafkaLogConfig(LogConfigDescriptor desc) {
        String kafkaConfig = desc.options.getOrDefault("kafkaConfig", "default");
        KafkaConfigService service = Framework.getService(KafkaConfigService.class);
        return new KafkaLogConfig(desc.getId(), desc.isDefault(), desc.getPatterns(),
                service.getTopicPrefix(kafkaConfig),
                service.getAdminProperties(kafkaConfig), service.getProducerProperties(kafkaConfig),
                service.getConsumerProperties(kafkaConfig));
    }

    protected LogConfig createChronicleLogConfig(LogConfigDescriptor desc) {
        String basePath = desc.options.getOrDefault("basePath", null);
        Path path = getChroniclePath(basePath);
        String retention = getChronicleRetention(desc.options.getOrDefault("retention", null));
        return new ChronicleLogConfig(desc.getId(), desc.isDefault(), desc.getPatterns(), path, retention);
    }

    protected void initProcessor(StreamProcessorDescriptor descriptor) {
        if (!descriptor.isEnabled()) {
            log.info("Processor {} disabled", descriptor.getId());
            return;
        }
        if (streamManager.getProcessorNames().contains(descriptor.getId())) {
            log.error("Processor already initialized: {}", descriptor.getId());
            return;
        }
        log.info("Init Stream processor: {}", descriptor.getId());
        Topology topology;
        try {
            topology = descriptor.klass.getDeclaredConstructor().newInstance().getTopology(descriptor.options);
        } catch (ReflectiveOperationException e) {
            throw new StreamRuntimeException("Can not create topology for processor: " + descriptor.getId(), e);
        }
        Settings settings = getSettings(descriptor);
        log.debug("Starting computation topology: {}\n{}", descriptor::getId, () -> topology.toPlantuml(settings));
        if (!isProcessingDisabled() && descriptor.isStart()) {
            streamManager.registerAndCreateProcessor(descriptor.getId(), topology, settings);
        } else {
            streamManager.register(descriptor.getId(), topology, settings);
        }
    }

    protected Settings getSettings(StreamProcessorDescriptor descriptor) {
        CodecService codecService = Framework.getService(CodecService.class);
        Codec<Record> actualCodec = descriptor.defaultCodec == null ? codecService.getCodec(DEFAULT_CODEC, Record.class)
                : codecService.getCodec(descriptor.defaultCodec, Record.class);
        Settings settings = new Settings(descriptor.defaultConcurrency, descriptor.defaultPartitions, actualCodec,
                descriptor.getDefaultPolicy(), null, descriptor.defaultExternal);
        descriptor.computations.forEach(comp -> settings.setConcurrency(comp.name, comp.concurrency));
        descriptor.policies.forEach(policy -> settings.setPolicy(policy.name, descriptor.getPolicy(policy.name)));
        for (StreamProcessorDescriptor.StreamDescriptor streamDescriptor : descriptor.streams) {
            settings.setPartitions(streamDescriptor.name,
                    streamDescriptor.partitions != null ? streamDescriptor.partitions : descriptor.defaultPartitions);
            if (streamDescriptor.codec != null) {
                settings.setCodec(streamDescriptor.name, codecService.getCodec(streamDescriptor.codec, Record.class));
            }
            streamDescriptor.filters.forEach(filter -> settings.addFilter(streamDescriptor.name, filter.getFilter()));
            settings.setExternal(Name.ofUrn(streamDescriptor.name),
                    streamDescriptor.external != null ? streamDescriptor.external : descriptor.defaultExternal);
        }
        return settings;
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        streamManager.close();
        logManager.close();
    }

    protected void startProcessors() {
        log.debug("Start processors");
        getDescriptors(XP_STREAM_PROCESSOR).forEach(d -> {
            StreamProcessor processor = streamManager.getProcessor(d.getId());
            if (processor != null && ((StreamProcessorDescriptor) d).isStart()) {
                processor.start();
            }
        });
    }

    @Override
    public void stopProcessors() {
        log.debug("Stop processors");
        getDescriptors(XP_STREAM_PROCESSOR).forEach(d -> {
            StreamProcessor processor = streamManager.getProcessor(d.getId());
            if (processor != null) {
                processor.stop(Duration.ofSeconds(1));
            }
        });
    }

    @Override
    public boolean startProcessor(String processorName) {
        if (isProcessingDisabled()) {
            return false;
        }
        StreamProcessor processor = streamManager.getProcessor(processorName);
        if (processor == null) {
            processor = streamManager.createStreamProcessor(processorName);
        }
        processor.start();
        return true;
    }

    @Override
    public boolean stopProcessor(String processorName) {
        StreamProcessor processor = streamManager.getProcessor(processorName);
        if (processor == null) {
            return false;
        }
        return processor.stop(Duration.ofSeconds(1));
    }

    @Override
    public boolean stopComputation(Name computation) {
        log.debug("Stop computation: {}", computation);
        return streamManager.getProcessorNames()
                            .stream()
                            .anyMatch(name -> streamManager.getProcessor(name) != null
                                    && streamManager.getProcessor(name).stopComputation(computation));
    }

    @Override
    public boolean restartComputation(Name computation) {
        log.debug("Restart computation: {}", computation);
        return streamManager.getProcessorNames()
                            .stream()
                            .anyMatch(name -> streamManager.getProcessor(name) != null
                                    && streamManager.getProcessor(name).startComputation(computation));
    }

    @Override
    public boolean setComputationPositionToEnd(Name computation, Name stream) {
        log.debug("Set computation position for {} to end", computation);
        try (LogTailer<Externalizable> tailer = logManager.createTailer(computation, stream)) {
            tailer.toEnd();
            tailer.commit();
        } catch (CommitFailedException e) {
            if (e.getMessage().contains("assigned the partitions to another member")) {
                // existing consumers prevent to change position
                return false;
            }
            throw e;
        }
        return true;
    }

    @Override
    public boolean setComputationPositionToBeginning(Name computation, Name stream) {
        log.debug("Set computation position for {} to beginning", computation);
        try (LogTailer<Externalizable> tailer = logManager.createTailer(computation, stream)) {
            tailer.reset();
            tailer.commit();
        } catch (CommitFailedException e) {
            if (e.getMessage().contains("assigned the partitions to another member")) {
                // existing consumers prevent to change position
                return false;
            }
            throw e;
        }
        return true;
    }

    @Override
    public boolean setComputationPositionToOffset(Name computation, Name stream, int partition, long offset) {
        log.debug("Set computation position for {} to partition: {}, offset: {}", computation, partition, offset);
        try (LogTailer<Externalizable> tailer = logManager.createTailer(computation, stream)) {
            tailer.seek(new LogOffsetImpl(stream, partition, offset));
            tailer.commit();
        } catch (CommitFailedException e) {
            if (e.getMessage().contains("assigned the partitions to another member")) {
                // existing consumers prevent to change position
                return false;
            }
            throw e;
        }
        return true;
    }

    @Override
    public boolean setComputationPositionAfterDate(Name computation, Name stream, Instant after) {
        log.debug("Set computation position for {} after date: {}", computation, after);
        try (LogTailer<Externalizable> tailer = logManager.createTailer(computation, stream)) {
            boolean moved = false;
            for (LogPartition partition: tailer.assignments()) {
                LogOffset offset = tailer.offsetForTimestamp(partition, after.toEpochMilli());
                if (offset != null) {
                    tailer.seek(offset);
                    moved = true;
                }
            }
            if (moved) {
                tailer.commit();
                return true;
            }
        } catch (CommitFailedException e) {
            if (e.getMessage().contains("assigned the partitions to another member")) {
                // existing consumers prevent to change position
                return false;
            }
            throw e;
        }
        return false;
    }

    protected class ComponentsLifeCycleListener implements ComponentManager.Listener {
        @Override
        public void afterStart(ComponentManager mgr, boolean isResume) {
            // this is called once all components are started and ready
            log.debug("afterStart");
            startProcessors();
        }

        @Override
        public void beforeStop(ComponentManager mgr, boolean isStandby) {
            // this is called before components are stopped
            log.debug("beforeStop");
            stopProcessors();
            Framework.getRuntime().getComponentManager().removeListener(this);
        }
    }

    protected boolean isProcessingDisabled() {
        if (isStreamProcessingDisabled == null) {
            if (Framework.isBooleanPropertyFalse(STREAM_PROCESSING_ENABLED)) {
                log.warn("Stream Processing has been disabled on this node");
                isStreamProcessingDisabled = true;
            } else {
                isStreamProcessingDisabled = false;
            }
        }
        return isStreamProcessingDisabled;
    }
}
