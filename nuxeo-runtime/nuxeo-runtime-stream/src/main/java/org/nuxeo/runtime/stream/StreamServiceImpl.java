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

import org.nuxeo.common.Environment;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamProcessor;
import org.nuxeo.lib.stream.log.LogManager;
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

    public static final String NUXEO_STREAM_DIR_PROP = "nuxeo.stream.chronicle.dir";

    public static final String NUXEO_STREAM_RET_DURATION_PROP = "nuxeo.stream.chronicle.retention.duration";

    protected static final String XP_LOG_CONFIG = "logConfig";

    protected static final String XP_STREAM_PROCESSOR = "streamProcessor";

    protected final Map<String, LogManager> managers = new HashMap<>();

    protected final Map<String, StreamProcessor> processors = new HashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        // start after kafka config service
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER + 10;
    }

    @Override
    public LogManager getLogManager(String name) {
        // TODO: returns a wrapper that don't expose the LogManager#close
        if (!managers.containsKey(name)) {
            LogConfigDescriptor config = getDescriptor(XP_LOG_CONFIG, name);
            if (config == null) {
                throw new IllegalArgumentException("Unknown logConfig: " + name);
            }
            if ("kafka".equalsIgnoreCase(config.type)) {
                managers.put(name, createKafkaLogManager(config));
            } else {
                managers.put(name, createChronicleLogManager(config));
            }
        }
        return managers.get(name);
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

    protected void createStreamIfNotExists(LogConfigDescriptor config) {
        if (config.logs.isEmpty()) {
            return;
        }
        LogManager manager = getLogManager(config.getId());
        config.logs.forEach(l -> {
            getLog().info("Create if not exists stream: " + l.getId() + " with manager: " + config.getId());
            manager.createIfNotExists(l.getId(), l.size);
        });
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<LogConfigDescriptor> logDescs = getDescriptors(XP_LOG_CONFIG);
        logDescs.forEach(this::createStreamIfNotExists);
        List<StreamProcessorDescriptor> streamDescs = getDescriptors(XP_STREAM_PROCESSOR);
        streamDescs.forEach(this::initProcessor);
        new ComponentsLifeCycleListener().install();
    }

    protected void initProcessor(StreamProcessorDescriptor descriptor) {
        if (processors.containsKey(descriptor.getId())) {
            getLog().error("Processor already initialized: " + descriptor.getId());
            return;
        }
        getLog().info("Init Stream processor: " + descriptor.getId() + " with manager: " + descriptor.config);
        LogManager manager = getLogManager(descriptor.config);
        Topology topology;
        try {
            topology = descriptor.klass.getDeclaredConstructor().newInstance().getTopology(descriptor.options);
        } catch (ReflectiveOperationException e) {
            throw new StreamRuntimeException("Can not create topology for processor: " + descriptor.getId(), e);
        }
        StreamProcessor streamProcessor = new LogStreamProcessor(manager);
        Settings settings = getSettings(descriptor);
        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    "Starting computation topology: " + descriptor.getId() + "\n" + topology.toPlantuml(settings));
        }
        streamProcessor.init(topology, settings);
        processors.put(descriptor.getId(), streamProcessor);
    }

    protected Settings getSettings(StreamProcessorDescriptor descriptor) {
        CodecService codecService = Framework.getService(CodecService.class);
        Codec<Record> actualCodec = descriptor.defaultCodec == null ? null
                : codecService.getCodec(descriptor.defaultCodec, Record.class);
        Settings settings = new Settings(descriptor.defaultConcurrency, descriptor.defaultPartitions, actualCodec);
        descriptor.computations.forEach(comp -> settings.setConcurrency(comp.name, comp.concurrency));
        descriptor.streams.forEach(stream -> settings.setPartitions(stream.name, stream.partitions));
        descriptor.streams.stream().filter(stream -> Objects.nonNull(stream.codec)).forEach(
                stream -> settings.setCodec(stream.name, codecService.getCodec(stream.codec, Record.class)));
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
            StreamProcessor manager = processors.get(d.getId());
            if (manager != null) {
                manager.start();
            }
        });
    }

    protected void stopComputations() {
        processors.forEach((name, manager) -> manager.stop(Duration.ofSeconds(1)));
        processors.clear();
    }

    protected void closeLogManagers() {
        managers.values().stream().filter(Objects::nonNull).forEach(LogManager::close);
        managers.clear();
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
