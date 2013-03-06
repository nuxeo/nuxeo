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
 *      Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.graphite.GraphiteReporter;
import com.yammer.metrics.log4j.InstrumentedAppender;
import com.yammer.metrics.reporting.CsvReporter;
import com.yammer.metrics.util.JmxGauge;

public class MetricsServiceImpl extends DefaultComponent implements
        MetricsService {

    private static final Log log = LogFactory.getLog(MetricsServiceImpl.class);

    private final Counter instanceUp = Metrics.defaultRegistry().newCounter(
            MetricsServiceImpl.class, "instance-up");

    public static final String CONFIGURATION_EP = "configuration";

    public static MetricsDescriptor config;

    public MetricsServiceImpl() {
        super();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIGURATION_EP.equals(extensionPoint)
                && contribution instanceof MetricsDescriptor) {
            log.debug("Registering metrics contribution");
            config = (MetricsDescriptor) contribution;
        } else {
            log.warn("Unknown EP " + extensionPoint);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        log.debug("Activate component.");
    }

    @Override
    public void deactivate(ComponentContext context) {
        instanceUp.dec();
        log.debug("Deactivate component.");
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (config == null) {
            // Use a default config
            config = new MetricsDescriptor();
        }
        log.info("Setting up metrics configuration");
        if (config.graphiteReporter.isEnabled()) {
            log.info(config.graphiteReporter);
            GraphiteReporter.enable(config.graphiteReporter.getPeriod(),
                    TimeUnit.SECONDS, config.graphiteReporter.getHost(),
                    config.graphiteReporter.getPort(),
                    config.graphiteReporter.getPrefix());
        }
        if (config.csvReporter.isEnabled()) {
            log.info(config.csvReporter);
            File outputDir = config.csvReporter.getOutput();
            if (outputDir.getParentFile().exists() && outputDir.getParentFile().isDirectory()) {
                outputDir.mkdir();
                CsvReporter.enable(outputDir, config.csvReporter.period,
                        TimeUnit.SECONDS);
            } else {
                config.csvReporter.enabled = false;
                log.error("Invalid output directory, disabling: " + config.csvReporter);
            }
        }
        if (config.log4jInstrunmentation.isEnabled()) {
            log.info(config.log4jInstrunmentation);
            LogManager.getRootLogger().addAppender(new InstrumentedAppender());
        }
        instanceUp.inc();

        if (config.tomcatInstrunmentation.isEnabled()) {
            log.info(config.tomcatInstrunmentation);
            // TODO: do not hard code the common datasource name
            String commonDs = "jdbc/nuxeo";
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "jdbc-numActive",
                    new JmxGauge(
                            "Catalina:type=DataSource,class=javax.sql.DataSource,name=\""
                                    + commonDs + "\"", "numActive"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "jdbc-numIdle",
                    new JmxGauge(
                            "Catalina:type=DataSource,class=javax.sql.DataSource,name=\""
                                    + commonDs + "\"", "numIdle"));

            String connector = String.format("http-%s-%s",
                    Framework.getProperty("nuxeo.bind.address", "0.0.0.0"),
                    Framework.getProperty("nuxeo.bind.port", "8080"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-currentThreadCount",
                    new JmxGauge("Catalina:type=ThreadPool,name=" + connector,
                            "currentThreadCount"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-currentThreadsBusy",
                    new JmxGauge("Catalina:type=ThreadPool,name=" + connector,
                            "currentThreadsBusy"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-errorCount",
                    new JmxGauge("Catalina:type=GlobalRequestProcessor,name="
                            + connector, "errorCount"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-errorCount",
                    new JmxGauge("Catalina:type=GlobalRequestProcessor,name="
                            + connector, "errorCount"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-requestCount",
                    new JmxGauge("Catalina:type=GlobalRequestProcessor,name="
                            + connector, "requestCount"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-processingTime",
                    new JmxGauge("Catalina:type=GlobalRequestProcessor,name="
                            + connector, "processingTime"));
            Metrics.defaultRegistry().newGauge(
                    getClass(),
                    "tomcat-activeSessions",
                    new JmxGauge(
                            "Catalina:type=Manager,path=/nuxeo,host=localhost",
                            "activeSessions"));
        }
    }

}
