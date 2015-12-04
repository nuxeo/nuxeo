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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.TrashSubPage;
import org.openqa.selenium.WebElement;

/**
 * Tests for the manage tab.
 *
 * @since 7.1
 */
public class ITManageTest extends AbstractTest {

    protected final static String TEST_FILE_TITLE = "Test File description";

    protected final static String WORKSPACE_TITLE = "workspace";

    /**
     * Test trash management.
     *
     * @since 7.1
     */
    @Test
    public void testTrashPurge() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        // Create a file under workspace.
        DocumentBasePage workspacePage = createWorkspace(documentBasePage, WORKSPACE_TITLE, null);
        DocumentBasePage filePage = createFile(workspacePage, TEST_FILE_TITLE, null, false, null, null, null);

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
        assertEquals(docs.size(), 5);
        // Select all files and remove them.
        content.removeAllDocuments().getChildDocumentRows();
        docs = content.getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 0);

        // Go to trash page.
        TrashSubPage trashSubPage = asPage(DocumentBasePage.class).getManageTab().getTrashSubTab();
        docs = trashSubPage.getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 5);

        // Empty the trash.
        docs = trashSubPage.emptyTrash().getChildDocumentRows();
        assertNotNull(docs);
        assertEquals(docs.size(), 0);
    }

}