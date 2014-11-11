/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * This test checks the filter feature.
 *
 * @since 5.7.2
 */
public class ITSelectAndFilterTest extends AbstractTest {

    protected final String CHECK_BOX_XPATH = "td/input[@type=\"checkbox\"]";

    protected final String DOCUMENT_TITLE_XPATH = "td//span[@id[starts-with(.,\"title_\")]]";

    protected final String RESET_FILTER_XPATH = "cv_document_content_0_resetFilterForm:resetFilter";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    /**
     * This tests create 2 documents in a workspace, select one of them, filter
     * the other one, clear the filter and make sure the originally selected
     * document is still selected.
     *
     * @throws Exception
     * @since 5.7.2
     */
    @Test
    public void testSelectAndFilter() throws Exception {

        DocumentBasePage defaultDomain = login();

        DocumentBasePage workspacePage = createWorkspace(defaultDomain,
                WORKSPACE_TITLE, null);

        // Create test File 1
        DocumentBasePage newFile = createFile(workspacePage,
                Boolean.toString(true), null, false, null, null, null);
        workspacePage = newFile.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);

        // Create test File 2
        newFile = createFile(workspacePage, Boolean.toString(false), null,
                false, null, null, null);
        workspacePage = newFile.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);

        ContentTabSubPage contentTabSubPage = workspacePage.getContentTab();

        List<WebElement> trelements = contentTabSubPage.getChildDocumentRows();

        // We must have 2 files
        assertTrue(trelements != null);
        assertEquals(2, trelements.size());

        // Select the first document
        trelements.get(0).findElement(By.xpath(CHECK_BOX_XPATH)).click();
        boolean selectedFileName = Boolean.parseBoolean(trelements.get(0).findElement(
                By.xpath(DOCUMENT_TITLE_XPATH)).getText());

        // Filter on the name of the other document
        contentTabSubPage.filterDocument(Boolean.toString(!selectedFileName),
                1, AJAX_TIMEOUT_SECONDS * 1000);
        Locator.waitUntilElementPresent(By.id(RESET_FILTER_XPATH));

        trelements = contentTabSubPage.getChildDocumentRows();

        // We must have only 1 file
        assertTrue(trelements != null);
        assertEquals(1, trelements.size());

        // Reset filter
        workspacePage.getContentTab().clearFilter(2,
                AJAX_TIMEOUT_SECONDS * 1000);
        Locator.waitUntilElementNotPresent(By.id(RESET_FILTER_XPATH));

        trelements = contentTabSubPage.getChildDocumentRows();

        // We must have 2 files
        assertTrue(trelements != null);
        assertEquals(2, trelements.size());

        // Now check that the selection is the same than before filtering
        boolean isSelectionOk = true;
        for (WebElement tr : trelements) {
            final boolean isCurrentTrSelected = tr.findElement(
                    By.xpath(CHECK_BOX_XPATH)).isSelected();
            String s = tr.findElement(By.xpath(DOCUMENT_TITLE_XPATH)).getText();
            final boolean isPreviouslySelectedDoc = (Boolean.parseBoolean(s) == selectedFileName);
            if (isCurrentTrSelected ^ isPreviouslySelectedDoc) {
                isSelectionOk = false;
                break;
            }
        }
        assertTrue(isSelectionOk);

        deleteWorkspace(workspacePage, WORKSPACE_TITLE);

        logout();
    }

}
