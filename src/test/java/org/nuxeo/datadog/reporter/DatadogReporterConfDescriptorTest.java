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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;




@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class DatadogReporterConfDescriptorTest {

    @Test
    public void hostIsComputedFromNuxeoUrl() throws Exception {
        // Given a nuxeo.url property
        OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
        runtime.setProperty("nuxeo.url", "https://nuxeohost.com:8080/nuxeo/");

        //When i have a configuration without defined host
        DatadogReporterConfDescriptor conf = new DatadogReporterConfDescriptor();

        //Then the host is computed from url
        assertThat(conf.getHost()).isEqualTo("nuxeohost.com");
    }

    @Test
    public void hostIsSetFromConfiguration() throws Exception {
        //Given a Datadog configuration
        DatadogReporterConfDescriptor conf = new DatadogReporterConfDescriptor();

        //When the host is set
        conf.host = "myhost.com";

        //The the host refers to the configured value
        assertThat(conf.getHost()).isEqualTo("myhost.com");

    }
}
