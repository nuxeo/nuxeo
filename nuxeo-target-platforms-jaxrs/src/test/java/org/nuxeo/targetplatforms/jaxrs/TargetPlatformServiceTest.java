/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.targetplatforms.jaxrs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.DetectThreadDeadlocksFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

@RunWith(FeaturesRunner.class)
@Features({ DetectThreadDeadlocksFeature.class, WebEngineFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.targetplatforms.core")
@Deploy("org.nuxeo.targetplatforms.core.test")
@Deploy("org.nuxeo.targetplatforms.jaxrs")
@Deploy("org.nuxeo.targetplatforms.core:OSGI-INF/test-datasource-contrib.xml")
@Deploy("org.nuxeo.targetplatforms.core:OSGI-INF/test-targetplatforms-contrib.xml")
public class TargetPlatformServiceTest extends BaseTest {

    @Ignore("NXP-17108")
    @Test
    public void ping() throws IOException {
        String url = getBaseURL() + "/target-platforms";
        WebResource resource = getServiceFor(url, "Administrator", "Administrator");
        try (CloseableClientResponse response = CloseableClientResponse.of(
                resource.path("/platforms").accept(APPLICATION_JSON).get(ClientResponse.class))) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String result = IOUtils.toString(response.getEntityInputStream());
            assertTrue(result.contains("nuxeo-dm-5.8"));
        }
    }

}
