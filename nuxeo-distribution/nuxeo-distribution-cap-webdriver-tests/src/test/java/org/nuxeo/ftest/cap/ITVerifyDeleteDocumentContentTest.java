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
 *     Gabriel Barata
 */

package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.TrashSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FOLDER_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.functionaltests.Constants.FOLDER_TYPE;
import static org.nuxeo.functionaltests.Constants.NOTE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

/**
 * Verify document deletion
 *
 * @since 8.2
 */
public class ITVerifyDeleteDocumentContentTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        String parentId = RestHelper.createDocument(TEST_WORKSPACE_PATH, FOLDER_TYPE, TEST_FOLDER_TITLE,
                "Test folder description");
        RestHelper.createDocument(parentId, NOTE_TYPE, "note to restore 1", "This note will be restored.");
        RestHelper.createDocument(parentId, NOTE_TYPE, "note to restore 2", "This note will also be restored.");
        RestHelper.createDocument(parentId, NOTE_TYPE, "note to purge", "This note will be purged.");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void verifyDeleteDocumentContent() throws UserNotConnectedException {
        try {
            // delete folder with notes inside
            login();
            open(TEST_WORKSPACE_URL);
            DocumentBasePage filePage = asPage(DocumentBasePage.class).getContentTab()
                                                                      .removeDocument(TEST_FOLDER_TITLE);
            assertEquals(0, filePage.getContentTab().getChildDocumentRows().size());
            logout();

            login();
            open(TEST_WORKSPACE_URL);

            // go to the trash tab and check that now New button is present
            TrashSubPage trashPage = asPage(DocumentBasePage.class).getManageTab().getTrashSubTab();
            assertTrue(trashPage.hasDocumentLink(TEST_FOLDER_TITLE));
            assertEquals(0, driver.findElements(By.xpath("//form[@id='nxw_newDocument_form']")).size());

            // go to folder and check contents
            trashPage = trashPage.goToDocument(TEST_FOLDER_TITLE);
            List<WebElement> children = trashPage.getChildDocumentRows();
            assertEquals(3, children.size());
            assertEquals("note to purge", children.get(0).findElement(By.xpath("td[3]")).getText());
            assertEquals("note to restore 1", children.get(1).findElement(By.xpath("td[3]")).getText());
            assertEquals("note to restore 2", children.get(2).findElement(By.xpath("td[3]")).getText());

            // test restore and delete buttons and restore note
            trashPage.selectByTitle("note to restore 1"); // tick and check buttons
            assertEquals(1, driver.findElements(
                By.xpath("//form[@id='document_trash_content_buttons:nxw_CURRENT_SELECTION_DELETE_form']"))
                .size());
            assertEquals(1, driver.findElements(
                By.xpath("//form[@id='document_trash_content_buttons:nxw_CURRENT_SELECTION_UNDELETE_form']"))
                .size());
            trashPage.selectByTitle("note to restore 1"); // untick
            filePage = trashPage.restoreDocument("note to restore 1");

            // test restored note
            children = filePage.getContentTab().getChildDocumentRows();
            assertEquals(1, children.size());
            assertEquals("note to restore 1", children.get(0).findElement(By.xpath("td[3]")).getText());

            // go to trash tab and purge one note and restore the other
            trashPage = filePage.getManageTab().getTrashSubTab();
            children = trashPage.getChildDocumentRows();
            assertEquals(2, children.size());
            assertEquals("note to purge", children.get(0).findElement(By.xpath("td[3]")).getText());
            assertEquals("note to restore 2", children.get(1).findElement(By.xpath("td[3]")).getText());
            trashPage = trashPage.purgeDocument("note to purge");
            filePage = trashPage.restoreDocument("note to restore 2");

            // test second restore
            children = filePage.getContentTab().getChildDocumentRows();
            assertEquals(2, children.size());
            assertEquals("note to restore 1", children.get(0).findElement(By.xpath("td[3]")).getText());
            assertEquals("note to restore 2", children.get(1).findElement(By.xpath("td[3]")).getText());

            // test purge
            trashPage = filePage.getManageTab().getTrashSubTab();
            children = trashPage.getChildDocumentRows();
            assertEquals(0, children.size());
        } finally {
            logout();
        }
    }
}
