/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.metrics.reporter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.contrib.dropwizard5.DropWizardMetrics;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.opencensus.metrics.Metrics;
import io.prometheus.client.exporter.HTTPServer;

/**
 * Reports metrics to Prometheus.
 *
 * @since 11.1
 */
public class PrometheusReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(PrometheusReporter.class);

    protected static final int DEFAULT_PORT = 9090;

    protected int port;

    protected int zPort;

    protected HTTPServer server;

    @Override
    public void init(long pollInterval, Map<String, String> options) {
        super.init(pollInterval, options);
        port = getOptionAsInt("port", DEFAULT_PORT);
    }

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Prometheus endpoint on port {}", port);
        DropWizardMetrics registries = new DropWizardMetrics(Collections.singletonList(registry), filter);
        Metrics.getExportComponent().getMetricProducerManager().add(registries);
        try {
            PrometheusStatsCollector.createAndRegister();
        } catch (IllegalArgumentException e) {
            log.warn("Prometheus collector already registered");
        }
        try {
            server = new HTTPServer(port, true);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot start Prometheus on port " + port, e);
        }
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        server.stop();
        server = null;
    }

}
