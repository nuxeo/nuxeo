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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.common.Duration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;

/**
 * Reports trace to Google Stackdriver.
 *
 * @since 11.4
 */
public class StackdriverTraceReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(StackdriverTraceReporter.class);

    public static final String GCP_PROJECT_ID_ENV_PROP = "GCP_PROJECT_ID";

    public static final String GCP_PROJECT_ID_OPTION_PROP = "gcpProjectId";

    protected boolean activated;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Stackdriver trace reporter");
        Duration timeout = Duration.create(
                DurationUtils.parsePositive(options.get(TIMEOUT_OPTION), DEFAULT_TIMEOUT).getSeconds(), 0);
        String projectId = getGcpProjectId(options);
        StackdriverTraceConfiguration configuration = StackdriverTraceConfiguration.builder()
                                                                                   .setDeadline(timeout)
                                                                                   .setProjectId(projectId)
                                                                                   .build();
        try {
            StackdriverTraceExporter.createAndRegister(configuration);
        } catch (IOException e) {
            log.error("Fail to create a Stackdriver trace reporter", e);
            return;
        }
        activated = true;
        enableTracing();
    }

    protected static String getGcpProjectId(Map<String, String> options) {
        return defaultIfBlank(options.get(GCP_PROJECT_ID_OPTION_PROP), Framework.getProperty(GCP_PROJECT_ID_ENV_PROP));
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (activated) {
            StackdriverTraceExporter.unregister();
            activated = false;
        }
    }
}
