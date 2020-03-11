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
package org.nuxeo.runtime.stream;

import static org.nuxeo.lib.stream.computation.log.ComputationRunner.GLOBAL_FAILURE_COUNT_REGISTRY_NAME;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.api.Probe;
import org.nuxeo.runtime.management.api.ProbeStatus;
import org.nuxeo.runtime.services.config.ConfigurationService;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * A probe to detect when computation has been terminated due to failure. A delay is applied before returning the
 * failure code.
 *
 * @since 11.1
 */
public class StreamProbe implements Probe {

    public static final String STREAM_PROBE_DELAY_PROPERTY = "nuxeo.stream.health.check.delay";

    public static final Duration STREAM_PROBE_DELAY_DEFAULT = Duration.ofHours(36);

    protected static final String FAILURE_MESSAGE = "%d computations have been terminated after failure. "
            + "First failure detected: %s, probe failure delayed by %s. "
            + "This Nuxeo instance must be restarted within the stream retention period.";

    protected Counter globalFailureCount;

    protected Long detected;

    protected Duration timeout;

    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssz")
                                                                          .withZone(ZoneOffset.UTC);

    @Override
    public ProbeStatus run() {
        long failures = getFailures();
        if (failures == 0) {
            return ProbeStatus.newSuccess("No failure");
        }
        String dateFailure = FORMATTER.format(Instant.ofEpochMilli(detected));
        String message = String.format(FAILURE_MESSAGE, failures, dateFailure, getTimeout());
        if (System.currentTimeMillis() - detected < getTimeout().toMillis()) {
            // Failure is delayed
            return ProbeStatus.newSuccess(message);
        }
        return ProbeStatus.newFailure(message);
    }

    protected Duration getTimeout() {
        if (timeout == null) {
            ConfigurationService confService = Framework.getService(ConfigurationService.class);
            timeout = confService.getDuration(STREAM_PROBE_DELAY_PROPERTY, STREAM_PROBE_DELAY_DEFAULT);
        }
        return timeout;
    }

    protected long getFailures() {
        long failures = getCounter().getCount();
        if (failures > 0 && detected == null) {
            detected = System.currentTimeMillis();
        }
        return failures;
    }

    protected Counter getCounter() {
        if (globalFailureCount == null) {
            MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);
            globalFailureCount = registry.counter(GLOBAL_FAILURE_COUNT_REGISTRY_NAME);
        }
        return globalFailureCount;
    }

    /**
     * Reset failure counter for testing purpose.
     *
     * @since 11.1
     */
    public void reset() {
        long count = getCounter().getCount();
        if (count > 0) {
            getCounter().dec(count);
        }
        detected = null;
    }
}
