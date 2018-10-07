/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.log4j2.InstrumentedAppender;
import com.readytalk.metrics.StatsDReporter;

@XObject("metrics")
public class MetricsDescriptor implements Serializable {

    private static final long serialVersionUID = 7833869486922092460L;

    public MetricsDescriptor() {
        super();
        graphiteReporter = new GraphiteDescriptor();
        csvReporter = new CsvDescriptor();
        tomcatInstrumentation = new TomcatInstrumentationDescriptor();
        log4jInstrumentation = new Log4jInstrumentationDescriptor();
    }

    @XObject(value = "graphiteReporter")
    public static class GraphiteDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.graphite.enabled";

        public static final String HOST_PROPERTY = "metrics.graphite.host";

        public static final String PORT_PROPERTY = "metrics.graphite.port";

        public static final String PERIOD_PROPERTY = "metrics.graphite.period";

        public static final String PREFIX_PROPERTY = "metrics.graphite.prefix";

        /**
         * A list of metric prefixes that if defined should be kept reported, separated by commas
         *
         * @since 9.3
         */
        public static final String ALLOWED_METRICS_PROPERTY = "metrics.graphite.allowedMetrics";

        /**
         * A list of metric prefixes that if defined should not be reported, separated by commas
         *
         * @since 9.3
         */
        public static final String DENIED_METRICS_PROPERTY = "metrics.graphite.deniedMetrics";

        /**
         * @since 9.3
         */
        public static final String DEFAULT_ALLOWED_METRICS = "nuxeo.cache.user-entry-cache,nuxeo.cache.group-entry-cache,nuxeo.directories.userDirectory,nuxeo.directories.groupDirectory";

        /**
         * @since 9.3
         */
        public static final String DEFAULT_DENIED_METRICS = "nuxeo.cache,nuxeo.directories";

        /**
         * @since 9.3
         */
        public static final String ALL_METRICS = "ALL";

        @XNode("@enabled")
        protected Boolean enabled = Boolean.valueOf(Framework.getProperty(ENABLED_PROPERTY, "false"));

        @XNode("@host")
        public String host = Framework.getProperty(HOST_PROPERTY, "0.0.0.0");

        @XNode("@port")
        public Integer port = Integer.valueOf(Framework.getProperty(PORT_PROPERTY, "2030"));

        @XNode("@periodInSecond")
        public Integer period = Integer.valueOf(Framework.getProperty(PERIOD_PROPERTY, "10"));

        @XNode("@prefix")
        public String prefix = getPrefix();

        /**
         * A list of metric prefixes that if defined should be kept reported
         *
         * @since 9.3
         */
        @XNodeList(value = "allowedMetrics/metric", type = ArrayList.class, componentType = String.class)
        public List<String> allowedMetrics = Arrays.asList(
                Framework.getProperty(ALLOWED_METRICS_PROPERTY, DEFAULT_ALLOWED_METRICS).split(","));

        /**
         * A list of metric prefixes that if defined should not be reported
         *
         * @since 9.3
         */
        @XNodeList(value = "deniedMetrics/metric", type = ArrayList.class, componentType = String.class)
        public List<String> deniedMetrics = Arrays.asList(
                Framework.getProperty(DENIED_METRICS_PROPERTY, DEFAULT_DENIED_METRICS).split(","));

