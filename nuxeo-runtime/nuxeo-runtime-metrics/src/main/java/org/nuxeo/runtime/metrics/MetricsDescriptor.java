/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

import com.codahale.metrics.CsvReporter;
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
import com.codahale.metrics.log4j.InstrumentedAppender;

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

        @XNode("@enabled")
        protected Boolean enabled = Boolean.valueOf(Framework.getProperty(
                ENABLED_PROPERTY, "false"));

        @XNode("@host")
        public String host = Framework.getProperty(HOST_PROPERTY, "0.0.0.0");

        @XNode("@port")
        public Integer port = Integer.valueOf(Framework.getProperty(
                PORT_PROPERTY, "2030"));

        @XNode("@periodInSecond")
        public Integer period = Integer.valueOf(Framework.getProperty(
                PERIOD_PROPERTY, "10"));

        @XNode("@prefix")
        public String prefix = prefix();

        public String prefix() {
            if (prefix == null) {
                prefix = Framework.getProperty(PREFIX_PROPERTY,
                        "servers.${hostname}.nuxeo");
            }
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName().split("\\.")[0];
            } catch (UnknownHostException e) {
                hostname = "unknown";
            }
            return prefix.replace("${hostname}", hostname);
        }

        @Override
        public String toString() {
            return String.format(
                    "graphiteReporter %s prefix: %s, host: %s, port: %d, period: %d",
                    enabled ? "enabled" : "disabled", prefix, host, port,
                    period);
        }

        protected GraphiteReporter reporter;

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            InetSocketAddress address = new InetSocketAddress(host, port);
            Graphite graphite = new Graphite(address);
            reporter = GraphiteReporter.forRegistry(registry).convertRatesTo(
                    TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MICROSECONDS).prefixedWith(
                    prefix()).build(graphite);
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
                period = Integer.valueOf(Framework.getProperty(PERIOD_PROPERTY,
                        "10"));
            }
            return period;
        }

        protected File outputDir() {
            String path = Framework.getProperty(OUTPUT_PROPERTY,
                    Framework.getProperty("nuxeo.log.dir"));
            DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date today = Calendar.getInstance().getTime();
            outputDir = new File(path, "metrics-" + df.format(today));
            return outputDir;
        }

        @Override
        public String toString() {
            return String.format("csvReporter %s, outputDir: %s, period: %d",
                    enabled ? "enabled" : "disabled", outputDir().toString(),
                    getPeriod());
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
                reporter.start(Long.valueOf(period), TimeUnit.SECONDS);
            } else {
                enabled = false;
                LogFactory.getLog(MetricsServiceImpl.class).error(
                        "Invalid output directory, disabling: " + this);
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

    @XObject(value = "log4jInstrumentation")
    public static class Log4jInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.log4j.enabled";

        @XNode("@enabled")
        protected boolean enabled = Boolean.getBoolean(Framework.getProperty(
                ENABLED_PROPERTY, "false"));

        private InstrumentedAppender appender;

        @Override
        public String toString() {
            return String.format("log4jInstrumentation %s", enabled ? "enabled"
                    : "disabled");
        }

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            LogFactory.getLog(MetricsServiceImpl.class).info(this);
            appender = new InstrumentedAppender(registry);
            LogManager.getRootLogger().addAppender(appender);
        }

        public void disable(MetricRegistry registry) {
            if (appender == null) {
                return;
            }
            try {
                LogManager.getRootLogger().removeAppender(appender);
            } finally {
                appender = null;
            }
        }

    }

    @XObject(value = "tomcatInstrumentation")
    public static class TomcatInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.tomcat.enabled";

        @XNode("@enabled")
        protected boolean enabled = Boolean.parseBoolean(Framework.getProperty(
                ENABLED_PROPERTY, "false"));

        @Override
        public String toString() {
            return String.format("tomcatInstrumentation %s",
                    enabled ? "enabled" : "disabled");
        }

        protected void registerTomcatGauge(String mbean, String attribute,
                MetricRegistry registry, String name) {
            try {
                registry.register(MetricRegistry.name("tomcat", name),
                        new JmxAttributeGauge(new ObjectName(mbean), attribute));
            } catch (MalformedObjectNameException | IllegalArgumentException e) {
                throw new UnsupportedOperationException(
                        "Cannot compute object name of " + mbean, e);
            }
        }

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            LogFactory.getLog(MetricsServiceImpl.class).info(this);
            // TODO: do not hard code the common datasource
            // nameenable(registry)
            String pool = "Catalina:type=DataSource,class=javax.sql.DataSource,name=\"jdbc/nuxeo\"";
            String connector = String.format(
                    "Catalina:type=ThreadPool,name=\"http-bio-%s-%s\"",
                    Framework.getProperty("nuxeo.bind.address", "0.0.0.0"),
                    Framework.getProperty("nuxeo.bind.port", "8080"));
            String requestProcessor = String.format(
                    "Catalina:type=GlobalRequestProcessor,name=\"http-bio-%s-%s\"",
                    Framework.getProperty("nuxeo.bind.address", "0.0.0.0"),
                    Framework.getProperty("nuxeo.bind.port", "8080"));
            String manager = "Catalina:type=Manager,context=/nuxeo,host=localhost";
            registerTomcatGauge(pool, "numActive", registry, "jdbc-numActive");
            registerTomcatGauge(pool, "numIdle", registry, "jdbc-numIdle");
            registerTomcatGauge(connector, "currentThreadCount", registry,
                    "currentThreadCount");
            registerTomcatGauge(connector, "currentThreadsBusy", registry,
                    "currentThreadBusy");
            registerTomcatGauge(requestProcessor, "errorCount", registry,
                    "errorCount");
            registerTomcatGauge(requestProcessor, "requestCount", registry,
                    "requestCount");
            registerTomcatGauge(requestProcessor, "processingTime", registry,
                    "processingTime");
            registerTomcatGauge(manager, "activeSessions", registry,
                    "activeSessions");
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
        protected boolean enabled = Boolean.parseBoolean(Framework.getProperty(
                ENABLED_PROPERTY, "true"));

        public void enable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            registry.register("jvm.memory", new MemoryUsageGaugeSet());
            registry.register("jvm.garbage", new GarbageCollectorMetricSet());
            registry.register("jvm.threads", new ThreadStatesGaugeSet());
            registry.register("jvm.files", new FileDescriptorRatioGauge());
            registry.register("jvm.buffers", new BufferPoolMetricSet(
                    ManagementFactory.getPlatformMBeanServer()));
        }

        public void disable(MetricRegistry registry) {
            if (!enabled) {
                return;
            }
            registry.remove("jvm.memory");
            registry.remove("jvm.garbage");
            registry.remove("jvm.threads");
            registry.remove("jvm.files");
            registry.remove("jvm.buffers");
        }
    }

    @XNode("graphiteReporter")
    public GraphiteDescriptor graphiteReporter = new GraphiteDescriptor();

    @XNode("csvReporter")
    public CsvDescriptor csvReporter = new CsvDescriptor();

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
    }

    public void disable(MetricRegistry registry) {
        try {
            jmxReporter.stop();
            graphiteReporter.disable(registry);
            csvReporter.disable(registry);
            log4jInstrumentation.disable(registry);
            tomcatInstrumentation.disable(registry);
            jvmInstrumentation.disable(registry);
        } finally {
            jmxReporter = null;
        }
    }

}
