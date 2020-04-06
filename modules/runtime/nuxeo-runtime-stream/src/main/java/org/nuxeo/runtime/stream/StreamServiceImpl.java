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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.runtime.api.Framework;
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

    protected final Map<String, LogManager> logManagers = new HashMap<>();

    protected final Map<String, StreamManager> streamManagers = new HashMap<>();

    protected final Map<String, StreamProcessor> processors = new HashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        // start after kafka config service
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER + 10;
    }

    @Override
    public LogManager getLogManager(String name) {
        // TODO: returns a wrapper that don't expose the LogManager#close
        if (!logManagers.containsKey(name)) {
            LogConfigDescriptor config = getDescriptor(XP_LOG_CONFIG, name);
            if (config == null || !config.isEnabled()) {
                throw new IllegalArgumentException("Unknown or disabled logConfig: " + name);
            }
            if ("kafka".equalsIgnoreCase(config.type)) {
                logManagers.put(name, createKafkaLogManager(config));
            } else {
                logManagers.put(name, createChronicleLogManager(config));
            }
        }
        return logManagers.get(name);
    }

    @Override
    public StreamManager getStreamManager(String name) {
        return streamManagers.computeIfAbsent(name, app -> new LogStreamManager(getLogManager(name)));
    }

    protected LogManager createKafkaLogManager(LogConfigDescriptor config) {
        String kafkaConfig = config.options.getOrDefault("kafkaConfig", "default");
        KafkaConfigService service = Framework.getService(KafkaConfigService.class);
        return new KafkaLogManager(service.getTopicPrefix(kafkaConfig), service.getProducerProperties(kafkaConfig),
                service.getConsumerProperties(kafkaConfig));
    }

    protected LogManager createChronicleLogManager(LogConfigDescriptor config) {
        String basePath = config.options.getOrDefault("basePath", null);
        String directory = config.options.getOrDefault("directory", config.getId());
        Path path = getChroniclePath(basePath, directory);
        String retention = getChronicleRetention(config.options.getOrDefault("retention", null));
        return new ChronicleLogManager(path, retention);
    }

    protected String getChronicleRetention(String retention) {
        return retention != null ? retention : Framework.getProperty(NUXEO_STREAM_RET_DURATION_PROP, "4d");
    }

    protected Path getChroniclePath(String basePath, String name) {
        if (basePath != null) {
            return Paths.get(basePath, name).toAbsolutePath();
        }
        basePath = Framework.getProperty(NUXEO_STREAM_DIR_PROP);
        if (basePath != null) {
            return Paths.get(basePath, name).toAbsolutePath();
        }
        basePath = Framework.getProperty(Environment.NUXEO_DATA_DIR);
        if (basePath != null) {
            return Paths.get(basePath, "stream", name).toAbsolutePath();
        }
        return Paths.get(Framework.getRuntime().getHome().getAbsolutePath(), "data", "stream", name).toAbsolutePath();
    }

    protected void createLogIfNotExists(LogConfigDescriptor config) {
        if (!config.isEnabled() || config.logs.isEmpty()) {
            return;
        }
        @SuppressWarnings("resource") // not ours to close
        LogManager manager = getLogManager(config.getId());
        config.logs.forEach(l -> {
            log.info("Create if not exists stream: {} with manager: {}", l.getId(), config.getId());
            manager.createIfNotExists(Name.ofUrn(l.getId()), l.size);
        });
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<LogConfigDescriptor> logDescs = getDescriptors(XP_LOG_CONFIG);
        logDescs.forEach(this::createLogIfNotExists);
        List<StreamProcessorDescriptor> streamDescs = getDescriptors(XP_STREAM_PROCESSOR);
        streamDescs.forEach(this::initProcessor);
        new ComponentsLifeCycleListener().install();
    }

    protected void initProcessor(StreamProcessorDescriptor descriptor) {
        if (! descriptor.isEnabled()) {
            log.info("Processor {} disabled", descriptor.getId());
            return;
        }
        if (processors.containsKey(descriptor.getId())) {
            log.error("Processor already initialized: {}", descriptor.getId());
            return;
        }
        log.info("Init Stream processor: {} with manager: {}", descriptor.getId(), descriptor.config);
        try {
            getLogManager(descriptor.config);
        } catch (IllegalArgumentException e) {
            log.error("Unknown logConfig: {}, on processor: {}", descriptor.config, descriptor.getId());
            throw e;
        }
        StreamManager streamManager = getStreamManager(descriptor.config);
        Topology topology;
        try {
            topology = descriptor.klass.getDeclaredConstructor().newInstance().getTopology(descriptor.options);
        } catch (ReflectiveOperationException e) {
            throw new StreamRuntimeException("Can not create topology for processor: " + descriptor.getId(), e);
        }
        Settings settings = getSettings(descriptor);
        log.debug("Starting computation topology: {}\n{}", descriptor::getId, () -> topology.toPlantuml(settings));
        if (descriptor.isStart()) {
            StreamProcessor streamProcessor = streamManager.registerAndCreateProcessor(descriptor.getId(), topology,
                    settings);
            processors.put(descriptor.getId(), streamProcessor);
        } else {
            streamManager.register(descriptor.getId(), topology, settings);
            processors.put(descriptor.getId(), null);
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
        stopComputations(); // should have already be done by the beforeStop listener
        closeLogManagers();
    }

    protected void startComputations() {
        getDescriptors(XP_STREAM_PROCESSOR).forEach(d -> {
            StreamProcessor processor = processors.get(d.getId());
            if (processor != null) {
                processor.start();
            }
        });
    }

    protected void stopComputations() {
        processors.forEach((name, processor) -> {
            if (processor != null) {
                processor.stop(Duration.ofSeconds(1));
            }
        });
        processors.clear();
    }

    protected void closeLogManagers() {
        logManagers.values().stream().filter(Objects::nonNull).forEach(LogManager::close);
        logManagers.clear();
    }

    protected class ComponentsLifeCycleListener implements ComponentManager.Listener {
        @Override
        public void afterStart(ComponentManager mgr, boolean isResume) {
            // this is called once all components are started and ready
            startComputations();
        }

        @Override
        public void beforeStop(ComponentManager mgr, boolean isStandby) {
            // this is called before components are stopped
            stopComputations();
            Framework.getRuntime().getComponentManager().removeListener(this);
        }
    }
}
