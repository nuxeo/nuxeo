/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;
import org.nuxeo.runtime.metrics.reporter.patch.NuxeoDynatraceReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * Reports metrics to Dynatrace via OneAgent's StatsD listener (UDP).
 * <p>
 * This reporter sends metrics locally over UDP to the Dynatrace OneAgent's embedded StatsD daemon. This approach is
 * non-blocking and doesn't require authentication since communication is local only.
 *
 * @since 2025.15
 */
public class DynatraceReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(DynatraceReporter.class);

    protected static final String DEFAULT_HOST = "localhost";

    protected static final int DEFAULT_PORT = 18125;

    protected static final String DEFAULT_PREFIX = "nuxeo";

    protected NuxeoDynatraceReporter reporter;

    protected String hostname;

    @Override
    public void init(long pollInterval, Map<String, String> options) {
        super.init(pollInterval, options);
        hostname = getHostname();
    }

    protected String getHostname() {
        String value = options.get("hostname");
        if (isNotBlank(value)) {
            return value;
        }
        value = getHostnameFromNuxeoUrl();
        if (isNotBlank(value)) {
            return value;
        }
        return getCurrentHostname();
    }

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        String host = getOption("host", DEFAULT_HOST);
        int port = getOptionAsInt("port", DEFAULT_PORT);
        String prefix = getOption("prefix", DEFAULT_PREFIX);
        Map<String, String> dimensions = parseDimensions(getOption("dimensions", ""));
        boolean emptyTimerAsCount = getOptionAsBoolean("emptyTimerAsCount", false);

        log.warn("Creating Dynatrace Statsd reporter to {}:{} reporting every {}s with prefix: {} from host: {}", host,
                port, pollInterval, prefix, hostname);

        reporter = NuxeoDynatraceReporter.builder(registry)
                                         .withHost(host)
                                         .withPort(port)
                                         .withPrefix(prefix)
                                         .withHostname(hostname)
                                         .withDimensions(dimensions)
                                         .withFilter(filter)
                                         .withDeniedExpansions(deniedExpansions)
                                         .withEmptyTimerAsCount(emptyTimerAsCount)
                                         .build();
        reporter.start(getPollInterval(), TimeUnit.SECONDS);
    }

    protected Map<String, String> parseDimensions(String dimensionsStr) {
        if (isBlank(dimensionsStr)) {
            return Map.of();
        }
        var dimensions = new HashMap<String, String>();
        for (String pair : dimensionsStr.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && isNotBlank(kv[0]) && isNotBlank(kv[1])) {
                dimensions.put(kv[0].trim(), kv[1].trim());
            }
        }
        return dimensions;
    }

    @Override
    public void stop() {
        log.debug("Stop reporting to Dynatrace");
        if (reporter != null) {
            reporter.stop();
            reporter = null;
        }
    }
}