        public String getPrefix() {
            if (prefix == null) {
                prefix = Framework.getProperty(PREFIX_PROPERTY, "servers.${hostname}.nuxeo");
            }
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName().split("\\.")[0];
            } catch (UnknownHostException e) {
                hostname = "unknown";
            }
            return prefix.replace("${hostname}", hostname);
        }

        public boolean filter(String name) {
            return allowedMetrics.stream().anyMatch(f -> ALL_METRICS.equals(f) || name.startsWith(f))
                    || deniedMetrics.stream().noneMatch(f -> ALL_METRICS.equals(f) || name.startsWith(f));
        }

        @Override
        public String toString() {
            return String.format("graphiteReporter %s prefix: %s, host: %s, port: %d, period: %d",
                    enabled ? "enabled" : "disabled", prefix, host, port, period);
        }

        protected GraphiteReporter reporter;

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }

            InetSocketAddress address = new InetSocketAddress(host, port);
            Graphite graphite = new Graphite(address);
            reporter = GraphiteReporter.forRegistry(registry)
                                       .convertRatesTo(TimeUnit.SECONDS)
                                       .convertDurationsTo(TimeUnit.MICROSECONDS)
                                       .prefixedWith(getPrefix())
                                       .filter((name, metric) -> filter(name))
                                       .build(graphite);
            reporter.start(period, TimeUnit.SECONDS);
        }

        public void disable(MetricRegistry registry) {
            if (reporter == null) {
                return;
            }
            try {
                reporter.stop();
            } finally {
                reporter = null;
            }
        }
    }

    @XObject(value = "csvReporter")
    public static class CsvDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.csv.enabled";

        public static final String PERIOD_PROPERTY = "metrics.csv.period";

        public static final String OUTPUT_PROPERTY = "metrics.csv.output";

        @XNode("@output")
        public File outputDir = outputDir();

        @XNode("@periodInSecond")
        public Integer period = 10;

        @XNode("@enabled")
        public boolean enabled = Framework.isBooleanPropertyTrue(ENABLED_PROPERTY);

        public int getPeriod() {
            if (period == null) {
                period = Integer.valueOf(Framework.getProperty(PERIOD_PROPERTY, "10"));
            }
            return period;
        }

        protected File outputDir() {
            String path = Framework.getProperty(OUTPUT_PROPERTY, Framework.getProperty(Environment.NUXEO_LOG_DIR));
            DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date today = Calendar.getInstance().getTime();
            outputDir = new File(path, "metrics-" + df.format(today));
            return outputDir;
        }

        @Override
        public String toString() {
            return String.format("csvReporter %s, outputDir: %s, period: %d", enabled ? "enabled" : "disabled",
                    outputDir().toString(), getPeriod());
        }

        protected CsvReporter reporter;

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            File parentDir = outputDir.getParentFile();
            if (parentDir.exists() && parentDir.isDirectory()) {
                outputDir.mkdir();
                reporter = CsvReporter.forRegistry(registry).build(outputDir);
                reporter.start(period, TimeUnit.SECONDS);
            } else {
                enabled = false;
                LogFactory.getLog(MetricsServiceImpl.class).error("Invalid output directory, disabling: " + this);
            }
        }

        public void disable(MetricRegistry registry) {
            if (reporter == null) {
                return;
            }
            try {
                reporter.stop();
            } finally {
                reporter = null;
            }
        }

    }

    /**
     * @since 10.3
     */
    @XObject(value = "statsDReporter")
    public static class StatsDDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.statsd.enabled";

        public static final String HOST_PROPERTY = "metrics.statsd.host";

        public static final String PORT_PROPERTY = "metrics.statsd.port";

        public static final String PERIOD_PROPERTY = "metrics.statsd.period";

        public static final String PREFIX_PROPERTY = "metrics.statsd.prefix";

        /**
         * A list of metric prefixes that if defined should be kept reported, separated by commas
         */
        public static final String ALLOWED_METRICS_PROPERTY = "metrics.statsd.allowedMetrics";

        /**
         * A list of metric prefixes that if defined should not be reported, separated by commas
         */
        public static final String DENIED_METRICS_PROPERTY = "metrics.statsd.deniedMetrics";

        public static final String DEFAULT_ALLOWED_METRICS = GraphiteDescriptor.DEFAULT_ALLOWED_METRICS;

        public static final String DEFAULT_DENIED_METRICS = GraphiteDescriptor.DEFAULT_DENIED_METRICS;

        public static final String ALL_METRICS = "ALL";

        @XNode("@enabled")
        protected Boolean enabled = Boolean.valueOf(Framework.getProperty(ENABLED_PROPERTY, "false"));

        @XNode("@host")
        public String host = Framework.getProperty(HOST_PROPERTY, "127.0.0.1");

        @XNode("@port")
        public Integer port = Integer.valueOf(Framework.getProperty(PORT_PROPERTY, "8125"));

        @XNode("@periodInSecond")
        public Integer period = Integer.valueOf(Framework.getProperty(PERIOD_PROPERTY, "10"));

        @XNode("@prefix")
        public String prefix = getPrefix();

        /**
         * A list of metric prefixes that if defined should be kept reported
         */
        @XNodeList(value = "allowedMetrics/metric", type = ArrayList.class, componentType = String.class)
        public List<String> allowedMetrics = Arrays.asList(
                Framework.getProperty(ALLOWED_METRICS_PROPERTY, DEFAULT_ALLOWED_METRICS).split(","));

        /**
         * A list of metric prefixes that if defined should not be reported
         */
        @XNodeList(value = "deniedMetrics/metric", type = ArrayList.class, componentType = String.class)
        public List<String> deniedMetrics = Arrays.asList(
                Framework.getProperty(DENIED_METRICS_PROPERTY, DEFAULT_DENIED_METRICS).split(","));

        public String getPrefix() {
            if (prefix == null) {
                prefix = Framework.getProperty(PREFIX_PROPERTY, "servers.${hostname}.nuxeo");
            }
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName().split("\\.")[0];
            } catch (UnknownHostException e) {
                hostname = "unknown";
            }
            return prefix.replace("${hostname}", hostname);
        }

        public boolean filter(String name) {
            return allowedMetrics.stream().anyMatch(f -> ALL_METRICS.equals(f) || name.startsWith(f))
                    || deniedMetrics.stream().noneMatch(f -> ALL_METRICS.equals(f) || name.startsWith(f));
        }

        @Override
        public String toString() {
            return String.format("statdReporter %s prefix: %s, host: %s, port: %d, period: %d",
                    enabled ? "enabled" : "disabled", prefix, host, port, period);
        }

        protected StatsDReporter reporter;

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            reporter = StatsDReporter.forRegistry(registry).build(host, port);
            reporter.start(period, TimeUnit.SECONDS);
        }

        public void disable(MetricRegistry registry) {
            if (reporter == null) {
                return;
            }
            try {
                reporter.stop();
            } finally {
                reporter = null;
            }
        }
    }

    @XObject(value = "log4jInstrumentation")
    public static class Log4jInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.log4j.enabled";

        @XNode("@enabled")
        protected boolean enabled = Boolean.parseBoolean(Framework.getProperty(ENABLED_PROPERTY, "false"));

        private InstrumentedAppender appender;

        @Override
        public String toString() {
            return String.format("log4jInstrumentation %s", enabled ? "enabled" : "disabled");
        }

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            LogFactory.getLog(MetricsServiceImpl.class).info(this);

            InstrumentedAppender appender = new InstrumentedAppender(registry, null, null, false);
            appender.start();

            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            config.getLoggerConfig(ROOT_LOGGER_NAME).addAppender(appender, INFO, null);
            context.updateLoggers(config);

        }

        public void disable(MetricRegistry registry) {
            if (appender == null) {
                return;
            }
            try {
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                Configuration config = context.getConfiguration();
                config.getLoggerConfig(ROOT_LOGGER_NAME).removeAppender(appender.getName());
                context.updateLoggers(config);
            } finally {
                appender = null;
            }
        }

    }

    @XObject(value = "tomcatInstrumentation")
    public static class TomcatInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.tomcat.enabled";

        @XNode("@enabled")
        protected boolean enabled = Boolean.parseBoolean(Framework.getProperty(ENABLED_PROPERTY, "false"));

        @Override
        public String toString() {
            return String.format("tomcatInstrumentation %s", enabled ? "enabled" : "disabled");
        }

        protected void registerTomcatGauge(String mbean, String attribute, MetricRegistry registry, String name) {
            try {
                registry.register(MetricRegistry.name("tomcat", name),
                        new JmxAttributeGauge(new ObjectName(mbean), attribute));
            } catch (MalformedObjectNameException | IllegalArgumentException e) {
                throw new UnsupportedOperationException("Cannot compute object name of " + mbean, e);
            }
        }

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            LogFactory.getLog(MetricsServiceImpl.class).info(this);
            // TODO: do not hard code the common datasource
            // nameenable(registry)
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
        }

        public void disable(MetricRegistry registry) {
            registry.remove("tomcat.jdbc-numActive");
            registry.remove("tomcat.jdbc-numIdle");
            registry.remove("tomcat.currentThreadCount");
            registry.remove("tomcat.currentThreadBusy");
            registry.remove("tomcat.errorCount");
            registry.remove("tomcat.requestCount");
            registry.remove("tomcat.processingTime");
            registry.remove("tomcat.activeSessions");
        }
    }

    @XObject(value = "jvmInstrumentation")
    public static class JvmInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.jvm.enabled";

        @XNode("@enabled")
        protected boolean enabled = Boolean.parseBoolean(Framework.getProperty(ENABLED_PROPERTY, "true"));

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            registry.register("jvm.memory", new MemoryUsageGaugeSet());
            registry.register("jvm.garbage", new GarbageCollectorMetricSet());
            registry.register("jvm.threads", new ThreadStatesGaugeSet());
            registry.register("jvm.files", new FileDescriptorRatioGauge());
            registry.register("jvm.buffers",
                    new BufferPoolMetricSet(Framework.getService(ServerLocator.class).lookupServer()));
        }

        public void disable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            registry.removeMatching((name, metric) -> name.startsWith("jvm."));

        }
    }

    @XNode("graphiteReporter")
    public GraphiteDescriptor graphiteReporter = new GraphiteDescriptor();

    @XNode("csvReporter")
    public CsvDescriptor csvReporter = new CsvDescriptor();

    @XNode("statsDReporter")
    public StatsDDescriptor statsDReporter = new StatsDDescriptor();

    @XNode("log4jInstrumentation")
    public Log4jInstrumentationDescriptor log4jInstrumentation = new Log4jInstrumentationDescriptor();

    @XNode("tomcatInstrumentation")
    public TomcatInstrumentationDescriptor tomcatInstrumentation = new TomcatInstrumentationDescriptor();

    @XNode(value = "jvmInstrumentation")
    public JvmInstrumentationDescriptor jvmInstrumentation = new JvmInstrumentationDescriptor();

    protected JmxReporter jmxReporter;

    public void enable(MetricRegistry registry) {
        jmxReporter = JmxReporter.forRegistry(registry).build();
        jmxReporter.start();
        graphiteReporter.enable(registry);
        csvReporter.enable(registry);
        log4jInstrumentation.enable(registry);
        tomcatInstrumentation.enable(registry);
        jvmInstrumentation.enable(registry);
        statsDReporter.enable(registry);
    }

    public void disable(MetricRegistry registry) {
        try {
            graphiteReporter.disable(registry);
            csvReporter.disable(registry);
            log4jInstrumentation.disable(registry);
            tomcatInstrumentation.disable(registry);
            jvmInstrumentation.disable(registry);
            statsDReporter.enable(registry);
            jmxReporter.stop();
        } finally {
            jmxReporter = null;
        }
    }

}
