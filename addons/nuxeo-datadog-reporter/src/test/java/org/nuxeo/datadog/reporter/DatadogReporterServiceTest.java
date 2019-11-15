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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.datadog.reporter")
public class DatadogReporterServiceTest {

    @Inject
    protected DatadogReporterService dds;

    @Test
    public void testNoDatadogAPIKeyProperty() {
        DatadogReporterServiceImpl rs = (DatadogReporterServiceImpl) dds;

        // default Datadog configuration
        DatadogReporterConfDescriptor configuration = rs.getConfig();
        assertNotNull(configuration);
        assertTrue(configuration.getApiKey().isBlank());
        assertTrue(configuration.getHost().isBlank());
        assertEquals(10L, configuration.getPollInterval());
        assertEquals(List.of("nuxeo"), configuration.getTags());

        // Datadog API key not provided: no reporter
        assertNull(rs.getReporter());
    }

    @Test
    @Deploy("org.nuxeo.datadog.reporter:test-datadog-contrib-no-filter.xml")
    public void testDatadogAPIKeyProperty() {
        DatadogReporterServiceImpl rs = (DatadogReporterServiceImpl) dds;

        // test Datadog configuration
        DatadogReporterConfDescriptor configuration = rs.getConfig();
        assertNotNull(configuration);
        assertEquals("DATADOG_API_KEY", configuration.getApiKey());
        assertEquals("testhost.com", configuration.getHost());
        assertEquals(25L, configuration.getPollInterval());
        assertTrue(configuration.getTags().isEmpty());

        // Datadog API key provided
        assertNotNull(rs.getReporter());
    }

}
