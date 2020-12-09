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
package org.nuxeo.runtime.stream;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;

/**
 * A Specialized Nuxeo Metrics Reporter that sends only Nuxeo Stream metrics into a Stream.
 *
 * @since 11.5
 */
public class StreamMetricsNuxeoReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(StreamMetricsNuxeoReporter.class);

    protected ScheduledReporter reporter;

    protected static final MetricFilter STREAM_METRICS_FILTER = MetricFilter.startsWith("nuxeo.stream");

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Reporting Stream Metrics to: {}", LogStreamManager.METRICS_STREAM);
        ScheduledReporter reporter = new StreamMetricsReporter(registry, STREAM_METRICS_FILTER);
        reporter.start(getPollInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (reporter != null) {
            reporter.stop();
        }
    }

}
