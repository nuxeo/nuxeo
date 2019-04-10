/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANTS_DIRECTORY;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ADMINISTRATORS_PROPERTY;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_CONFIG_FACET;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ID_PROPERTY;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig
@Deploy({ "org.nuxeo.ecm.multi.tenant", "org.nuxeo.ecm.platform.login",
        "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
public class TestMultiTenantService {

    @Inject
    protected FeaturesRunner featuresRunner;

    @Inject
    protected CoreSession session;

    @Inject
    protected MultiTenantService multiTenantService;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected UserManager userManager;

    @Before
    public void deleteAllUsers() throws ClientException {
        if (userManager.getPrincipal("bender") != null) {
            userManager.deleteUser("bender");
        }
        if (userManager.getPrincipal("fry") != null) {
            userManager.deleteUser("fry");
        }
        if (userManager.getPrincipal("leela") != null) {
            userManager.deleteUser("leela");
        }
    }

    @Test
    public void serviceRegistration() {
        assertNotNull(multiTenantService);
    }

    @Test
    public void shouldEnableTenantIsolation() throws ClientException {
        assertFalse(multiTenantService.isTenantIsolationEnabled(session));

        multiTenantService.enableTenantIsolation(session);

        assertTrue(multiTenantService.isTenantIsolationEnabled(session));
        DocumentModel domain = session.getDocument(new PathRef(
                "/default-domain"));
        assertNotNull(domain);
        assertTrue(domain.hasFacet(TENANT_CONFIG_FACET));
        assertEquals("default-domain",
                domain.getPropertyValue(TENANT_ID_PROPERTY));

        ACP acp = domain.getACP();
        ACL acl = acp.getOrCreateACL();
        assertNotNull(acl);

        Session session = null;
        try {
            session = directoryService.open(TENANTS_DIRECTORY);
            DocumentModelList docs = session.getEntries();
            assertEquals(1, docs.size());
            DocumentModel doc = docs.get(0);
            assertEquals(domain.getName(), doc.getPropertyValue("tenant:id"));
            assertEquals(domain.getTitle(),
                    doc.getPropertyValue("tenant:label"));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test
    public void shouldDisableTenantIsolation() throws ClientException {
        // make sure the tenant isolation is disabled
        multiTenantService.disableTenantIsolation(session);
        assertFalse(multiTenantService.isTenantIsolationEnabled(session));

        multiTenantService.enableTenantIsolation(session);
        assertTrue(multiTenantService.isTenantIsolationEnabled(session));

        DocumentModel domain = session.getDocument(new PathRef(
                "/default-domain"));
        assertNotNull(domain);
        assertTrue(domain.hasFacet(TENANT_CONFIG_FACET));
        ACL acl = domain.getACP().getOrCreateACL();
        assertNotNull(acl);

        multiTenantService.disableTenantIsolation(session);
        assertFalse(multiTenantService.isTenantIsolationEnabled(session));

        domain = session.getDocument(new PathRef("/default-domain"));
        assertNotNull(domain);
        assertFalse(domain.hasFacet(TENANT_CONFIG_FACET));

        Session session = null;
        try {
            session = directoryService.open(TENANTS_DIRECTORY);
            DocumentModelList docs = session.getEntries();
            assertEquals(0, docs.size());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test
    public void shouldEnableTenantIsolationForNewDomain() throws ClientException {
        multiTenantService.enableTenantIsolation(session);

        DocumentModel newDomain = session.createDocumentModel("/", "newDomain", "Domain");
        newDomain = session.createDocument(newDomain);
        session.save();
        assertTrue(newDomain.hasFacet(TENANT_CONFIG_FACET));
        assertEquals(newDomain.getName(),
                newDomain.getPropertyValue(TENANT_ID_PROPERTY));

        Session session = null;
        try {
            session = directoryService.open(TENANTS_DIRECTORY);
            DocumentModelList docs = session.getEntries();
            assertEquals(2, docs.size());
            DocumentModel doc = docs.get(1);
            assertEquals(newDomain.getName(), doc.getPropertyValue("tenant:id"));
            assertEquals(newDomain.getTitle(),
                    doc.getPropertyValue("tenant:label"));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test
    public void shouldGiveManageEverythingRightForTenantManager()
            throws ClientException, LoginException {
        multiTenantService.enableTenantIsolation(session);

        DocumentModel domain = session.getDocument(new PathRef(
                "/default-domain"));
        assertNotNull(domain);
        assertTrue(domain.hasFacet(TENANT_CONFIG_FACET));
        assertEquals(domain.getName(),
                domain.getPropertyValue(TENANT_ID_PROPERTY));

        NuxeoPrincipal bender = createUser("bender", domain.getName());
        LoginContext loginContext = Framework.loginAsUser("bender");
        CoreSession benderSession = openSession(bender);
        assertTrue(benderSession.hasPermission(domain.getRef(),
                SecurityConstants.READ));
        assertFalse(benderSession.hasPermission(domain.getRef(),
                SecurityConstants.EVERYTHING));
        CoreInstance.getInstance().close(benderSession);
        loginContext.logout();

        domain.setPropertyValue(TENANT_ADMINISTRATORS_PROPERTY,
                (Serializable) Arrays.asList("bender"));
        session.saveDocument(domain);
        session.save();

        bender = userManager.getPrincipal(bender.getName());
        loginContext = Framework.loginAsUser("bender");
        benderSession = openSession(bender);
        benderSession.save();
        assertTrue(benderSession.hasPermission(domain.getRef(),
                SecurityConstants.READ));
        assertTrue(benderSession.hasPermission(domain.getRef(),
                SecurityConstants.EVERYTHING));
        CoreInstance.getInstance().close(benderSession);
        loginContext.logout();
    }

    @Test
    public void tenantManagerShouldCreateGroupsForTenant()
            throws ClientException, LoginException {
        multiTenantService.enableTenantIsolation(session);

        DocumentModel domain = session.getDocument(new PathRef(
                "/default-domain"));

        createUser("fry", domain.getName());
        LoginContext loginContext = Framework.loginAsUser("fry");

        NuxeoGroup nuxeoGroup = createGroup("testGroup");
        assertEquals("tenant_" + domain.getName() + "_testGroup",
                nuxeoGroup.getName());

        List<DocumentModel> groups = userManager.searchGroups(null);
        assertEquals(1, groups.size());
        DocumentModel group = groups.get(0);
        assertEquals("tenant_" + domain.getName() + "_testGroup",
                group.getPropertyValue("group:groupname"));
        assertEquals(domain.getName(), group.getPropertyValue("group:tenantId"));

        loginContext.logout();

        // other user not belonging to the tenant cannot see the group
        createUser("leela", "nonExistingTenant");
        loginContext = Framework.loginAsUser("leela");

        groups = userManager.searchGroups(null);
        assertEquals(0, groups.size());

        loginContext.logout();
    }

    @Test
    public void shouldGiveWriteRightOnTenant() throws ClientException, LoginException {
        multiTenantService.enableTenantIsolation(session);

        DocumentModel domain = session.getDocument(new PathRef(
                "/default-domain"));
        domain.setPropertyValue(TENANT_ADMINISTRATORS_PROPERTY,
                (Serializable) Arrays.asList("fry"));
        session.saveDocument(domain);
        session.save();

        NuxeoPrincipal fry = createUser("fry", domain.getName());
        LoginContext loginContext = Framework.loginAsUser("fry");

        NuxeoGroup nuxeoGroup = createGroup("supermembers");
        assertEquals("tenant_" + domain.getName() + "_supermembers",
                nuxeoGroup.getName());
        CoreSession frySession = openSession(fry);

        // add the Read ACL
        DocumentModel doc = frySession.getDocument(domain.getRef());
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(0, new ACE(nuxeoGroup.getName(), "Write", true));
        doc.setACP(acp, true);
        frySession.saveDocument(doc);
        frySession.save();

        CoreInstance.getInstance().close(frySession);
        loginContext.logout();

        // bender is part of the supermembers group
        NuxeoPrincipal bender = createUser("bender", domain.getName());
        bender.setGroups(Arrays.asList(nuxeoGroup.getName()));
        userManager.updateUser(bender.getModel());
        bender = createUser("bender", domain.getName());
        loginContext = Framework.loginAsUser("bender");
        CoreSession benderSession = openSession(bender);

        assertTrue(benderSession.hasPermission(domain.getRef(), "Write"));

        CoreInstance.getInstance().close(benderSession);
        loginContext.logout();

        // leela does not have Write permission
        NuxeoPrincipal leela = createUser("leela", domain.getName());
        loginContext = Framework.loginAsUser("leela");
        CoreSession leelaSession = openSession(leela);

        assertTrue(leelaSession.hasPermission(domain.getRef(), "Read"));
        assertFalse(leelaSession.hasPermission(domain.getRef(), "Write"));

        CoreInstance.getInstance().close(leelaSession);
        loginContext.logout();
    }

    protected CoreSession openSession(NuxeoPrincipal principal)
            throws ClientException {
        CoreFeature coreFeature = featuresRunner.getFeature(CoreFeature.class);
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        return coreFeature.getRepository().getRepositoryHandler().openSession(
                ctx);
    }

    protected NuxeoPrincipal createUser(String username, String tenant)
            throws ClientException {
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

    protected NuxeoGroup createGroup(String groupName) throws ClientException {
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", groupName);
        String computedGroupName = groupName;
        try {
            computedGroupName = userManager.createGroup(group).getId();
        } finally {
            session.save();
        }
        return userManager.getGroup(computedGroupName);
    }
}
