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

import java.net.MalformedURLException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.common.Duration;
import io.opencensus.exporter.trace.datadog.DatadogTraceConfiguration;
import io.opencensus.exporter.trace.datadog.DatadogTraceExporter;

/**
 * Reports traces to Datadog agent.
 *
 * @since 11.1
 */
public class DatadogTraceReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(DatadogTraceReporter.class);

    protected static final String TYPE_OPTION = "type";

    protected static final String DEFAULT_TYPE = "web";

    protected boolean activated;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Datadog Trace reporter");
        String url = options.get(URL_OPTION);
        String service = options.getOrDefault(SERVICE_OPTION, DEFAULT_SERVICE);
        String type = options.getOrDefault(TYPE_OPTION, DEFAULT_TYPE);
        Duration timeout = Duration.create(
                DurationUtils.parsePositive(options.get(TIMEOUT_OPTION), DEFAULT_TIMEOUT).getSeconds(), 0);
        DatadogTraceConfiguration config = DatadogTraceConfiguration.builder()
                                                                    .setAgentEndpoint(url)
                                                                    .setService(service)
                                                                    .setType(type)
                                                                    .setDeadline(timeout)
                                                                    .build();
        try {
            DatadogTraceExporter.createAndRegister(config);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        activated = true;
        enableTracing();
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (activated) {
            DatadogTraceExporter.unregister();
            activated = false;
        }
    }
}
