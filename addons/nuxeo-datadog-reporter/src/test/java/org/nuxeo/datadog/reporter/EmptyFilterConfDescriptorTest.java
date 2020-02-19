/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import io.dropwizard.metrics5.MetricFilter;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.datadog.reporter")
@Deploy("org.nuxeo.datadog.reporter.test:test-datadog-contrib-no-filter.xml")
public class EmptyFilterConfDescriptorTest {

    @Inject
    protected DatadogReporterService reporter;

    @Test
    public void canConfigureMetricFilter() {
        DatadogReporterServiceImpl service = (DatadogReporterServiceImpl) reporter;
        DatadogReporterConfDescriptor config = service.getConfig();
        assertNotNull(config);

        assertFalse(config.filter.getUseRegexFilters());
        assertFalse(config.filter.getUseSubstringMatching());

        MetricFilter filter = service.getFilter();

        assertTrue(filter.matches("jvm.useful", null));
        assertTrue(filter.matches("jvm.useless", null));
        assertTrue(filter.matches("nuxeo.all", null));
    }

    @Test
    public void canConfigureExpansions() {
        DatadogReporterServiceImpl service = (DatadogReporterServiceImpl) reporter;
        DatadogReporterConfDescriptor config = service.getConfig();

        assertEquals(Expansion.ALL, config.filter.getExpansions());
    }

}
