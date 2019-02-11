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

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.common.Duration;
import io.opencensus.exporter.trace.zipkin.ZipkinExporterConfiguration;
import io.opencensus.exporter.trace.zipkin.ZipkinTraceExporter;

/**
 * @since 11.1
 */
public class ZipkinReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(ZipkinReporter.class);

    public static final String URL = "url";

    public static final String TIMEOUT = "timeout";

    public static final java.time.Duration DEFAULT_TIMEOUT = java.time.Duration.ofSeconds(10);

    protected boolean activated;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Zipkin reporter");
        String url = options.get(URL);
        Duration timeout = Duration.create(
                DurationUtils.parsePositive(options.get(TIMEOUT), DEFAULT_TIMEOUT).getSeconds(), 0);
        ZipkinExporterConfiguration configuration = ZipkinExporterConfiguration.builder()
                                                                               .setServiceName("nuxeo")
                                                                               .setV2Url(url)
                                                                               .setDeadline(timeout)
                                                                               .build();
        ZipkinTraceExporter.createAndRegister(configuration);
        activated = true;
        JaegerReporter.enableTracing(options);
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (activated) {
            ZipkinTraceExporter.unregister();
            activated = false;
        }
    }
}
