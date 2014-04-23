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
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;

/**
 * Tests the contextual menu actions
 */
public class ITContextualActionsTest extends AbstractTest {

    public final static String WORKSPACE_ROOT = "Workspaces";

    public final static String WORKSPACE_NAME = "WorkspaceTest";

    public final static String WORKSPACE_DESC = "Workspace Test Description";

    public final static String DOCUMENT_NAME = "DocumentTest";

    public final static String DOCUMENT_DESC = "Document Test Description";

    public final static String DOCUMENT_STATE = "Project";

    public final static String DOCUMENT_LOCKED = "Locked";

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
        DocumentBasePage workspacePage = workspaceFormPage.createNewWorkspace(
                WORKSPACE_NAME, WORKSPACE_DESC);

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = createFile(workspacePage,
                DOCUMENT_NAME, DOCUMENT_DESC, true, filePrefix, ".txt",
                "Webdriver test file content.");

        // Verify summary informations
        Assert.assertEquals(DOCUMENT_DESC,
                filePage.getCurrentDocumentDescription());
        Assert.assertEquals(DOCUMENT_NAME, filePage.getCurrentDocumentTitle());
        List<String> states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_STATE));
        Assert.assertEquals("Administrator", filePage.getCurrentContributors());

        // Test contextual actions
        ContextualActions actions = filePage.getContextualActions();

        // Test favorites action
        actions.clickOnButton(actions.favoritesButton);

        // Test lock action
        actions.clickOnButton(actions.lockButton);
        states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_LOCKED));

        // Test download action
        actions.clickOnButton(actions.downloadButton);
        // Test permalink action
        actions.clickOnButton(actions.permaButton);
        // wait for element to be shown to close it, otherwise DOM may not be
        // udpated yet
        actions.findElementWithTimeout(By.className(actions.permaBoxFocusName),
                20 * 1000);
        actions.clickOnButton(actions.closePermaBoxButton);

        // Test follow action
        actions.clickOnButton(actions.moreButton);
        actions.clickOnButton(actions.followButton);

        // Test Add to Worklist action
        actions.clickOnButton(actions.moreButton);
        actions.clickOnButton(actions.addToWorklistButton);
        // Test More button & Export
        actions.clickOnButton(actions.moreButton);
        actions.clickOnButton(actions.exportButton);

        logout();
    }
}
