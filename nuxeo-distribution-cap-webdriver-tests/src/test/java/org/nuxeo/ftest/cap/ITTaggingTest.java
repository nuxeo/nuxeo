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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.openqa.selenium.By;

/**
 * @since 5.9.6
 */
public class ITTaggingTest extends AbstractTest {

    public final static String TEST_FILE_NAME = "test1";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    public final static String SELECT2_TAG_ELT_ID = "s2id_nxl_grid_summary_layout:nxw_summary_current_document_tagging_form:nxw_summary_current_document_tagging_select2";

    @Test
    public void testAddAndRemoveTagOnDocument()
            throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        // Create test File
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        FileDocumentBasePage fileDocumentBasePage = createFile(workspacePage,
                TEST_FILE_NAME, "Test File description", false, null, null,
                null);

        Select2WidgetElement tagWidget = new Select2WidgetElement(
                driver,
                Locator.findElementWithTimeout(By.id(SELECT2_TAG_ELT_ID)),
                true);
        assertTrue(tagWidget.getSelectedValues().isEmpty());
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        tagWidget.selectValue("first_tag");
        a.waitForAjaxRequests();
        assertEquals(1, tagWidget.getSelectedValues().size());
        a.watchAjaxRequests();
        tagWidget.selectValue("second_tag");
        a.waitForAjaxRequests();
        assertEquals(2, tagWidget.getSelectedValues().size());
        fileDocumentBasePage.getEditTab();
        fileDocumentBasePage.getSummaryTab();
        tagWidget = new Select2WidgetElement(
                driver,
                Locator.findElementWithTimeout(By.id(SELECT2_TAG_ELT_ID)),
                true);
        assertEquals(2, tagWidget.getSelectedValues().size());
        a.watchAjaxRequests();
        tagWidget.removeFromSelection("first_tag");
        a.waitForAjaxRequests();
        assertEquals(1, tagWidget.getSelectedValues().size());
        fileDocumentBasePage.getEditTab();
        fileDocumentBasePage.getSummaryTab();
        tagWidget = new Select2WidgetElement(
                driver,
                Locator.findElementWithTimeout(By.id(SELECT2_TAG_ELT_ID)),
                true);
        assertEquals(1, tagWidget.getSelectedValues().size());
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        DocumentBasePage documentBasePage = login();
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

}
