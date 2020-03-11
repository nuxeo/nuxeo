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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;
import org.nuxeo.runtime.metrics.reporter.patch.NuxeoGraphiteReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.graphite.Graphite;
import io.dropwizard.metrics5.graphite.GraphiteSender;
import io.dropwizard.metrics5.graphite.GraphiteUDP;

/**
 * @since 11.1
 */
public class GraphiteReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(GraphiteReporter.class);

    protected static final Integer DEFAULT_PORT = 2003;

    protected static final String DEFAULT_PREFIX = "servers.${hostname}.nuxeo";

    protected InetSocketAddress address;

    protected NuxeoGraphiteReporter reporter;

    protected String prefix;

    public GraphiteReporter() {
    }

    @Override
    public void init(long pollInterval, Map<String, String> options) {
        super.init(pollInterval, options);
        String host = requireOption("host");
        int port = getOptionAsInt("port", DEFAULT_PORT);
        address = new InetSocketAddress(host, port);
        prefix = getPrefix();
    }

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        GraphiteSender graphite;
        if (getOptionAsBoolean("udp", false)) {
            log.warn("Connecting to graphite in UDP {} reporting every {}s with prefix: {}", address, pollInterval, prefix);
            graphite = new GraphiteUDP(address);
        } else {
            log.warn("Connecting to graphite {} reporting every {}s with prefix: {}", address, pollInterval, prefix);
            graphite = new Graphite(address);
        }
        reporter = new NuxeoGraphiteReporter(registry, filter,
                io.dropwizard.metrics5.graphite.GraphiteReporter.forRegistry(registry)
                                                                .convertRatesTo(TimeUnit.SECONDS)
                                                                .convertDurationsTo(TimeUnit.MICROSECONDS)
                                                                .prefixedWith(prefix)
                                                                .filter(filter)
                                                                .disabledMetricAttributes(deniedExpansions)
                                                                .build(graphite));
        reporter.start(getPollInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        reporter.stop();
    }

    protected String getPrefix() {
        prefix = getOption("prefix", DEFAULT_PREFIX);
        return prefix.replace("${hostname}", getCurrentHostname());
    }

}
