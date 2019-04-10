/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import static org.coursera.metrics.datadog.DatadogReporter.Expansion.COUNT;
import static org.coursera.metrics.datadog.DatadogReporter.Expansion.MEDIAN;
import static org.coursera.metrics.datadog.DatadogReporter.Expansion.P95;
import static org.coursera.metrics.datadog.DatadogReporter.Expansion.P99;
import static org.coursera.metrics.datadog.DatadogReporter.Expansion.RATE_15_MINUTE;
import static org.coursera.metrics.datadog.DatadogReporter.Expansion.RATE_1_MINUTE;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.coursera.metrics.datadog.DefaultMetricNameFormatter;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class DatadogReporterServiceImpl extends DefaultComponent implements DatadogReporterService {

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private DatadogReporter reporter;

    private DatadogReporterConfDescriptor conf;

    private static final Log log = LogFactory.getLog(DatadogReporterService.class);

    @Override
    public void applicationStarted(ComponentContext context) {
        if (reporter != null) {
            startReporter();
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("configuration".equals(extensionPoint)) {
            setConfiguration((DatadogReporterConfDescriptor) contribution);
        }
    }

    private void setConfiguration(DatadogReporterConfDescriptor conf) {
        if (StringUtils.isBlank(conf.getApiKey())) {
            log.error("Datadog reporter service is not well configured : apiKey is empty. Your metrics won't be sent.");
        } else {
            this.conf = conf;
            buildReporter();
        }
    }

    private void buildReporter() {

        EnumSet<Expansion> expansions = EnumSet.of(COUNT, RATE_1_MINUTE, RATE_15_MINUTE, MEDIAN, P95, P99);
        HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(conf.getApiKey()).build();
        reporter = DatadogReporter.forRegistry(metrics)//
        .withHost(conf.getHost())//
        .withTransport(httpTransport)//
        .withExpansions(expansions)//
        .withMetricNameFormatter(new DefaultMetricNameFormatter())//
        .build();

    }

    @Override
    public void startReporter() {
        if (reporter != null) {
            log.info("Starting Datadog reporter");
            reporter.start(conf.getPollInterval(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void stopReporter() {
        log.info("Stopping Datadog reporter");
        reporter.stop();
    }

    DatadogReporter getReporter() {
        return reporter;
    }

    DatadogReporterConfDescriptor getConfig() {
        return conf;
    }

}
