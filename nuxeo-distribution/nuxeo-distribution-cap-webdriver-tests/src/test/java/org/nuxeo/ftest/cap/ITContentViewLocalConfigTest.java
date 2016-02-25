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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.TrashSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * Tests content view local config
 *
 * @since 8.2
 */
public class ITContentViewLocalConfigTest extends AbstractTest {
    @BeforeClass
    public static void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        String parentId = RestHelper.createDocument(TEST_WORKSPACE_PATH, FOLDER_TYPE, TEST_FOLDER_TITLE,
                "Test folder description");
        RestHelper.createDocument(parentId, NOTE_TYPE, "localConfigNote", "Note description");
    }

    @AfterClass
    public static void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testContentViewLocalConfig() throws UserNotConnectedException {
        try {
            login();

            open(TEST_WORKSPACE_URL);

            DocumentBasePage page = asPage(DocumentBasePage.class).getContentTab()
                                                                  .goToDocument(TEST_FOLDER_TITLE)
                                                                  .getContentTab()
                                                                  .removeDocument("localConfigNote");

            assertEquals(0, page.getContentTab().getChildDocumentRows().size());

            TrashSubPage trashPage = page.goToDocumentByBreadcrumb(TEST_WORKSPACE_TITLE)
                                         .getManageTab()
                                         .getLocalConfigSubTabe()
                                         .enableDocumentContentConfig()
                                         .addDocumentContentConfig("Folder", "Trash content")
                                         .getContentTab()
                                         .goToDocument(TEST_FOLDER_TITLE)
                                         .getContentTab(TrashSubPage.class);

            assertTrue(trashPage.hasDocumentLink("localConfigNote"));
            trashPage.restoreDocument("localConfigNote");
            assertFalse(trashPage.hasDocumentLink("localConfigNote"));

            ContentTabSubPage contentTab = asPage(DocumentBasePage.class).goToDocumentByBreadcrumb(TEST_WORKSPACE_TITLE)
                                                                         .getManageTab()
                                                                         .getLocalConfigSubTabe()
                                                                         .disableDocumentContentConfig()
                                                                         .getContentTab()
                                                                         .goToDocument(TEST_FOLDER_TITLE)
                                                                         .getContentTab();

            assertTrue(contentTab.hasDocumentLink("localConfigNote"));
        } finally {
            logout();
        }
    }
}
