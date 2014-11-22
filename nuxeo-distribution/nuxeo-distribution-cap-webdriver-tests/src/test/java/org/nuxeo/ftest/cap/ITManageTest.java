/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ftest.cap;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage
        .UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.TrashSubPage;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for the manage tab.
 *
 * @since 7.1
 */
public class ITManageTest extends AbstractTest {

    protected final static String TEST_FILE_NAME = "test1";

    protected final static String WORKSPACE_TITLE = "workspace";

    /**
     * Test trash management.
     *
     * @since 7.1
     */
    @Test
    public void testTrashPurge() throws UserNotConnectedException,
            IOException {
        DocumentBasePage documentBasePage = login();

        // Create five files under workspace.
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);
        workspacePage.getHeaderLinks().getNavigationSubPage().goToDocument(
                "workspace");
        createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);
        workspacePage.getHeaderLinks().getNavigationSubPage().goToDocument(
                "workspace");
        createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);
        workspacePage.getHeaderLinks().getNavigationSubPage().goToDocument(
                "workspace");
        createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);
        workspacePage.getHeaderLinks().getNavigationSubPage().goToDocument(
                "workspace");
        createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);
        workspacePage.getHeaderLinks().getNavigationSubPage().goToDocument(
                "workspace");
        List<WebElement> docs = workspacePage.getContentTab()
                .getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 5);
        // Select every files and remove them.
        workspacePage.getContentTab().removeAllDocuments();
        docs = workspacePage.getContentTab().getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 0);
        // Go to trash page.
        TrashSubPage trashSubPage = workspacePage.getManageTab()
                .getTrashSubTab();
        docs = trashSubPage.getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 5);
        // Empty the trash.
        trashSubPage.emptyTrash();
        docs = trashSubPage.getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 0);
    }

}
