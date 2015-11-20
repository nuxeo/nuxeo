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
package org.nuxeo.ftest.cap;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.NavigationSubPage;
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Tests the contextual menu actions
 */
public class ITContextualActionsTest extends AbstractTest {

    private static final Log log = getLog(ITContextualActionsTest.class);

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
        DocumentBasePage workspacePage = workspaceFormPage.createNewWorkspace(WORKSPACE_NAME, WORKSPACE_DESC);

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = createFile(workspacePage, DOCUMENT_NAME, DOCUMENT_DESC, true, filePrefix,
                ".txt", "Webdriver test file content.");

        // Verify summary informations
        Assert.assertEquals(DOCUMENT_DESC, filePage.getCurrentDocumentDescription());
        Assert.assertEquals(DOCUMENT_NAME, filePage.getCurrentDocumentTitle());
        List<String> states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_STATE));
        Assert.assertEquals("Administrator", filePage.getCurrentContributors());

        // Test contextual actions
        ContextualActions actions = filePage.getContextualActions();

        // Test favorites action
        actions.clickOnButton(actions.favoritesButton);
        actions = filePage.getContextualActions();

        // Test lock action
        actions.clickOnButton(actions.lockButton);
        actions = filePage.getContextualActions();
        states = filePage.getCurrentStates();
        Assert.assertTrue(states.contains(DOCUMENT_LOCKED));

        // Test permalink action
        actions.clickOnButton(actions.permaButton);
        actions = filePage.getContextualActions();
        // wait for element to be shown to close it, otherwise DOM may not be
        // udpated yet
        actions.findElementWithTimeout(By.className(actions.permaBoxFocusName), 20 * 1000);
        actions.clickOnButton(actions.closePermaBoxButton);
        actions = filePage.getContextualActions();

        // Test follow action
        actions.openMore();
        actions = filePage.getContextualActions();
        actions.clickOnButton(actions.followButton);
        actions = filePage.getContextualActions();

        // Test Add to Worklist action
        actions.openMore();
        actions = filePage.getContextualActions();
        actions.clickOnButton(actions.addToWorklistButton);
        actions = filePage.getContextualActions();
        // Test More button & Export
        actions.openMore();
        actions = filePage.getContextualActions();
        actions.clickOnButton(actions.exportButton);
        actions = filePage.getContextualActions();
        waitForExportPopup();

        logout();
    }

    private void waitForExportPopup() {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(5, TimeUnit.SECONDS)
                                                                .pollingEvery(100, TimeUnit.MILLISECONDS)
                                                                .ignoring(NoSuchElementException.class);
        try {
            wait.until(new Function<WebDriver, WebElement>() {
                @Override
                public WebElement apply(WebDriver driver) {
                    return driver.findElement(By.linkText("XML Export"));
                }
            });
        } catch (TimeoutException e) {
            log.warn("Could not see Export popup.");
        }
    }
}
