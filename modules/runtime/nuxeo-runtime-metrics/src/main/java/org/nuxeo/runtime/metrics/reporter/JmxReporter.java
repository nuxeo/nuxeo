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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * Reports metrics to JMX.
 *
 * @since 11.1
 */
public class JmxReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(JmxReporter.class);

    private io.dropwizard.metrics5.jmx.JmxReporter reporter;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Jmx reporter");
        reporter = io.dropwizard.metrics5.jmx.JmxReporter.forRegistry(registry)
                                                         .convertRatesTo(TimeUnit.SECONDS)
                                                         .convertDurationsTo(TimeUnit.MICROSECONDS)
                                                         .filter(filter)
                                                         .build();
        reporter.start();
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        reporter.stop();
        reporter = null;
    }

}
