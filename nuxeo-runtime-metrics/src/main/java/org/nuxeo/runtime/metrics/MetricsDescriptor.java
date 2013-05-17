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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("metrics")
public class MetricsDescriptor implements Serializable {

    private static final long serialVersionUID = 7833869486922092460L;

    public MetricsDescriptor() {
        super();
        graphiteReporter = new GraphiteDescriptor();
        csvReporter = new CsvDescriptor();
        tomcatInstrunmentation = new TomcatInstrumentationDescriptor();
        log4jInstrunmentation = new Log4jInstrumentationDescriptor();
    }

    @XObject(value = "graphiteReporter")
    public static class GraphiteDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.graphite.enabled";

        public static final String HOST_PROPERTY = "metrics.graphite.host";

        public static final String PORT_PROPERTY = "metrics.graphite.port";

        public static final String PERIOD_PROPERTY = "metrics.graphite.period";

        public static final String PREFIX_PROPERTY = "metrics.graphite.prefix";

        @XNode("@enabled")
        protected Boolean enabled;

        @XNode("@host")
        public String host;

        @XNode("@port")
        public Integer port;

        @XNode("@periodInSecond")
        public Integer period;

        @XNode("@prefix")
        public String prefix;

        public boolean isEnabled() {
            if (enabled == null) {
                enabled = Boolean.valueOf(Framework.getProperty(
                        ENABLED_PROPERTY, "false"));
            }
            return enabled;
        }

        public String getHost() {
            if (host == null) {
                host = Framework.getProperty(HOST_PROPERTY, "0.0.0.0");
            }
            return host;
        }

        public int getPort() {
            if (port == null) {
                port = Integer.valueOf(Framework.getProperty(PORT_PROPERTY,
                        "2030"));
            }
            return port;
        }

        public int getPeriod() {
            if (period == null) {
                period = Integer.valueOf(Framework.getProperty(PERIOD_PROPERTY,
                        "10"));
            }
            return period;
        }

        public String getPrefix() {
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
                    isEnabled() ? "enabled" : "disabled", getPrefix(),
                    getHost(), getPort(), getPeriod());
        }

    }

    @XObject(value = "csvReporter")
    public static class CsvDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.csv.enabled";

        public static final String PERIOD_PROPERTY = "metrics.csv.period";

        public static final String OUTPUT_PROPERTY = "metrics.csv.output";

        @XNode("@output")
        public String outputPath;

        public File outputDir;

        @XNode("@periodInSecond")
        public Integer period = 10;

        @XNode("@enabled")
        public Boolean enabled;

        public boolean isEnabled() {
            if (enabled == null) {
                enabled = Boolean.valueOf(Framework.getProperty(
                        ENABLED_PROPERTY, "false"));
            }
            return enabled;
        }

        public int getPeriod() {
            if (period == null) {
                period = Integer.valueOf(Framework.getProperty(PERIOD_PROPERTY,
                        "10"));
            }
            return period;
        }

        public File getOutput() {
            if (outputDir == null) {
                if (outputPath == null) {
                    outputPath = Framework.getProperty(OUTPUT_PROPERTY,
                            Framework.getProperty("nuxeo.log.dir"));
                }
                DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
                Date today = Calendar.getInstance().getTime();
                outputDir = new File(outputPath, "metrics-" + df.format(today));
            }
            return outputDir;
        }

        @Override
        public String toString() {
            return String.format("csvReporter %s, outputDir: %s, period: %d",
                    isEnabled() ? "enabled" : "disabled",
                    getOutput().toString(), getPeriod());
        }

    }

    @XObject(value = "log4jInstrumentation")
    public static class Log4jInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.log4j.enabled";

        @XNode("@enabled")
        protected Boolean enabled;

        public boolean isEnabled() {
            if (enabled == null) {
                enabled = Boolean.valueOf(Framework.getProperty(
                        ENABLED_PROPERTY, "false"));
            }
            return enabled;
        }

        @Override
        public String toString() {
            return String.format("log4jInstrumentation %s",
                    isEnabled() ? "enabled" : "disabled");
        }

    }

    @XObject(value = "tomcatInstrumentation")
    public static class TomcatInstrumentationDescriptor {

        public static final String ENABLED_PROPERTY = "metrics.tomcat.enabled";

        @XNode("@enabled")
        protected Boolean enabled;

        public boolean isEnabled() {
            if (enabled == null) {
                enabled = Boolean.valueOf(Framework.getProperty(
                        ENABLED_PROPERTY, "false"));
            }
            return enabled;
        }

        @Override
        public String toString() {
            return String.format("tomcatInstrumentation %s",
                    enabled ? "enabled" : "disabled");
        }

    }

    @XNode("graphiteReporter")
    public GraphiteDescriptor graphiteReporter;

    @XNode("csvReporter")
    public CsvDescriptor csvReporter;

    @XNode("log4jInstrumentation")
    public Log4jInstrumentationDescriptor log4jInstrunmentation;

    @XNode("tomcatInstrumentation")
    public TomcatInstrumentationDescriptor tomcatInstrunmentation;

}
