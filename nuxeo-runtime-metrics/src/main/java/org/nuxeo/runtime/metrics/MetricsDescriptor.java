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

@XObject("metrics")
public class MetricsDescriptor implements Serializable {

    private static final long serialVersionUID = 7833869486922092460L;

    public MetricsDescriptor() {
        super();
    }

    @XObject(value = "graphiteReporter")
    public static class GraphiteDescriptor {

        @XNode("@host")
        public String host = "localhost";

        @XNode("@port")
        public int port = 2030;

        @XNode("@periodInSecond")
        public int period = 10;

        @XNode("@prefix")
        public String prefix = "servers.${hostname}.nuxeo.";

        @XNode("@enabled")
        protected boolean enabled = false;

        @Override
        public String toString() {
            return String.format(
                    "graphiteReporter %s prefix: %s, host: %s, port: %d, period: %d",
                    enabled ? "enabled" : "disabled", getPrefix(), host, port,
                    period);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getPrefix() {
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                hostname = "unknown";
            }
            return prefix.replace("${hostname}", hostname);
        }
    }

    @XObject(value = "csvReporter")
    public static class CsvDescriptor {

        @XNode("@outputDir")
        public String outputDir = "${nuxeo.log.dir}";

        @XNode("@periodInSecond")
        public int period = 10;

        @XNode("@enabled")
        protected boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public File getOutputDir() {
            DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date today = Calendar.getInstance().getTime();
            File dir = new File(outputDir, "metrics-" + df.format(today));
            return dir;
        }

        @Override
        public String toString() {
            return String.format("csvReporter %s, outputDir: %s, period: %d",
                    enabled ? "enabled" : "disabled", outputDir, period);
        }

    }

    @XObject(value = "log4jInstrumentation")
    public static class Log4jInstrumentationDescriptor {

        @XNode("@enabled")
        protected boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String toString() {
            return String.format("log4jInstrumentation %s", enabled ? "enabled"
                    : "disabled");
        }

    }

    @XNode("graphiteReporter")
    public GraphiteDescriptor graphiteReporter;

    @XNode("csvReporter")
    public CsvDescriptor csvReporter;

    @XNode("log4jInstrumentation")
    public Log4jInstrumentationDescriptor log4jInstrunmentation;

}
