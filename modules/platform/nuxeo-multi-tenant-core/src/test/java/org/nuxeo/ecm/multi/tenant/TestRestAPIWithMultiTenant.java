/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.multi.tenant;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = MultiTenantRepositoryInit.class)
@Deploy("org.nuxeo.ecm.multi.tenant")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-enabled-default-test-contrib.xml")
public class TestRestAPIWithMultiTenant extends BaseTest {

    @Test
    public void restAPICanAccessTenantSpecifyingDomainPart() throws IOException {
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "path/domain1/");
                InputStream stream = response.getEntityInputStream()) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(stream);
            assertEquals("/domain1", node.get("path").asText());
        }
    }

}
