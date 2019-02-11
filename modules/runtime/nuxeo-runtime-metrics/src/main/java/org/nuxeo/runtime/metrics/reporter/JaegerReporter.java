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

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.common.Duration;
import io.opencensus.exporter.trace.jaeger.JaegerExporterConfiguration;
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;

/**
 * @since 11.1
 */
public class JaegerReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(JaegerReporter.class);

    public static final String URL = "url";

    public static final String TIMEOUT = "timeout";

    public static final java.time.Duration DEFAULT_TIMEOUT = java.time.Duration.ofSeconds(10);

    public static final String MAX_ATTRIBUTES = "maxAttributes";

    public static final String DEFAULT_MAX_ATTRIBUTES = "128";

    public static final String MAX_ANNOTATIONS = "maxAnnotations";

    public static final String DEFAULT_MAX_ANNOTATIONS = "128";

    public static final String SAMPLER_PROB = "samplerProbability";

    public static final String DEFAULT_SAMPLER_PROB = "0.1";

    protected boolean activated;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Jaeger reporter");
        String url = options.get(URL);
        Duration timeout = Duration.create(
                DurationUtils.parsePositive(options.get(TIMEOUT), DEFAULT_TIMEOUT).getSeconds(), 0);
        JaegerExporterConfiguration configuration = JaegerExporterConfiguration.builder()
                                                                               .setServiceName("nuxeo")
                                                                               .setThriftEndpoint(url)
                                                                               .setDeadline(timeout)
                                                                               .build();
        JaegerTraceExporter.createAndRegister(configuration);
        activated = true;
        enableTracing(options);
    }

    public static void enableTracing(Map<String, String> options) {
        TraceConfig traceConfig = Tracing.getTraceConfig();
        TraceParams activeTraceParams = traceConfig.getActiveTraceParams();
        TraceParams.Builder builder = activeTraceParams.toBuilder();
        int maxAttributes = Integer.parseInt(options.getOrDefault(MAX_ATTRIBUTES, DEFAULT_MAX_ATTRIBUTES));
        int maxAnnotations = Integer.parseInt(options.getOrDefault(MAX_ANNOTATIONS, DEFAULT_MAX_ANNOTATIONS));
        builder.setMaxNumberOfAttributes(maxAttributes).setMaxNumberOfAnnotations(maxAnnotations);
        float samplerProbability = Float.parseFloat(options.getOrDefault(SAMPLER_PROB, DEFAULT_SAMPLER_PROB));
        if (samplerProbability >= 0.999) {
            builder.setSampler(Samplers.alwaysSample());
        } else if (samplerProbability <= 0.001) {
            builder.setSampler(Samplers.neverSample());
        } else {
            builder.setSampler(Samplers.probabilitySampler(samplerProbability));
        }
        traceConfig.updateActiveTraceParams(builder.build());
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (activated) {
            JaegerTraceExporter.unregister();
            activated = false;
        }
    }
}
