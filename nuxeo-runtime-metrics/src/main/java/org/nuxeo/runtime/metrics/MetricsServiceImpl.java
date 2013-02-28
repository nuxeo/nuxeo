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
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.graphite.GraphiteReporter;
import com.yammer.metrics.log4j.InstrumentedAppender;
import com.yammer.metrics.reporting.CsvReporter;

public class MetricsServiceImpl extends DefaultComponent implements
        MetricsService {

    private static final Log log = LogFactory.getLog(MetricsServiceImpl.class);

    private final Counter instanceUp = Metrics.defaultRegistry().newCounter(getClass(),
            "instance-up");

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
            log.warn("Registering metrics contribution");
            config = (MetricsDescriptor) contribution;
        } else {
            log.warn("Unknown EP " + extensionPoint);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        log.warn("Activate component.");
    }

    @Override
    public void deactivate(ComponentContext context) {
        instanceUp.dec();
        log.warn("Deactivate component.");
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        log.warn("Setting up metrics configuration");
        if (config == null) {
            return;
        }
        if (config.graphiteReporter.isEnabled()) {
            log.warn(config.graphiteReporter);
            GraphiteReporter.enable(config.graphiteReporter.period,
                    TimeUnit.SECONDS, config.graphiteReporter.host,
                    config.graphiteReporter.port,
                    config.graphiteReporter.getPrefix());
        }
        if (config.csvReporter.isEnabled()) {
            log.warn(config.csvReporter);
            File outputDir = new File(config.csvReporter.outputDir);
            if (outputDir.exists() && outputDir.isDirectory()) {
                outputDir = config.csvReporter.getOutputDir();
                outputDir.mkdir();
                CsvReporter.enable(outputDir, config.csvReporter.period,
                        TimeUnit.SECONDS);
            } else {
                config.csvReporter.enabled = false;
                log.error("Invalid directory, disabling: " + config.csvReporter);
            }
        }
        if (config.log4jInstrunmentation.isEnabled()) {
            log.warn(config.log4jInstrunmentation);
            // TODO: delete the outputDir ?
            LogManager.getRootLogger().addAppender(new InstrumentedAppender());
        }
        instanceUp.inc();
    }

}
