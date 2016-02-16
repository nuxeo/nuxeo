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

import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_URL;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 6.0
 */
public class ITTaggingTest extends AbstractTest {

    public final static String TEST_FILE_NAME = "test1";

    public final static String SELECT2_TAG_ELT_ID = "s2id_nxl_grid_summary_layout:nxw_summary_current_document_tagging_form:nxw_summary_current_document_tagging_select2";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testAddAndRemoveTagOnDocument() throws UserNotConnectedException, IOException {
        login();
        open(TEST_FILE_URL);

        DocumentBasePage fileDocumentBasePage = asPage(DocumentBasePage.class);
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
}
