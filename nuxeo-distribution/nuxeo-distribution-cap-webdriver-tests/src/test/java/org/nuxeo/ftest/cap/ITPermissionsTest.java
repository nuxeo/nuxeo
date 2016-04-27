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
import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_URL;
import static org.nuxeo.ftest.cap.TestConstants.TEST_SECTION_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_SECTION_URL;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.SECTIONS_PATH;
import static org.nuxeo.functionaltests.Constants.SECTION_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_URL;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PermissionsSubPage;
import org.nuxeo.functionaltests.pages.tabs.SectionContentTabSubPage;
import org.openqa.selenium.NoSuchElementException;

/**
 * @since 8.3
 */
public class ITPermissionsTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createUser("bree", "bree1", null, null, null, null, "members");
        RestHelper.createUser("linnet", "linnet1", null, null, null, null, "members");
        RestHelper.createUser("susan", "susan1", null, null, null, null, "members");
        RestHelper.createGroup("housewives", "Housewives", new String[] { "linnet", "susan" }, null);
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.addPermission(WORKSPACES_PATH, TEST_USERNAME, "Everything");
        RestHelper.createDocument(SECTIONS_PATH, SECTION_TYPE, TEST_SECTION_TITLE, null);
        RestHelper.addPermission(SECTIONS_PATH, TEST_USERNAME, "Everything");
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
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
    public void testWorkspacePermissions() throws DocumentBasePage.UserNotConnectedException {
        testPermissionsOn(TEST_WORKSPACE_URL, false, false);
    }

    @Test
    public void testSectionPermissions() throws DocumentBasePage.UserNotConnectedException {
        testPermissionsOn(TEST_SECTION_URL, true, false);
    }

    @Test
    public void testDocumentPermissions() throws DocumentBasePage.UserNotConnectedException {
        testPermissionsOn(TEST_FILE_URL, false, true);
    }

    private void testPermissionsOn(String docUrl, boolean isSection, boolean isFile)
            throws DocumentBasePage.UserNotConnectedException {
        // check bree has only read permission
        login("bree", "bree1");
        open(docUrl);

        DocumentBasePage page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // check linnet has only read permission
        login("linnet", "linnet1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // grant manage everything to bree and housewives group as admin
        login();
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        PermissionsSubPage permissionsTab = page.getPermissionsTab();
        PermissionsSubPage permissionsSubPage = permissionsTab.grantPermission("Manage everything", "bree");
        permissionsSubPage = permissionsTab.grantPermission("Manage everything", "housewives");
        assertTrue(permissionsSubPage.hasPermission("Manage everything", "bree"));
        assertTrue(permissionsSubPage.hasPermission("Manage everything", "housewives"));

        // check result for bree
        login("bree", "bree1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertTrue(hasFilesTab(page));
            assertTrue(hasNewRelationLink(page));
        } else {
            assertTrue(hasNewButton(page, isSection));
        }
        assertTrue(hasEditTab(page));
        assertTrue(hasNewPermissionsButton(page));
        assertTrue(hasManageTab(page));

        // check result for linnet
        login("linnet", "linnet1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertTrue(hasFilesTab(page));
            assertTrue(hasNewRelationLink(page));
        } else {
            assertTrue(hasNewButton(page, isSection));
        }
        assertTrue(hasEditTab(page));
        assertTrue(hasNewPermissionsButton(page));
        assertTrue(hasManageTab(page));

        // revoke manage everything to bree and housewives as admin
        login();
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        permissionsTab = page.getPermissionsTab();

        permissionsSubPage = permissionsTab.deletePermission("Manage everything", "bree");
        permissionsSubPage = permissionsTab.deletePermission("Manage everything", "housewives");
        assertFalse(permissionsSubPage.hasPermission("Manage everything", "bree"));
        assertFalse(permissionsSubPage.hasPermission("Manage everything", "housewives"));

        // check result for bree
        login("bree", "bree1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // check result for linnet
        login("linnet", "linnet1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // check susan has only read premission
        login("susan", "susan1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // grant edit to bree and housewives as manager
        login(TEST_USERNAME, TEST_PASSWORD);
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        permissionsTab = page.getPermissionsTab();
        permissionsSubPage = permissionsTab.grantPermission("Edit", "bree");
        permissionsSubPage = permissionsTab.grantPermission("Edit", "housewives");
        assertTrue(permissionsSubPage.hasPermission("Edit", "bree"));
        assertTrue(permissionsSubPage.hasPermission("Edit", "housewives"));

        // check result for bree
        login("bree", "bree1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertTrue(hasFilesTab(page));
            assertTrue(hasNewRelationLink(page));
        } else {
            assertTrue(hasNewButton(page, isSection));
        }
        assertTrue(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // check result for susan
        login("susan", "susan1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertTrue(hasFilesTab(page));
            assertTrue(hasNewRelationLink(page));
        } else {
            assertTrue(hasNewButton(page, isSection));
        }
        assertTrue(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // revoke edit to bree and housewives as manager
        login(TEST_USERNAME, TEST_PASSWORD);
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        permissionsTab = page.getPermissionsTab();

        permissionsSubPage = permissionsTab.deletePermission("Edit", "bree");
        permissionsSubPage = permissionsTab.deletePermission("Edit", "housewives");
        assertFalse(permissionsSubPage.hasPermission("Edit", "bree"));
        assertFalse(permissionsSubPage.hasPermission("Edit", "housewives"));

        // check result for bree
        login("bree", "bree1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));

        // check result for susan
        login("susan", "susan1");
        open(docUrl);

        page = asPage(DocumentBasePage.class);
        if (isFile) {
            assertFalse(hasFilesTab(page));
            assertFalse(hasNewRelationLink(page));
        } else {
            assertFalse(hasNewButton(page, isSection));
        }
        assertFalse(hasEditTab(page));
        assertFalse(hasNewPermissionsButton(page));
        assertFalse(hasManageTab(page));
    }

    private boolean hasNewRelationLink(DocumentBasePage page) {
        return page.getRelationTab().hasNewRelationLink();
    }

    private boolean hasFilesTab(DocumentBasePage page) {
        try {
            return page.getFilesTab() != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean hasNewButton(DocumentBasePage page, boolean isSection) {
        if (isSection) {
            return page.getContentTab(SectionContentTabSubPage.class).hasNewButton();
        }
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
