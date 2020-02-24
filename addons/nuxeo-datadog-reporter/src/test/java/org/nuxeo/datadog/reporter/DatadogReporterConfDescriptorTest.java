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

import java.util.EnumSet;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.datadog.reporter")
@Deploy("org.nuxeo.datadog.reporter.test:test-datadog-contrib.xml")
public class DatadogReporterConfDescriptorTest {

    @Inject
    protected DatadogReporterService reporter;

    @Test
    public void hostIsComputedFromNuxeoUrl() {
        // Given a nuxeo.url property
        OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
        runtime.setProperty("nuxeo.url", "https://nuxeohost.com:8080/nuxeo/");

        // When i have a configuration without defined host
        DatadogReporterConfDescriptor conf = new DatadogReporterConfDescriptor();

        // Then the host is computed from url
        assertEquals("nuxeohost.com", conf.getHost());
    }

    @Test
    public void hostIsSetFromConfiguration() {
        // Given a Datadog configuration
        DatadogReporterConfDescriptor conf = new DatadogReporterConfDescriptor();

        // When the host is set
        conf.host = "myhost.com";

        // The the host refers to the configured value
        assertEquals("myhost.com", conf.getHost());
    }

    @Test
    public void canConfigureMetricFilter() {
        DatadogReporterServiceImpl service = (DatadogReporterServiceImpl) reporter;
        DatadogReporterConfDescriptor config = service.getConfig();
        assertNotNull(config);

        assertFalse(config.filter.getUseRegexFilters());
        assertTrue(config.filter.getUseSubstringMatching());

        MetricFilter filter = service.getFilter();

        assertTrue(filter.matches(MetricName.build("jvm.useful"), null));
        assertFalse(filter.matches(MetricName.build("jvm.useless"), null));
        assertTrue(filter.matches(MetricName.build("nuxeo.all"), null));
    }

    @Test
    public void canConfigureExpansions() {
        DatadogReporterServiceImpl service = (DatadogReporterServiceImpl) reporter;
        DatadogReporterConfDescriptor config = service.getConfig();

        assertEquals(EnumSet.of(NuxeoDatadogReporter.Expansion.P99, NuxeoDatadogReporter.Expansion.COUNT),
                config.filter.getExpansions());
    }

}
