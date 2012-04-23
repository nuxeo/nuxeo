/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.functionaltests;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.NavigationSubPage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;

/**
 * Tests the contextual menu actions
 */
public class ITContextualActionsTest extends AbstractTest {

    public final static String WORKSPACE_ROOT = "Workspaces";

    public final static String WORKSPACE_NAME = "WorkspaceTest";

    public final static String WORKSPACE_DESC = "Workspace Test Description";

    public final static String DOCUMENT_NAME = "DocumentTest";

    public final static String DOCUMENT_DESC = "Document Test Description";

    public final static String DOCUMENT_STATE = "PROJECT";

    public final static String DOCUMENT_LOCKED = "LOCKED";

    public final static String NOTE_TYPE = "Note";

    @Test
    public void verifyContextualActions() throws Exception {

        // Login
        DocumentBasePage home = login();

        // Navigate to workspaces root
        NavigationSubPage domainContent = home.getNavigationSubPage();
        DocumentBasePage workspacesPage = domainContent.goToDocument(WORKSPACE_ROOT);
        WorkspacesContentTabSubPage workspacesContent = workspacesPage.getWorkspacesContentTab();

        // Create a workspace and navigate into it
        WorkspaceFormPage workspaceFormPage = workspacesContent.getWorkspaceCreatePage();
        workspaceFormPage.createNewWorkspace(WORKSPACE_NAME, WORKSPACE_DESC);

        // Create a file and navigate into it
        FileCreationFormPage fileFormPage = workspacesContent.getDocumentCreatePage(
                NOTE_TYPE, FileCreationFormPage.class);
        FileDocumentBasePage filePage = fileFormPage.createFileDocument(
                DOCUMENT_NAME, DOCUMENT_DESC);

        // Verify summary informations
        Assert.assertEquals(DOCUMENT_DESC,
                filePage.getCurrentDocumentDescription());
        Assert.assertEquals(DOCUMENT_NAME, filePage.getCurrentDocumentTitle());
        List<String> states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_STATE));
        Assert.assertEquals("Administrator", filePage.getCurrentContributors());

        // Test lock action
        filePage.clickOnButton(filePage.lockButton);
        states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_LOCKED));

        // Test follow action
        filePage.clickOnButton(filePage.followButton);
        // Test Add to Worklist action
        filePage.clickOnButton(filePage.addToWorklistButton);
        // Test More button & Export
        filePage.clickOnButton(filePage.moreButton);
        filePage.clickOnButton(filePage.exportButton);

        // Test permalink action
//        filePage.clickOnButton(filePage.permaButton);
//        filePage.clickOnButton(filePage.closePermaBoxButton);

        // Log out
        navToUrl("http://localhost:8080/nuxeo/logout");
    }
}
