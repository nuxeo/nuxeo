/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *      Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.stream.Collectors;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.jvm.BufferPoolMetricSet;
import io.dropwizard.metrics5.jvm.FileDescriptorRatioGauge;
import io.dropwizard.metrics5.jvm.GarbageCollectorMetricSet;
import io.dropwizard.metrics5.jvm.JmxAttributeGauge;
import io.dropwizard.metrics5.jvm.MemoryUsageGaugeSet;
import io.dropwizard.metrics5.jvm.ThreadStatesGaugeSet;
import io.dropwizard.metrics5.log4j2.InstrumentedAppender;

public class MetricsServiceImpl extends DefaultComponent implements MetricsService {

    private static final Logger log = LogManager.getLogger(MetricsServiceImpl.class);

    protected static final String CONFIGURATION_EP = "configuration";

    protected static final String REPORTER_EP = "reporter";

    protected MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter instanceUp = registry.counter(MetricRegistry.name("nuxeo", "instance-up"));

    protected MetricsConfigurationDescriptor config;

    protected List<MetricsReporterDescriptor> reporterConfigs;

    protected List<MetricsReporter> reporters;

    protected InstrumentedAppender appender;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        log.debug("Activating component");
        SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.debug("Deactivating component");
        SharedMetricRegistries.remove(MetricsService.class.getName());
        super.deactivate(context);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        log.debug("Starting component");
        instanceUp.inc();
        config = getDescriptor(CONFIGURATION_EP, UNIQUE_DESCRIPTOR_ID);
        startReporters();
    }

    @Override
    public void stop(ComponentContext context) {
        log.debug("Stopping component");
        stopReporters();
        instanceUp.dec();
    }

    protected boolean metricEnabled() {
        return config != null && config.isEnabled();
    }

    @Override
    public void startReporters() {
        if (!metricEnabled() || reporters != null) {
            log.debug("Metrics disabled or already started.");
            return;
        }
        log.info("Starting reporters");
        reporterConfigs = getDescriptors(REPORTER_EP);
        updateInstrumentation(config.getInstruments(), true);
        reporters = reporterConfigs.stream()
                                   .map(MetricsReporterDescriptor::newInstance)
                                   .collect(Collectors.toList());
        reporters.forEach(reporter -> reporter.start(registry, config, config.getDeniedExpansions()));
    }

    @Override
    public void stopReporters() {
        if (!metricEnabled() || reporters == null) {
            log.debug("Metrics disabled or already stopped.");
            return;
        }
        log.warn("Stopping reporters");
        reporters.forEach(MetricsReporter::stop);
        updateInstrumentation(config.getInstruments(), false);
        reporters.clear();
        reporters = null;
        reporterConfigs = null;
    }

    protected void updateInstrumentation(List<MetricsConfigurationDescriptor.InstrumentDescriptor> instruments, boolean activate) {
        for (String instrument : instruments.stream()
                                            .filter(MetricsConfigurationDescriptor.InstrumentDescriptor::isEnabled)
                                            .map(MetricsConfigurationDescriptor.InstrumentDescriptor::getId)
                                            .collect(Collectors.toList())) {
            switch (instrument) {
            case "log4j":
                instrumentLog4j(activate);
                break;
            case "tomcat":
                instrumentTomcat(activate);
                break;
            case "jvm":
                instrumentJvm(activate);
                break;
            default:
                log.warn("Ignoring unknown instrumentation: " + instrument);
            }
        }
    }

    protected void instrumentLog4j(boolean activate) {
        if (activate) {
            appender = new InstrumentedAppender(registry, null, null, false);
            appender.start();
            @SuppressWarnings("resource") // not ours to close
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            config.getLoggerConfig(ROOT_LOGGER_NAME).addAppender(appender, INFO, null);
            context.updateLoggers(config);
        } else if (appender != null) {
            try {
                @SuppressWarnings("resource") // not ours to close
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                Configuration config = context.getConfiguration();
                config.getLoggerConfig(ROOT_LOGGER_NAME).removeAppender(appender.getName());
                context.updateLoggers(config);
            } finally {
                appender = null;
            }
        }
    }

    protected void registerTomcatGauge(String mbean, String attribute, MetricRegistry registry, String name) {
        try {
            registry.register(MetricRegistry.name("tomcat", name),
                    new JmxAttributeGauge(new ObjectName(mbean), attribute));
        } catch (MalformedObjectNameException | IllegalArgumentException e) {
            throw new UnsupportedOperationException("Cannot compute object name of " + mbean, e);
        }
    }

    protected void instrumentTomcat(boolean activate) {
        if (activate) {
            // TODO: do not hard code the common datasource
            String pool = "org.nuxeo.ecm.core.management.jtajca:type=ConnectionPoolMonitor,name=jdbc/nuxeo";
            String connector = String.format("Catalina:type=ThreadPool,name=\"http-nio-%s-%s\"",
                    Framework.getProperty("nuxeo.bind.address", "0.0.0.0"),
                    Framework.getProperty("nuxeo.bind.port", "8080"));
            String requestProcessor = String.format("Catalina:type=GlobalRequestProcessor,name=\"http-nio-%s-%s\"",
                    Framework.getProperty("nuxeo.bind.address", "0.0.0.0"),
                    Framework.getProperty("nuxeo.bind.port", "8080"));
            String manager = "Catalina:type=Manager,host=localhost,context=/nuxeo";
            registerTomcatGauge(pool, "ConnectionCount", registry, "jdbc-numActive");
            registerTomcatGauge(pool, "IdleConnectionCount", registry, "jdbc-numIdle");
            registerTomcatGauge(connector, "currentThreadCount", registry, "currentThreadCount");
            registerTomcatGauge(connector, "currentThreadsBusy", registry, "currentThreadBusy");
            registerTomcatGauge(requestProcessor, "errorCount", registry, "errorCount");
            registerTomcatGauge(requestProcessor, "requestCount", registry, "requestCount");
            registerTomcatGauge(requestProcessor, "processingTime", registry, "processingTime");
            registerTomcatGauge(requestProcessor, "bytesReceived", registry, "bytesReceived");
            registerTomcatGauge(requestProcessor, "bytesSent", registry, "bytesSent");
            registerTomcatGauge(manager, "activeSessions", registry, "activeSessions");
        } else {
            registry.removeMatching((name, metric) -> name.getKey().startsWith("tomcat."));
        }
    }

    protected void instrumentJvm(boolean activate) {
        if (activate) {
            registry.register("jvm.memory", new MemoryUsageGaugeSet());
            registry.register("jvm.garbage", new GarbageCollectorMetricSet());
            registry.register("jvm.threads", new ThreadStatesGaugeSet());
            registry.register("jvm.files", new FileDescriptorRatioGauge());
            registry.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        } else {
            registry.removeMatching((name, metric) -> name.getKey().startsWith("jvm."));
        }
    }
}
