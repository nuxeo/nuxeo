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

import static org.assertj.core.api.Assertions.assertThat;

import org.coursera.metrics.datadog.DatadogReporter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.datadog.reporter")
@Deploy("org.nuxeo.datadog.reporter:datadog-contrib.xml")
public class DatadogReporterServiceTest {


    @Inject
    DatadogReporterService dds;


    @Test
    public void serviceIsDeployed() throws Exception {
        assertThat(dds).isNotNull();
    }

    @Test
    public void serviceRegisterDatadogReporter() throws Exception {
        DatadogReporterServiceImpl rs = (DatadogReporterServiceImpl) dds;
        DatadogReporter reporter = rs.getReporter();
        assertThat(reporter).isNotNull();
        DatadogReporterConfDescriptor config = rs.getConfig();
        assertThat(config.getApiKey()).isEqualTo("DATADOG_API_KEY");
        assertThat(config.getPollInterval()).isEqualTo(25L);
        assertThat(config.getHost()).isEqualTo("testhost.com");

    }
}
