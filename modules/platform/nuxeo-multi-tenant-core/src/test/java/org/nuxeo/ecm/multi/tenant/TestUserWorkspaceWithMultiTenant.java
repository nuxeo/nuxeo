/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.multi.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.NuxeoLoginFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ NuxeoLoginFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.multi.tenant")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
public class TestUserWorkspaceWithMultiTenant {

    @Inject
    protected CoreSession session;

    @Inject
    protected MultiTenantService multiTenantService;

    @Inject
    protected UserManager userManager;

    @Inject
    protected UserWorkspaceService userWorkspaceService;

    @Test
    public void testUserWorkspace() {

        DocumentModel defaultDomain = session.getChild(session.getRootDocument().getRef(), "default-domain");
        assertNotNull(defaultDomain);

        // ensure the multi-tenant is activated
        multiTenantService.enableTenantIsolation(session);

        // create 2 tenants
        DocumentModel domain1 = session.createDocumentModel("/", "domain1", "Domain");
        domain1 = session.createDocument(domain1);
        DocumentModel domain2 = session.createDocumentModel("/", "domain2", "Domain");
        domain2 = session.createDocument(domain2);
        session.save();

        // create 2 tenant bound users
        NuxeoPrincipal mario = createUser("mario", domain1.getName());
        NuxeoPrincipal yoshi = createUser("yoshi", domain2.getName());

        DocumentModel marioWS = userWorkspaceService.getUserPersonalWorkspace(mario, session.getRootDocument());
        DocumentModel yoshiWS = userWorkspaceService.getUserPersonalWorkspace(yoshi, session.getRootDocument());
        assertNotNull(marioWS);
        assertNotNull(yoshiWS);

        assertTrue(marioWS.getPathAsString().contains(domain1.getPathAsString()));
        assertTrue(yoshiWS.getPathAsString().contains(domain2.getPathAsString()));

        DocumentModel marioWS2 = userWorkspaceService.getUserPersonalWorkspace(mario.getName(),
                session.getRootDocument());
        DocumentModel yoshiWS2 = userWorkspaceService.getUserPersonalWorkspace(yoshi.getName(),
                session.getRootDocument());
        assertNotNull(marioWS2);
        assertNotNull(yoshiWS2);

        assertEquals(marioWS.getId(), marioWS2.getId());
        assertEquals(yoshiWS.getId(), yoshiWS2.getId());

        // check admin WS
        DocumentModel adminWS = userWorkspaceService.getCurrentUserPersonalWorkspace(session,
                session.getRootDocument());
        assertNotNull(adminWS);
        assertTrue(adminWS.getPathAsString().contains(defaultDomain.getPathAsString()));

        // check ACLs
        ACP acp = marioWS.getACP();
        System.out.println(acp);
    }

    protected NuxeoPrincipal createUser(String username, String tenant) {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", username);
        user.setPropertyValue("user:tenantId", tenant);
        try {
            userManager.createUser(user);
        } catch (UserAlreadyExistsException e) {
            // do nothing
        } finally {
            session.save();
        }
        return userManager.getPrincipal(username);
    }

}
