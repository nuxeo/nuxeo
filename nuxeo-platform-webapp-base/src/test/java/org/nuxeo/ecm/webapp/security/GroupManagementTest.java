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

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * @author dmetzler
 *
 */
public class GroupManagementTest extends BaseUserGroupMock {

    private GroupManagementActions actions = new GroupManagementActions();

    @Before
    public void doBefore() throws Exception {
        actions.userManager = mockUserManager();

        //For this test, user is at least a power user
        actions.webActions = mock(WebActions.class);
        when(
                actions.webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)).thenReturn(
                true);

    }

    @Test
    public void aPowerUserShouldNotBeAbleToEditAdminGroup() throws Exception {

        // Given a power user (not admin)
        actions.currentUser = getMockedUser("Power",false);

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
        //Given an admin user
        actions.currentUser = getMockedUser("Administrator",true);

        // When selected group is administrators
        actions.selectedGroup = mockGroup("administrators");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditGroup());

        // When selected group is other
        actions.selectedGroup = mockGroup("normalusers");

        // Then it should not be able to edit it
        assertTrue(actions.getAllowEditGroup());
    }

}
