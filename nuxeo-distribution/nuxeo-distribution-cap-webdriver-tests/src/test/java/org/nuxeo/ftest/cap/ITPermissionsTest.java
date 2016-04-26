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
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PermissionsSubPage;
import org.openqa.selenium.NoSuchElementException;

import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;

import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_URL;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 8.3
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

        login("linnet", "linnet1");
        open(WORKSPACES_URL);

        page = asPage(DocumentBasePage.class);
        contentTab = page.getContentTab();
        assertTrue(contentTab.hasDocumentLink(TEST_WORKSPACE_TITLE));

        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        PermissionsSubPage permissionsTab = page.getPermissionsTab();
        assertTrue(permissionsTab.hasPermission("Read", "members"));
        permissionsTab = permissionsTab.blockPermissions();
        assertTrue(permissionsTab.hasPermission("Manage everything", TEST_USERNAME));
        assertTrue(permissionsTab.hasPermission("Manage everything", "administrators"));
        assertFalse(permissionsTab.hasPermission("Read", "members"));

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
        assertFalse(permissionsTab.hasPermission("Read", "members"));
        permissionsTab = permissionsTab.unblockPermissions();
        assertTrue(permissionsTab.hasPermission("Read", "members"));

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
    public void testWorkspacePermissionsByAdmin() throws DocumentBasePage.UserNotConnectedException {
        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        // check bree has only read permission
        DocumentBasePage page = asPage(DocumentBasePage.class);
        assertFalse(hasNewButton(page));
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        login();
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        PermissionsSubPage permissionsTab = page.getPermissionsTab();

        // grant manage everything to bree
        PermissionsSubPage permissionsSubPage = permissionsTab.grantPermission("Manage everything", "bree");
        assertTrue(permissionsSubPage.hasPermission("Manage everything", "bree"));

        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        // check result
        page = asPage(DocumentBasePage.class);
        assertTrue(hasNewButton(page));
        assertTrue(hasEditTab(page));
        assertTrue(hasNewPermissionsButton(page));
        assertTrue(hasManageTab(page));

        login();
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        permissionsTab = page.getPermissionsTab();

        // revoke manage everything to bree
        permissionsSubPage = permissionsTab.deletePermission("Manage everything", "bree");
        assertFalse(permissionsSubPage.hasPermission("Manage everything", "bree"));

        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        // check result
        page = asPage(DocumentBasePage.class);
        assertFalse(hasNewButton(page));
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));
    }

    @Test
    public void testWorkspacePermissionsByManager() throws DocumentBasePage.UserNotConnectedException {
        login("bree", "bree1");
        open(TEST_WORKSPACE_URL);

        DocumentBasePage page = asPage(DocumentBasePage.class);
        assertFalse(hasEditTab(page));

        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_WORKSPACE_URL);

        page = asPage(DocumentBasePage.class);
        PermissionsSubPage permissionsTab = page.getPermissionsTab();
        PermissionsSubPage permissionsSubPage = permissionsTab.grantPermission("Edit", "bree");
        assertTrue(permissionsSubPage.hasPermission("Edit", "bree"));

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

    @Test
    public void testSectionPermissions() throws DocumentBasePage.UserNotConnectedException {

    }

    @Test
    public void testDocumentPermissions() throws DocumentBasePage.UserNotConnectedException {

    }

    private boolean hasNewButton(DocumentBasePage page) {
        return page.getContentTab().hasNewButton();
    }

    private boolean hasEditTab(DocumentBasePage page) {
        try {
            return page.getEditTab() != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean hasNewPermissionsButton(DocumentBasePage page) {
        return page.getPermissionsTab().hasNewPermissionButton();
    }

    private boolean hasManageTab(DocumentBasePage page) {
        try {
            return page.getManageTab() != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
