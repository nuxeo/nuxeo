/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.TrashSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Tests for the manage tab.
 *
 * @since 7.1
 */
public class ITManageTest extends AbstractTest {

    protected final static String TEST_FILE_TITLE = "Trash Test File " + new Date().getTime();

    protected final static String WORKSPACE_TITLE = "Trash Test Workspace " + new Date().getTime();

    /**
     * Test trash management.
     *
     * @since 7.1
     */
    @Test
    public void testTrashPurge() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        // Create a file under workspace.
        DocumentBasePage workspacePage = documentBasePage.createWorkspace(WORKSPACE_TITLE, null);
        DocumentBasePage filePage = workspacePage.createFile(TEST_FILE_TITLE, null, false, null, null, null);

        // Copy/paste it to get 5 files.
        ContentTabSubPage content = filePage.getHeaderLinks()
                                            .getNavigationSubPage()
                                            .goToDocument(WORKSPACE_TITLE)
                                            .getContentTab()
                                            .copyByTitle(TEST_FILE_TITLE)
                                            .paste()
                                            .paste()
                                            .paste()
                                            .paste();

        List<WebElement> docs = content.getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(5, docs.size());
        // Select all files and remove them.
        docs = content.removeAllDocuments().getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(0, docs.size());

        // Go to trash page.
        TrashSubPage trashSubPage = asPage(DocumentBasePage.class).getManageTab().getTrashSubTab();
        docs = trashSubPage.getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(5, docs.size());

        // Empty the trash.
        docs = trashSubPage.emptyTrash().getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(0, docs.size());

        // cleanup workspace
        asPage(DocumentBasePage.class).getNavigationSubPage().goToDocument("Workspaces");
        asPage(ContentTabSubPage.class).removeDocument(WORKSPACE_TITLE);
    }

    /**
     * NXP-18328: Non-regression tests for ajaxified tabs cache issue when displaying subtabs.
     *
     * @since 8.1
     */
    @Test
    public void testManageTabContent() throws UserNotConnectedException, IOException {
        login().getManageTab();
        assertEquals(1, driver.findElements(By.xpath("//a[contains(@id,'nxw_TAB_TRASH_CONTENT')]/span")).size());
        assertEquals(1, driver.findElements(By.xpath("//a[contains(@id,'nxw_TAB_LOCAL_CONFIGURATION')]/span")).size());
        assertEquals(0, driver.findElements(By.xpath("//a[contains(@id,'nxw_TAB_EVENTS')]/span")).size());
        asPage(DocumentBasePage.class).getHistoryTab();
        assertEquals(0, driver.findElements(By.xpath("//a[contains(@id,'nxw_TAB_TRASH_CONTENT')]/span")).size());
        assertEquals(0, driver.findElements(By.xpath("//a[contains(@id,'nxw_TAB_LOCAL_CONFIGURATION')]/span")).size());
        assertEquals(1, driver.findElements(By.xpath("//a[contains(@id,'nxw_TAB_EVENTS')]/span")).size());
    }

}
