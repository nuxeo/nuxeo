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
 *     Thomas Roger
 *
 */

package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_URL;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_URL;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

/**
 * @since 8.2
 */
public class ITExportTest extends AbstractTest {

    private static final String XML_EXPORT_LINK_TEXT = "XML Export";

    private static final String ZIP_TREE_XML_EXPORT_LINK_TEXT = "ZIP Tree XML Export";

    private static final String ZIP_XML_EXPORT_LINK_TEXT = "ZIP XML Export";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testFolderishExports() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        openExportPopup(false);
        assertTrue(hasExportLink(XML_EXPORT_LINK_TEXT));
        assertTrue(hasExportLink(ZIP_TREE_XML_EXPORT_LINK_TEXT));
        assertFalse(hasExportLink(ZIP_XML_EXPORT_LINK_TEXT));
    }

    @Test
    public void testDocumentExports() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_FILE_URL);

        openExportPopup(true);
        assertTrue(hasExportLink(XML_EXPORT_LINK_TEXT));
        assertTrue(hasExportLink(ZIP_XML_EXPORT_LINK_TEXT));
        assertFalse(hasExportLink(ZIP_TREE_XML_EXPORT_LINK_TEXT));
    }

    private void openExportPopup(boolean openMoreMenu) {
        DocumentBasePage page = asPage(DocumentBasePage.class);
        ContextualActions contextualActions = page.getContextualActions();
        if (openMoreMenu) {
            contextualActions = contextualActions.openMore();
        }
        contextualActions.clickOnButton(contextualActions.exportButton);
        waitForExportPopup();
    }

    private boolean hasExportLink(String link) {
        try {
            WebElement element = driver.findElement(By.linkText(link));
            return element != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private void waitForExportPopup() {
        Locator.waitUntilGivenFunction(input -> {
            try {
                driver.findElement(By.xpath("//h3[text()='Export']"));
            } catch (NoSuchElementException e) {
                return false;
            }
            return true;
        });
    }

}
