/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;

/**
 * Test workspace structure creation.
 *
 * @since 8.2
 */
public class ITWorkspaceTest extends AbstractTest {

    protected final static String WORKSPACE_TITLE = "Test Workspace " + new Date().getTime();

    protected static final String WORKSPACE_DESCRIPTION = "Workspace Description" + new Date().getTime();

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.addPermission(WORKSPACES_PATH, TEST_USERNAME, "Write");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testWorkspaceStructure() throws UserNotConnectedException, IOException {
        try {
            DocumentBasePage documentBasePage = loginAsTestUser();
            DocumentBasePage wsPage = documentBasePage.createWorkspace(WORKSPACE_TITLE, WORKSPACE_DESCRIPTION);
            checkAvailableTabs();
            wsPage.getContentTab().createFolder("My folder", "folder desc");
            checkAvailableTabs();
        } finally {
            asPage(DocumentBasePage.class).deleteWorkspace(WORKSPACE_TITLE);
            logout();
        }
    }

    protected void checkAvailableTabs() {
        asPage(DocumentBasePage.class).getContentTab();
        asPage(DocumentBasePage.class).getEditTab();
        // click on "permissions" tab, but do not require addition/removal right
        DocumentBasePage page = asPage(DocumentBasePage.class);
        page.clickOnDocumentTabLink(page.permissionsTabLink, false);
        asPage(DocumentBasePage.class).getHistoryTab();
    }

    @Test
    public void testDeleteWorkspace() throws Exception {
        // First create workspace as Administrator
        login().createWorkspace(WORKSPACE_TITLE, WORKSPACE_DESCRIPTION).createNote("Note to delete",
                "Note description to delete", false, null);
        logout();

        // Delete it as Test User
        DocumentBasePage workspacesPage = loginAsTestUser().goToWorkspaces().goToDocumentWorkspaces();
        ContentTabSubPage contentTabPage = workspacesPage.getContentTab().selectByTitle(WORKSPACE_TITLE);
        WebElement deleteLink = driver.findElementById("document_content_buttons:nxw_CURRENT_SELECTION_TRASH_form:nxw_CURRENT_SELECTION_TRASH");
        assertTrue(deleteLink.isEnabled());
        assertTrue(driver.findElementById(
                "document_content_buttons:nxw_CURRENT_SELECTION_ADDTOLIST_form:nxw_CURRENT_SELECTION_ADDTOLIST")
                         .isEnabled());
        // Delete the workspace then cancel it on confirmation
        deleteLink.click();
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete selected document(s)?", alert.getText());
        alert.dismiss();
        // De-select workspace to delete as removeDocument() select it
        contentTabPage = contentTabPage.getContentTab().selectByTitle(WORKSPACE_TITLE);
        contentTabPage = contentTabPage.removeDocument(WORKSPACE_TITLE).asPage(ContentTabSubPage.class);
        assertFalse(contentTabPage.getChildDocumentRows()
                                  .stream()
                                  .filter(element -> WORKSPACE_TITLE.equals(element.getText()))
                                  .findAny()
                                  .isPresent());

    }

}
