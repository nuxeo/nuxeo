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
 * @since 6.0
 */
public class ITTaggingTest extends AbstractTest {

    public final static String TEST_FILE_NAME = "test1";

    private final static String WORKSPACE_TITLE = ITTaggingTest.class.getSimpleName() + "_WorkspaceTitle_" + new Date().getTime();

    public final static String SELECT2_TAG_ELT_ID = "s2id_nxl_grid_summary_layout:nxw_summary_current_document_tagging_form:nxw_summary_current_document_tagging_select2";

    @Test
    public void testAddAndRemoveTagOnDocument() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        // Create test File
        DocumentBasePage workspacePage = createWorkspace(documentBasePage, WORKSPACE_TITLE, null);
        FileDocumentBasePage fileDocumentBasePage = createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);

        Select2WidgetElement tagWidget = new Select2WidgetElement(driver,
                Locator.findElementWithTimeout(By.id(SELECT2_TAG_ELT_ID)), true);
        assertTrue(tagWidget.getSelectedValues().isEmpty());
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        tagWidget.selectValue("first_tag", false, true);
        a.waitForAjaxRequests();
        assertEquals(1, tagWidget.getSelectedValues().size());
        a.watchAjaxRequests();
        tagWidget.selectValue("second_tag", false, true);
        a.waitForAjaxRequests();
        assertEquals(2, tagWidget.getSelectedValues().size());
        fileDocumentBasePage.getEditTab();
        fileDocumentBasePage.getSummaryTab();
        tagWidget = new Select2WidgetElement(driver, Locator.findElementWithTimeout(By.id(SELECT2_TAG_ELT_ID)), true);
        if (tagWidget.getSelectedValues().size() == 0) {
            // TODO use a server-side API to detect repository capabilities
            // MongoDB does not have tags
            return;
        }
        assertEquals(2, tagWidget.getSelectedValues().size());
        a.watchAjaxRequests();
        tagWidget.removeFromSelection("first_tag");
        a.waitForAjaxRequests();
        assertEquals(1, tagWidget.getSelectedValues().size());
        fileDocumentBasePage.getEditTab();
        fileDocumentBasePage.getSummaryTab();
        tagWidget = new Select2WidgetElement(driver, Locator.findElementWithTimeout(By.id(SELECT2_TAG_ELT_ID)), true);
        assertEquals(1, tagWidget.getSelectedValues().size());
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        DocumentBasePage documentBasePage = login();
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

}
