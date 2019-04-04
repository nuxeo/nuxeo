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
package org.nuxeo.runtime.stream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kafka.KafkaConfigService;
import org.nuxeo.runtime.kafka.KafkaConfigServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 9.3
 */
public class StreamServiceImpl extends DefaultComponent implements StreamService {
    public static final String NUXEO_STREAM_DIR_PROP = "nuxeo.stream.chronicle.dir";

    public static final String NUXEO_STREAM_RET_DURATION_PROP = "nuxeo.stream.chronicle.retention.duration";

    protected static final String LOG_CONFIG_XP = "logConfig";

    protected static final String STREAM_PROCESSOR_XP = "streamProcessor";

    private static final Log log = LogFactory.getLog(StreamServiceImpl.class);

    protected final Map<String, LogConfigDescriptor> configs = new HashMap<>();

    protected final Map<String, LogManager> logManagers = new HashMap<>();

    protected final Map<String, StreamManager> streamManagers = new HashMap<>();

    protected final Map<String, StreamProcessor> processors = new HashMap<>();

    protected final Map<String, StreamProcessorDescriptor> processorDescriptors = new HashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        // start after kafka config service
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER + 10;
    }

    @Override
    public LogManager getLogManager(String name) {
        // TODO: return a wrapper that don't expose the LogManager#close
        if (!logManagers.containsKey(name)) {
            if (!configs.containsKey(name)) {
                throw new IllegalArgumentException("Unknown logConfig: " + name);
            }
            LogConfigDescriptor config = configs.get(name);
            if (config.isKafkaLog()) {
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
        String kafkaConfig = config.getOption("kafkaConfig", "default");
        KafkaConfigService service = Framework.getService(KafkaConfigService.class);
        return new KafkaLogManager(service.getTopicPrefix(kafkaConfig), service.getProducerProperties(kafkaConfig),
                service.getConsumerProperties(kafkaConfig));
    }

    protected LogManager createChronicleLogManager(LogConfigDescriptor config) {
        String basePath = config.getOption("basePath", null);
        String directory = config.getOption("directory", config.getName());
        Path path = getChroniclePath(basePath, directory);
        String retention = getChronicleRetention(config.getOption("retention", null));
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

    protected void createLogsIfNotExists(LogConfigDescriptor config) {
        if (config.getLogsToCreate().isEmpty()) {
            return;
        }
        LogManager manager = getLogManager(config.getName());
        config.getLogsToCreate().forEach((stream, size) -> {
            log.info("Create if not exists stream: " + stream + " with manager: " + config.getName());
            manager.createIfNotExists(stream, size);
        });
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        configs.values().forEach(this::createLogsIfNotExists);
        processorDescriptors.values().forEach(this::initProcessor);
        new ComponentsLifeCycleListener().install();
    }

    protected void initProcessor(StreamProcessorDescriptor descriptor) {
        if (processors.containsKey(descriptor.getName())) {
            log.error("Processor already initialized: " + descriptor.getName());
            return;
        }
        log.info("Init Stream processor: " + descriptor.getName() + " with manager: " + descriptor.config);
        LogManager manager = getLogManager(descriptor.config);
        StreamManager streamManager = getStreamManager(descriptor.config);
        Topology topology;
        try {
            topology = descriptor.klass.getDeclaredConstructor().newInstance().getTopology(descriptor.options);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Can not create topology for processor: " + descriptor.getName(), e);
        }
        Settings settings = getSettings(descriptor);
        if (log.isDebugEnabled()) {
            log.debug("Starting computation topology: " + descriptor.getName() + "\n" + topology.toPlantuml(settings));
        }
        StreamProcessor streamProcessor = streamManager.registerAndCreateProcessor(descriptor.getName(), topology, settings);
        processors.put(descriptor.getName(), streamProcessor);
    }

    protected Settings getSettings(StreamProcessorDescriptor descriptor) {
        Settings settings = new Settings(descriptor.defaultConcurrency, descriptor.defaultPartitions);
        descriptor.computations.forEach(comp -> settings.setConcurrency(comp.name, comp.concurrency));
        for (StreamProcessorDescriptor.StreamDescriptor streamDescriptor : descriptor.streams) {
            settings.setPartitions(streamDescriptor.name, streamDescriptor.partitions);
            streamDescriptor.filters.forEach(filter -> settings.addFilter(streamDescriptor.name, filter.getFilter()));
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
        processorDescriptors.keySet().forEach(name -> {
            StreamProcessor manager = processors.get(name);
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
        logManagers.values().stream().filter(Objects::nonNull).forEach(LogManager::close);
        logManagers.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(LOG_CONFIG_XP)) {
            LogConfigDescriptor descriptor = (LogConfigDescriptor) contribution;
            configs.put(descriptor.name, descriptor);
            log.debug(String.format("Register logConfig: %s", descriptor.name));
        } else if (extensionPoint.equals(STREAM_PROCESSOR_XP)) {
            StreamProcessorDescriptor descriptor = (StreamProcessorDescriptor) contribution;
            processorDescriptors.put(descriptor.name, descriptor);
            log.debug(String.format("Register Stream StreamProcessorTopologyProcessor: %s", descriptor.name));
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(LOG_CONFIG_XP)) {
            LogConfigDescriptor descriptor = (LogConfigDescriptor) contribution;
            configs.remove(descriptor.name);
            log.debug(String.format("Unregister logConfig: %s", descriptor.name));
        } else if (extensionPoint.equals(STREAM_PROCESSOR_XP)) {
            StreamProcessorDescriptor descriptor = (StreamProcessorDescriptor) contribution;
            processorDescriptors.remove(descriptor.name);
            log.debug(String.format("Unregister Stream StreamProcessorTopologyProcessor: %s", descriptor.name));
        }
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
