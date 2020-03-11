/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.runtime.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import io.dropwizard.metrics5.MetricName;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.runtime.metrics:test-metrics-contrib.xml")
public class TestMetricsService {

    @Inject
    protected MetricsService service;

    @Test
    public void testService() {
        assertNotNull(service);
        service.stopReporters();
        service.startReporters();
        service.stopReporters();
        service.startReporters();
    }

    @Test
    public void testMockReporter() {
        assertNotNull(MockReporter.getFilterForTesting());
        assertEquals(42, MockReporter.getPollIntervalForTesting());
    }

    @Test
    public void testFilterMetrics() {
        List<String> metrics = Arrays.asList("nuxeo.directories.continent.cache.hits", //
                "nuxeo.directories.continent.cache.invalidations", //
                "nuxeo.directories.continent.cache.misses", //
                "nuxeo.directories.continent.cache.size", //
                "nuxeo.directories.digestauth.cache.hits", //
                "nuxeo.directories.digestauth.cache.invalidations", //
                "nuxeo.directories.digestauth.cache.misses", //
                "nuxeo.directories.digestauth.cache.size", //
                "nuxeo.directories.directory.groupDirectory.cache.hits", //
                "nuxeo.directories.directory.groupDirectory.cache.invalidations", //
                "nuxeo.directories.directory.groupDirectory.cache.misses", //
                "nuxeo.directories.directory.groupDirectory.cache.size", //
                "nuxeo.cache.vocab-WorkflowType-cache-without-references.invalidate-all-counter", //
                "nuxeo.cache.vocab-WorkflowType-cache-without-references.read-counter", //
                "nuxeo.cache.vocab-WorkflowType-cache-without-references.read-hit-counter", //
                "nuxeo.cache.vocab-WorkflowType-cache-without-references.read-hit-ratio", //
                "nuxeo.cache.vocab-WorkflowType-cache-without-references.read-miss-counter", //
                "nuxeo.cache.userDisplayName.invalidate-all-counter", //
                "nuxeo.cache.userDisplayName.read-counter", //
                "nuxeo.cache.userDisplayName.read-hit-counter", //
                "nuxeo.cache.userDisplayName.read-hit-ratio", //
                "nuxeo.cache.userDisplayName.read-miss-counter", //
                "nuxeo.cache.user-entry-cache-without-references.invalidate-all-counter", //
                "nuxeo.cache.user-entry-cache-without-references.read-counter", //
                "nuxeo.cache.user-entry-cache-without-references.read-hit-counter", //
                "nuxeo.cache.user-entry-cache-without-references.read-hit-ratio", //
                "nuxeo.cache.user-entry-cache-without-references.read-miss-counter", //
                "nuxeo.cache.user-entry-cache.invalidate-all-counter", //
                "nuxeo.cache.user-entry-cache.read-counter", //
                "nuxeo.cache.user-entry-cache.read-hit-counter", //
                "nuxeo.cache.user-entry-cache.read-hit-ratio", //
                "nuxeo.cache.user-entry-cache.read-miss-counter", //
                "nuxeo.elasticsearch.service.bulkIndex", //
                "nuxeo.elasticsearch.service.delete", //
                "nuxeo.elasticsearch.service.fetch", //
                "nuxeo.elasticsearch.service.index", //
                "nuxeo.elasticsearch.service.scroll", //
                "nuxeo.elasticsearch.service.search",
                "org.nuxeo.ecm.core.management.standby.StandbyComponent.meter.count");

        List<String> expectedMetrics = Arrays.asList("nuxeo.directories.directory.groupDirectory.cache.hits", //
                "nuxeo.directories.directory.groupDirectory.cache.invalidations", //
                "nuxeo.directories.directory.groupDirectory.cache.misses", //
                "nuxeo.directories.directory.groupDirectory.cache.size", //
                "nuxeo.cache.user-entry-cache.invalidate-all-counter", //
                "nuxeo.cache.user-entry-cache.read-counter", //
                "nuxeo.cache.user-entry-cache.read-hit-counter", //
                "nuxeo.cache.user-entry-cache.read-hit-ratio", //
                "nuxeo.cache.user-entry-cache.read-miss-counter", //
                "nuxeo.elasticsearch.service.bulkIndex", //
                "nuxeo.elasticsearch.service.delete", //
                "nuxeo.elasticsearch.service.fetch", //
                "nuxeo.elasticsearch.service.index", //
                "nuxeo.elasticsearch.service.scroll", //
                "nuxeo.elasticsearch.service.search");
        assertNotEquals(expectedMetrics.size(), metrics.size());

        assertNotNull(MockReporter.getFilterForTesting());
        List<String> filteredMetrics = metrics.stream()
                                              .filter(name -> MockReporter.getFilterForTesting()
                                                                          .matches(MetricName.build(name), null))
                                              .collect(Collectors.toList());
        assertEquals(expectedMetrics.size(), filteredMetrics.size());
    }

}
