/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.tests;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.google.inject.Inject;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMService;

@RunWith(FeaturesRunner.class)
@Features({ ScimFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = ScimServerInit.class)
@Ignore("Unable to make it run reliability because of internal parser issues ...")
public class ScimServerTest {

    @Inject
    CoreSession session;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Test
    public void shouldListUsers() throws Exception {

        int port = servletContainerFeature.getPort();
        final URI uri = URI.create("http://localhost:" + port + "/scim/v1/");

        final SCIMService scimService = new SCIMService(uri, "user0", "user0");
        scimService.setAcceptType(MediaType.APPLICATION_JSON_TYPE);

        // Core user resource CRUD operation example
        final SCIMEndpoint<UserResource> endpoint = scimService.getUserEndpoint();

        Assert.assertNotNull(endpoint);

        UserResource u = endpoint.get("user0");

        Assert.assertEquals("user0", u.getId());
        Assert.assertEquals("Steve", u.getName().getGivenName());
        Assert.assertEquals("Jobs", u.getName().getFamilyName());
        Assert.assertEquals("http://localhost:" + port + "/scim/v1/Users/user0", u.getMeta().getLocation().toString());

        List<String> actualGroups = new ArrayList<>();
        for (Entry<String> group : u.getGroups()) {
            actualGroups.add(group.getValue());
        }
        Assert.assertTrue(actualGroups.contains("powerusers"));
        Assert.assertTrue(actualGroups.contains("defgr"));
        Assert.assertEquals(2, actualGroups.size());

        Resources<UserResource> users = endpoint.query(null);
        Assert.assertEquals(5, users.getTotalResults());

        users = endpoint.query("userName eq user1");
        Assert.assertEquals(1, users.getTotalResults());
        Assert.assertEquals("user1", users.iterator().next().getId());

    }

}
