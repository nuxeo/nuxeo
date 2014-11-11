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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.USERS_GROUPS_MANAGEMENT_ACCESS_FILTER;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * @author dmetzler
 *
 */
public class UserManagementTest extends BaseUserGroupMock {
    UserManagementActions actions = new UserManagementActions();

    @Before
    public void doBefore() throws Exception {
        actions.userManager = mockUserManager();

        // For this test, user is at least a power user
        actions.webActions = mock(WebActions.class);
        when(
                actions.webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)).thenReturn(
                true);
    }

    @Test
    public void aPowerUserShouldNotBeAbleToEditAdminUser() throws Exception {
        // Given a power user (not admin)
        actions.currentUser = getMockedUser("power", false, actions.userManager);
        when(actions.userManager.getPrincipal("power")).thenReturn(
                (NuxeoPrincipal) actions.currentUser);

        // When selected user is administrators
        NuxeoPrincipal user = (NuxeoPrincipal) getMockedUser("Administrator",
                true);
        when(actions.userManager.getPrincipal("Administrator")).thenReturn(user);
        actions.selectedUser = mockUserDM("Administrator");

        // Then it should not be able to edit it
        assertFalse(actions.getAllowEditUser());

        // When selected user is not administrator
        user = (NuxeoPrincipal) getMockedUser("jdoe", false);
        when(actions.userManager.getPrincipal("jdoe")).thenReturn(user);
        actions.selectedUser = mockUserDM("jdoe");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditUser());

    }



    @Test
    public void anAdministratorShouldBeAbleToEditAdminUser() throws Exception {
        // Given a power user (not admin)
        actions.currentUser = getMockedUser("Administrator", true, actions.userManager);

        // When selected user is administrators
        actions.selectedUser = mockUserDM("Administrator");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditUser());

        // When selected user is not administrator
        getMockedUser("jdoe", false,actions.userManager);
        actions.selectedUser = mockUserDM("jdoe");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditUser());

    }

}
