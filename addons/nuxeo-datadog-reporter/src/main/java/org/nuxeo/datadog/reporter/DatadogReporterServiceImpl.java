/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
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
    public void start(ComponentContext context) {
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
        HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(conf.getApiKey()).build();
        reporter = DatadogReporter.forRegistry(metrics)//
                                  .withHost(conf.getHost())//
                                  .withTags(conf.getTags())
                                  .withTransport(httpTransport)//
                                  .withExpansions(Expansion.ALL)//
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
