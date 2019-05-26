/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ftest.cap;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.NavigationSubPage;
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.nuxeo.functionaltests.pages.forms.WorkspaceCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;

import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;

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

    @After
    public void after() {
        RestHelper.deleteDocument(WORKSPACES_PATH + WORKSPACE_NAME);
    }

    @Test
    public void verifyContextualActions() throws Exception {

        // Login
        DocumentBasePage home = login();

        // Navigate to workspaces root
        NavigationSubPage domainContent = home.getNavigationSubPage();
        DocumentBasePage workspacesPage = domainContent.goToDocument(WORKSPACE_ROOT);
        WorkspacesContentTabSubPage workspacesContent = workspacesPage.getWorkspacesContentTab();

        // Create a workspace and navigate into it
        WorkspaceCreationFormPage workspaceFormPage = workspacesContent.getWorkspaceCreatePage();
        DocumentBasePage workspacePage = workspaceFormPage.createNewWorkspace(WORKSPACE_NAME, WORKSPACE_DESC);

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = workspacePage.createFile(DOCUMENT_NAME, DOCUMENT_DESC, true, filePrefix, ".txt",
                "Webdriver test file content.");

        // Verify summary informations
        Assert.assertEquals(DOCUMENT_DESC, filePage.getCurrentDocumentDescription());
        Assert.assertEquals(DOCUMENT_NAME, filePage.getCurrentDocumentTitle());
        List<String> states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_STATE));
        Assert.assertEquals("Administrator", filePage.getCurrentContributors());

        // Test contextual actions
        ContextualActions actions = filePage.getContextualActions();

        // Test favorites action
        actions = actions.clickOnButton(actions.favoritesButton);

        // Test lock action
        actions = actions.clickOnButton(actions.lockButton);
        states = asPage(FileDocumentBasePage.class).getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_LOCKED));

        // Test permalink action
        actions = actions.clickOnButton(actions.permaButton);
        // wait for element to be shown to close it, otherwise DOM may not be
        // updated yet
        Locator.findElementWithTimeout(By.className(actions.permaBoxFocusName), 20 * 1000);
        actions = asPage(ContextualActions.class).closeFancyPermalinBox();

        // Test follow action
        actions = actions.openMore().clickOnButton(actions.followButton);

        // Test More button & Add to Worklist action
        actions = actions.openMore().clickOnButton(actions.addToWorklistButton);
        // Test More button & Export
        actions = actions.openMore().clickOnButton(actions.exportButton);
        Locator.findElementWithTimeout(By.linkText(actions.xmlExportTitle), 20 * 1000);

        logout();
    }

}
