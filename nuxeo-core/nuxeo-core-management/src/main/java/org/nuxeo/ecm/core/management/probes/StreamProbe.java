/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.management.probes;

import static org.nuxeo.lib.stream.computation.log.ComputationRunner.GLOBAL_FAILURE_COUNT_REGISTRY_NAME;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * A probe to detect when computation has been terminated due to failure.
 *
 * @since 11.1
 */
public class StreamProbe implements Probe {

    protected static final String FAILURE_MESSAGE = "%d computations have been terminated after failure. This Nuxeo instance must be restarted within the stream retention period.";

    protected Counter globalFailureCount;

    @Override
    public ProbeStatus run() {
        long failures = getFailures();
        if (failures > 0) {
            return ProbeStatus.newFailure(String.format(FAILURE_MESSAGE, failures));
        }
        return ProbeStatus.newSuccess("No failure");
    }

    protected long getFailures() {
        if (globalFailureCount == null) {
            MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);
            globalFailureCount = registry.counter(GLOBAL_FAILURE_COUNT_REGISTRY_NAME);
        }
        return globalFailureCount.getCount();
    }
}
