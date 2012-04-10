/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests;

import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;

/**
 * <p>
 * Test Modifying a workspace description in Nuxeo DM.
 * </p>
 * <p>
 * Requirements: the user jsmith is created
 * </p>
 * <ol>
 * <li>loginAs jsmith</li>
 * <li>followLink to testWorkspace1</li>
 * <li>modifyWorkspaceDescription</li>
 * <li>logout</li>
 * </ol>
 */
public class ITModifyWorkspaceDescriptionTest extends AbstractTest {

    @Test
    public void testModifyWsDescription() throws Exception {

        // As an administrator, check that jsmith is created and has rights
        UsersGroupsBasePage usergroupPage = login().getAdminCenter().getUsersGroupsHomePage();
        UsersTabSubPage page = usergroupPage.getUsersTab().searchUser("jsmith");
        if (!page.isUserFound("jsmith")) {
            usergroupPage = page.getUserCreatePage().createUser("jsmith",
                    "John", "Smith", "Nuxeo", "jsmith@nuxeo.com", "jsmith1",
                    "members");
        }

        DocumentBasePage documentBasePage = usergroupPage.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        AccessRightsSubPage accessRightSubTab = documentBasePage.getManageTab().getAccessRightsSubTab();
        // Need WriteSecurity (so in practice Manage everything) to edit a
        // Workspace
        if (!accessRightSubTab.hasPermissionForUser("Manage everything",
                "jsmith")) {
            accessRightSubTab.addPermissionForUser("jsmith",
                    "Manage everything", true);
        }

        logout();

        // Starting the test for real
        documentBasePage = login("jsmith", "jsmith1").getContentTab().goToDocument(
                "Workspaces");

        // Create a new workspace named 'WorkspaceDescriptionModify_{current
        // time}'
        String workspaceTitle = "WorkspaceDescriptionModify_"
                + new Date().getTime();
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                workspaceTitle, "A workspace description");

        // Modify Workspace description
        String descriptionModified = "Description modified";
        workspacePage = workspacePage.getEditTab().edit(null,
                descriptionModified, null);

        assertEquals(descriptionModified,
                workspacePage.getCurrentDocumentDescription());
        assertEquals(workspaceTitle, workspacePage.getCurrentDocumentTitle());

        // Clean up repository
        deleteWorkspace(workspacePage, workspaceTitle);

        // Logout
        logout();

    }

}
