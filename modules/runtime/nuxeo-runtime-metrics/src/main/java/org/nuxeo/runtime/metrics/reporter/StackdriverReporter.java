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
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.common.Duration;
import io.opencensus.contrib.dropwizard5.DropWizardMetrics;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.metrics.Metrics;

/**
 * Reports metrics to Google Stackdriver.
 *
 * @since 11.4
 */
public class StackdriverReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(StackdriverReporter.class);

    protected static final String PREFIX_OPTION = "prefix";

    protected static final String DEFAULT_PREFIX = "custom.googleapis.com/nuxeo/";

    protected boolean activated;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Stackdriver metrics reporter");
        DropWizardMetrics registries = new DropWizardMetrics(Collections.singletonList(registry), filter);
        Metrics.getExportComponent().getMetricProducerManager().add(registries);
        Duration timeout = Duration.create(
                DurationUtils.parsePositive(options.get(TIMEOUT_OPTION), DEFAULT_TIMEOUT).getSeconds(), 0);
        String projectId = StackdriverTraceReporter.getGcpProjectId(options);
        String prefix = options.getOrDefault(PREFIX_OPTION, DEFAULT_PREFIX);
        Duration interval = Duration.fromMillis(getPollInterval() * 1000);
        StackdriverStatsConfiguration configuration = StackdriverStatsConfiguration.builder()
                                                                                   .setDeadline(timeout)
                                                                                   .setProjectId(projectId)
                                                                                   .setMetricNamePrefix(prefix)
                                                                                   .setExportInterval(interval)
                                                                                   .build();
        try {
            StackdriverStatsExporter.createAndRegister(configuration);
        } catch (IOException e) {
            log.error("Fail to create a Stackdriver metric reporter", e);
            return;
        }
        activated = true;
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (activated) {
            StackdriverStatsExporter.unregister();
            activated = false;
        }
    }
}
