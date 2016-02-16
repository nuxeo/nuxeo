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
 *     Guillaume Renard
 */
package org.nuxeo.ftest.cap;

import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test checks the filter feature.
 *
 * @since 5.7.2
 */
public class ITSelectAndFilterTest extends AbstractTest {

    protected final String CHECK_BOX_XPATH = "td/input[@type=\"checkbox\"]";

    protected final String DOCUMENT_TITLE_XPATH = "td//span[@id[starts-with(.,\"title_\")]]";

    protected final String RESET_FILTER_XPATH = "cv_document_content_0_resetFilterForm:resetFilter";

    private final static String WORKSPACE_TITLE = ITSelectAndFilterTest.class.getSimpleName() + "_WorkspaceTitle_" + new Date().getTime();

    private static String wsId;

    @Before
    public void before() {
        wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_TITLE, null);
        RestHelper.createDocument(wsId, FILE_TYPE, Boolean.toString(true), null);
        RestHelper.createDocument(wsId, FILE_TYPE, Boolean.toString(false), null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
        wsId = null;
    }

    /**
     * This tests create 2 documents in a workspace, select one of them, filter the other one, clear the filter and make
     * sure the originally selected document is still selected.
     *
     * @throws Exception
     * @since 5.7.2
     */
    @Test
    public void testSelectAndFilter() throws Exception {
        login();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        DocumentBasePage workspacePage = asPage(DocumentBasePage.class);
        ContentTabSubPage contentTabSubPage = workspacePage.getContentTab();

        List<WebElement> trelements = contentTabSubPage.getChildDocumentRows();

        // We must have 2 files
        assertTrue(trelements != null);
        assertEquals(2, trelements.size());

        // Select the first document
        trelements.get(0).findElement(By.xpath(CHECK_BOX_XPATH)).click();
        boolean selectedFileName = Boolean.parseBoolean(
                trelements.get(0).findElement(By.xpath(DOCUMENT_TITLE_XPATH)).getText());

        // Filter on the name of the other document
        contentTabSubPage.filterDocument(Boolean.toString(!selectedFileName), 1, AJAX_TIMEOUT_SECONDS * 1000);
        Locator.waitUntilElementPresent(By.id(RESET_FILTER_XPATH));

        trelements = contentTabSubPage.getChildDocumentRows();

        // We must have only 1 file
        assertTrue(trelements != null);
        assertEquals(1, trelements.size());

        // Reset filter
        workspacePage.getContentTab().clearFilter(2, AJAX_TIMEOUT_SECONDS * 1000);
        Locator.waitUntilElementNotPresent(By.id(RESET_FILTER_XPATH));

        trelements = contentTabSubPage.getChildDocumentRows();

        // We must have 2 files
        assertTrue(trelements != null);
        assertEquals(2, trelements.size());

        // Now check that the selection is the same than before filtering
        boolean isSelectionOk = true;
        for (WebElement tr : trelements) {
            final boolean isCurrentTrSelected = tr.findElement(By.xpath(CHECK_BOX_XPATH)).isSelected();
            String s = tr.findElement(By.xpath(DOCUMENT_TITLE_XPATH)).getText();
            final boolean isPreviouslySelectedDoc = (Boolean.parseBoolean(s) == selectedFileName);
            if (isCurrentTrSelected ^ isPreviouslySelectedDoc) {
                isSelectionOk = false;
                break;
            }
        }
        assertTrue(isSelectionOk);

        logout();
    }

}
