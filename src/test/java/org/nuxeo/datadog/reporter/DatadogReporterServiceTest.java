/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.datadog.reporter")
@LocalDeploy("org.nuxeo.datadog.reporter:datadog-contrib.xml")
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
