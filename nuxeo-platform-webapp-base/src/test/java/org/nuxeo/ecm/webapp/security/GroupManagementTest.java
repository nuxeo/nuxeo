/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.security;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.USERS_GROUPS_MANAGEMENT_ACCESS_FILTER;

import java.security.Principal;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * @author dmetzler
 *
 */
public class GroupManagementTest {

    private static final String GROUP_IDFIELD = "name";

    private static final String GROUP_SCHEMA = "group";

    private GroupManagementActions actions = new GroupManagementActions();

    @Before
    public void doBefore() throws Exception {
        actions.userManager = mockUserManager();

    }

    @Test
    public void aPowerUserShouldNotBeAbleToEditAdminGroup() throws Exception {

        // Given a power user (not admin)
        actions.currentUser = getMockedUser(false);

        actions.webActions = mock(WebActions.class);
        when(
                actions.webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)).thenReturn(
                true);

        // When selected group is administrators
        actions.selectedGroup = mockGroup("administrators");

        // Then it should not be able to edit it
        assertFalse(actions.getAllowEditGroup());

        // When selected group is not administrators
        actions.selectedGroup = mockGroup("normalusers");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditGroup());

    }

    @Test
    public void anAdministratorShouldBeAbleToEditAdminGroup() throws Exception {
        actions.currentUser = getMockedUser(true);

        actions.webActions = mock(WebActions.class);
        when(
                actions.webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)).thenReturn(
                true);

        // When selected group is administrators
        actions.selectedGroup = mockGroup("administrators");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditGroup());

     // When selected group is other
        actions.selectedGroup = mockGroup("normalusers");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditGroup());
    }




    /**
     * Return a mocked principal wich is admin or not
     * @param admin
     * @return
     */
    private Principal getMockedUser(boolean admin) {
        Principal user = mock(NuxeoPrincipal.class);
        when(((NuxeoPrincipal) user).isAdministrator()).thenReturn(admin);
        return user;
    }

    /**
     * Return a mocked group
     * @param groupName
     * @return
     * @throws Exception
     */
    private DocumentModel mockGroup(String groupName) throws Exception {

        DocumentModel group = mock(DocumentModel.class);
        when(group.getPropertyValue(GROUP_SCHEMA + ":" + GROUP_IDFIELD)).thenReturn(
                groupName);

        // Just for BaseSession#isReadOnly to work
        when(group.getContextData()).thenReturn(new ScopedMap());

        return group;
    }


    private UserManager mockUserManager() throws ClientException {
        UserManager um = mock(UserManager.class);
        when(um.areGroupsReadOnly()).thenReturn(false);
        when(um.getAdministratorsGroups()).thenReturn(
                Arrays.asList(new String[] { "administrators" }));
        when(um.getGroupSchemaName()).thenReturn(GROUP_SCHEMA);
        when(um.getGroupIdField()).thenReturn(GROUP_IDFIELD);
        return um;
    }
}
