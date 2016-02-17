/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     guillaume
 */
package org.nuxeo.ftest.cap;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the copy and past feature.
 *
 * @since 5.8
 */
public class ITCopyPasteTest extends AbstractTest {

    private final static String WORKSPACE1_TITLE = ITCopyPasteTest.class.getSimpleName() + "_WorkspaceTitle1_" + new Date().getTime();

    private final static String WORKSPACE2_TITLE = ITCopyPasteTest.class.getSimpleName() + "_WorkspaceTitle2_" + new Date().getTime();

    private final String FILE1_NAME = "testFile1";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE1_TITLE, null);
        RestHelper.createDocument(WORKSPACES_PATH + WORKSPACE1_TITLE, FILE_TYPE, FILE1_NAME, null);
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE2_TITLE, null);
        RestHelper.addPermission(WORKSPACES_PATH + WORKSPACE2_TITLE, TEST_USERNAME, "Everything");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    /**
     * Copy and paste a simple file.
     *
     * @since 5.8
     */
    @Test
    public void testSimpleCopyAndPaste() throws UserNotConnectedException, IOException, ParseException {
        // NXP-18344, to be removed once upgraded to more recent webdriver lib
        doNotRunOnWindowsWithFF26();

        DocumentBasePage documentBasePage;

        // Log as test user and edit the created workspace
        loginAsTestUser();
        open(String.format("/nxpath/default%s@view_documents", WORKSPACES_PATH + WORKSPACE1_TITLE));

        ContentTabSubPage contentTabSubPage = asPage(DocumentBasePage.class).getContentTab();

        contentTabSubPage.copyByTitle(FILE1_NAME);

        documentBasePage = contentTabSubPage.getHeaderLinks().getNavigationSubPage().goToDocument(WORKSPACE2_TITLE);

        contentTabSubPage = documentBasePage.getContentTab();

        contentTabSubPage = contentTabSubPage.paste();

        List<WebElement> docs = contentTabSubPage.getChildDocumentRows();

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertNotNull(docs.get(0).findElement(By.linkText(FILE1_NAME)));
    }
}
