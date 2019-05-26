/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webapp.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.USERS_GROUPS_MANAGEMENT_ACCESS_FILTER;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author dmetzler
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core:test-schema-contrib.xml")
public class GroupManagementTest extends BaseUserGroupMock {

    private GroupManagementActions actions = new GroupManagementActions();

    @Before
    public void doBefore() throws Exception {
        actions.userManager = mockUserManager();

        // For this test, user is at least a power user
        actions.webActions = mock(WebActions.class);
        when(actions.webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)).thenReturn(true);

    }

    @Test
    public void aPowerUserShouldNotBeAbleToEditAdminGroup() throws Exception {

        // Given a power user (not admin)
        actions.currentUser = getMockedUser("Power", false);

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
        // Given an admin user
        actions.currentUser = getMockedUser("Administrator", true);

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
