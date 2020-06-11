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
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.contrib.zpages.ZPageHandlers;

/**
 * Expose zPages endpoint to control tracing.
 *
 * @since 11.1
 */
public class ZPageReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(ZPageReporter.class);

    public static final String PORT = "port";

    public static final String DEFAULT_PORT = "8887";

    protected boolean activated;

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        if (activated) {
            log.debug("Already activated");
            return;
        }
        int port = Integer.parseInt(options.getOrDefault(PORT, DEFAULT_PORT));
        log.warn("Creating ZPage reporter on port: {}", port);
        try {
            ZPageHandlers.startHttpServerAndRegisterAll(port);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot start ZPage server on port: " + port, e);
        }
        activated = true;
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        // there is no way to stop ZPage not a big deal
    }
}
