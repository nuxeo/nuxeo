/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.multi.tenant;

import static org.junit.Assert.*;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.rest.api.RestClient;
import org.nuxeo.ecm.automation.client.rest.api.RestResponse;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 *
 *
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = MultiTenantRepositoryInit.class)
@Jetty(port = 18080)
@Deploy({ "org.nuxeo.ecm.multi.tenant", "org.nuxeo.ecm.platform.login",
        "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy({ "org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml",
        "org.nuxeo.ecm.multi.tenant:multi-tenant-enabled-default-test-contrib.xml" })
public class TestRestAPIWithMultiTenant {

    @Inject
    protected MultiTenantService multiTenantService;

    @Inject
    protected DirectoryService directoryService;

    HttpAutomationClient client = new HttpAutomationClient("http://localhost:18080/automation/");



    @Test
    public void restAPICanAccessTenantSpecifyingDomainPart() throws Exception {
        client.getSession("user1", "user1");
        RestClient rclient = client.getRestClient();

        RestResponse response = rclient.newRequest("path/domain1/").execute();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("/domain1", response.asMap().get("path"));

    }

}
