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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_URL;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_URL;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PermissionsSubPage;
import org.openqa.selenium.NoSuchElementException;

/**
 * @since 8.2
 */
public class ITPermissionsTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createUser("bree", "bree1", null, null, null, null, "members");
        RestHelper.createUser("linnet", "linnet1", null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.addPermission(WORKSPACES_PATH, TEST_USERNAME, "Everything");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testBlockAndUnblockPermissions() throws DocumentBasePage.UserNotConnectedException {
        login("bree", "bree1");
        open(WORKSPACES_URL);

        DocumentBasePage page = asPage(DocumentBasePage.class);
        ContentTabSubPage contentTab = page.getContentTab();
        assertTrue(contentTab.hasDocumentLink(TEST_WORKSPACE_TITLE));

        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        PermissionsSubPage permissionsTab = page.getPermissionsTab();
        assertTrue(permissionsTab.hasPermissionForUser("Read", "members"));
        permissionsTab = permissionsTab.blockPermissions();
        assertTrue(permissionsTab.hasPermissionForUser("Manage everything", TEST_USERNAME));
        assertTrue(permissionsTab.hasPermissionForUser("Manage everything", "administrators"));
        assertFalse(permissionsTab.hasPermissionForUser("Read", "members"));

        login("bree", "bree1");
        open(WORKSPACES_URL);

        page = asPage(DocumentBasePage.class);
        contentTab = page.getContentTab();
        assertFalse(contentTab.hasDocumentLink(TEST_WORKSPACE_TITLE));

        login("linnet", "linnet1");
        open(WORKSPACES_URL);

        page = asPage(DocumentBasePage.class);
        contentTab = page.getContentTab();
        assertFalse(contentTab.hasDocumentLink(TEST_WORKSPACE_TITLE));

        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        permissionsTab = page.getPermissionsTab();
        assertFalse(permissionsTab.hasPermissionForUser("Read", "members"));
        permissionsTab = permissionsTab.unblockPermissions();
        assertTrue(permissionsTab.hasPermissionForUser("Read", "members"));

        login("bree", "bree1");
        open(WORKSPACES_URL);

        page = asPage(DocumentBasePage.class);
        contentTab = page.getContentTab();
        assertTrue(contentTab.hasDocumentLink(TEST_WORKSPACE_TITLE));

        login("linnet", "linnet1");
        open(WORKSPACES_URL);

        page = asPage(DocumentBasePage.class);
        contentTab = page.getContentTab();
        assertTrue(contentTab.hasDocumentLink(TEST_WORKSPACE_TITLE));
    }

    @Test
    public void testLocalPermissions() throws DocumentBasePage.UserNotConnectedException {
        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        DocumentBasePage page = asPage(DocumentBasePage.class);
        assertFalse(hasEditTab(page));

        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        PermissionsSubPage permissionsTab = page.getPermissionsTab();
        PermissionsSubPage permissionsSubPage = permissionsTab.grantPermissionForUser("Edit", "bree");
        assertTrue(permissionsSubPage.hasPermissionForUser("Edit", "bree"));

        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        assertTrue(hasEditTab(page));

        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        permissionsTab = page.getPermissionsTab();
        permissionsTab.deletePermission("Edit", "bree");

        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        assertFalse(hasEditTab(page));
    }

    private boolean hasEditTab(DocumentBasePage page) {
        try {
            return page.getEditTab() != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
