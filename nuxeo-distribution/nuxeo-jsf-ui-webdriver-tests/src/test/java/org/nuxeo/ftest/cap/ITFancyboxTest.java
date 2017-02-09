/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.DateWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;

import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertTrue;

/**
 * Tests behaviours inside fancyboxes.
 *
 * @since 9.1
 */
public class ITFancyboxTest extends AbstractTest {

    public static final String TEST_FILE_NAME = "test fancy";

    public static final String BULK_EDIT_ACTION_TITLE = "Edit";

    protected static final String FANCYFORM_ID = "document_content_buttons:nxw_CURRENT_SELECTION_EDIT_after_view_fancy_subview:nxw_CURRENT_SELECTION_EDIT_after_view_fancyform";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.addPermission(TEST_WORKSPACE_PATH, TEST_USERNAME, SecurityConstants.EVERYTHING);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_NAME, "Test File description");
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        RestHelper.cleanup();
    }

    /**
     * Non regression test for NXP-19494, where the fancybox is not reopened correctly after a validation error.
     */
    @Test
    public void testEditInvalidDateInFancybox() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        ContentTabSubPage content = asPage(ContentTabSubPage.class);
        content.selectByTitle(TEST_FILE_NAME);
        Locator.waitUntilEnabledAndClick(
                content.getContentViewElement().getSelectionActionByTitle(BULK_EDIT_ACTION_TITLE));

        // make sure fancybox is open
        AbstractPage.getFancyBoxContent();

        // set an invalid date and submit
        DateWidgetElement w = getFancyDate();
        w.setInputValue("foo");
        updateFancy();

        // check that fancybox reopened and that error is displayed
        AbstractPage.getFancyBoxContent();
        w = getFancyDate();
        String message = w.getMessageValue();
        assertTrue(message != null && message.contains("'foo' could not be understood as a date."));

        // submit again
        updateFancy();

        // same check again
        AbstractPage.getFancyBoxContent();
        w = getFancyDate();
        message = w.getMessageValue();
        assertTrue(message != null && message.contains("'foo' could not be understood as a date."));

        logout();
    }

    protected DateWidgetElement getFancyDate() {
        LayoutElement layout = new LayoutElement(driver, FANCYFORM_ID + ":nxl_bulkEdit_edit");
        return layout.getWidget("nxw_expired", DateWidgetElement.class);
    }

    protected void updateFancy() {
        Locator.findElementWaitUntilEnabledAndClick(By.id(FANCYFORM_ID + ":update"));
    }

}
