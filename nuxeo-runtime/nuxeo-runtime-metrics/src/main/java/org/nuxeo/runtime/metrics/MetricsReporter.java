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
package org.nuxeo.runtime.metrics;

import java.util.Map;
import java.util.Set;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * @since 11.1
 */
public interface MetricsReporter {

    /**
     * Called on new instance.
     */
    void init(long pollInterval, Map<String, String> options);

    /**
     * Starts metrics reporter.
     */
    void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions);

    /**
     * Stops metrics reporter.
     */
    void stop();

}
