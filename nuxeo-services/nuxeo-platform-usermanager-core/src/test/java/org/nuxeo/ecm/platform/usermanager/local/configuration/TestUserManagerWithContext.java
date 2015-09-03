/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.usermanager.local.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.localconfiguration.DirectoryConfiguration;
import org.nuxeo.ecm.platform.usermanager.DefaultUserMultiTenantManagement;
import org.nuxeo.ecm.platform.usermanager.MultiTenantUserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.ecm.platform.usermanager.UserManagerTestCase;
import org.nuxeo.ecm.platform.usermanager.UserMultiTenantManagement;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * These Test Cases test the usermanager when a Directory Local Configuration is set.
 *
 * @author Benjamin JALON
 */
@Deploy("org.nuxeo.ecm.directory.multi")
@LocalDeploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl-multitenant/directory-for-context-config.xml")
public class TestUserManagerWithContext extends UserManagerTestCase {

    protected Mockery mockery = new JUnit4Mockery();

    MultiTenantUserManager mtum;

    @Before
    public void setUp()  {
        UserMultiTenantManagement umtm = new DefaultUserMultiTenantManagementMock();
        // to simulate the directory local configuration
        ((UserManagerImpl) userManager).multiTenantManagement = umtm;
        mtum = (MultiTenantUserManager) userManager;
    }

    /**
     * Context doc with a local conf using "tenanta".
     */
    protected DocumentModel getContextDoc() {
        DirectoryConfiguration directoryConfiguration = mockery.mock(DirectoryConfiguration.class);
        mockery.checking(new Expectations() {
            {
                allowing(directoryConfiguration).canMerge();
                will(returnValue(Boolean.FALSE));
                allowing(directoryConfiguration).getDirectorySuffix();
                will(returnValue("tenanta"));
            }
        });

        CoreSession session = mockery.mock(CoreSession.class);
        mockery.checking(new Expectations() {
            {
                allowing(session).adaptFirstMatchingDocumentWithFacet(with(any(DocumentRef.class)),
                        with(any(String.class)), with(any(Class.class)));
                will(returnValue(directoryConfiguration));
            }
        });

        DocumentModel doc = mockery.mock(DocumentModel.class);
        mockery.checking(new Expectations() {
            {
                allowing(doc).getCoreSession();
                will(returnValue(session));
                allowing(doc).getRef();
                will(returnValue(new IdRef("123")));
            }
        });
        return doc;
    }

    @Test
    public void testGetAdministratorTenanta() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator@tenanta");
        assertNotNull(principal);
        assertTrue(principal.isMemberOf("administrators-tenanta"));
        assertFalse(principal.isMemberOf("administrators-tenantb"));
    }

    @Test
    public void testShouldReturnOnlyUserFromTenantA() throws Exception {

        DocumentModelList users = mtum.searchUsers("%%", null);

        assertEquals(2, users.size());

        DocumentModel fakeDoc = getContextDoc();
        users = mtum.searchUsers("Administrator", fakeDoc);

        assertEquals(1, users.size());
        assertEquals("Administrator@tenanta", users.get(0).getPropertyValue("username"));
    }

    @Test
    public void testShouldReturnOnlyGroupsFromTenantA() throws Exception {

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        HashSet<String> fulltext = new HashSet<String>();
        DocumentModel fakeDoc = getContextDoc();

        DocumentModelList groups = mtum.searchGroups(filter, fulltext, null);
        assertEquals(4, groups.size());

        groups = mtum.searchGroups(filter, fulltext, fakeDoc);
        assertEquals(2, groups.size());

        filter.put("groupname", "administrators%");
        fulltext.add("groupname");
        groups = mtum.searchGroups(filter, fulltext, fakeDoc);
        assertEquals(1, groups.size());
        assertEquals("administrators-tenanta", groups.get(0).getPropertyValue("groupname"));
    }

    @Test
    public void testShouldAddPrefixToIdWhenGroupCreated() throws Exception {

        DocumentModel fakeDoc = getContextDoc();

        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setPropertyValue("groupname", "test");
        mtum.createGroup(newGroup, fakeDoc);

        DocumentModel group = mtum.getGroupModel("test", fakeDoc);
        String groupIdValue = (String) group.getPropertyValue("groupname");
        assertTrue(groupIdValue.endsWith("-tenanta"));

    }

}
