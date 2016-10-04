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
 *     Yannis JULIENNE
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_TITLE;
import static org.nuxeo.functionaltests.Constants.SECTIONS_TITLE;
import static org.nuxeo.functionaltests.Constants.TEMPLATES_TITLE;
import static org.nuxeo.functionaltests.Constants.DOMAIN_TITLE;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.workspace.WorkspaceHomePage;
import org.nuxeo.functionaltests.pages.workspace.WorkspaceRepositoryPage;

/**
 * Test workspace structure creation.
 *
 * @since 8.3
 */
public class ITDomainTest extends AbstractTest {

    private static final String TEST_NEW_DOMAIN_TITLE = "New Domain";

    private static final String TEST_NEW_WORKSPACE_TITLE = "Test workspace in New Domain";

    private static final String TEST_NEW_FILE_TITLE = "Test file in New Domain";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
        RestHelper.deleteDocument("/" + TEST_NEW_DOMAIN_TITLE);
    }

    @Test
    public void testMultipleDomainsDashboard() throws UserNotConnectedException, IOException {
        login(TEST_USERNAME, TEST_PASSWORD);

        // check dashboard on default domain as member
        UserHomePage userHome = asPage(DocumentBasePage.class).getUserHome();
        assertTrue(userHome.hasUserWorkspace(TEST_WORKSPACE_TITLE));
        assertTrue(userHome.hasDomainDocument(TEST_FILE_TITLE));
        assertFalse(userHome.hasSelectDomainInput());

        // create new domain as admin
        login();
        DocumentBasePage page = asPage(WorkspaceHomePage.class).goToRepository().getDomainCreatePage().createDomain(
                TEST_NEW_DOMAIN_TITLE, null);

        // check result
        page.checkDocTitle(TEST_NEW_DOMAIN_TITLE);
        assertTrue(page.getContentTab().hasDocumentLink(SECTIONS_TITLE));
        assertTrue(page.getContentTab().hasDocumentLink(TEMPLATES_TITLE));
        assertTrue(page.getContentTab().hasDocumentLink(WORKSPACES_TITLE));

        // create new workspace and new file in new domain
        page = page.getContentTab().goToDocument(WORKSPACES_TITLE);
        page = page.createWorkspace(TEST_NEW_WORKSPACE_TITLE, null);
        page.createFile(TEST_NEW_FILE_TITLE, null, false, null, null, null);

        // check result as member
        login(TEST_USERNAME, TEST_PASSWORD);
        page = asPage(WorkspaceRepositoryPage.class);
        assertTrue(page.getContentTab().hasDocumentLink(DOMAIN_TITLE));
        assertTrue(page.getContentTab().hasDocumentLink(TEST_NEW_DOMAIN_TITLE));

        // check result on dashboard
        userHome = page.getUserHome();
        DocumentBasePage.makeBreadcrumbUsable(driver);
        assertTrue(userHome.hasSelectDomainInput());
        assertTrue(userHome.hasUserWorkspace(TEST_WORKSPACE_TITLE));
        assertTrue(userHome.hasDomainDocument(TEST_FILE_TITLE));
        assertFalse(userHome.hasUserWorkspace(TEST_NEW_WORKSPACE_TITLE));
        assertFalse(userHome.hasDomainDocument(TEST_NEW_FILE_TITLE));

        userHome = userHome.selectDomain(TEST_NEW_DOMAIN_TITLE);
        DocumentBasePage.makeBreadcrumbUsable(driver);
        assertTrue(userHome.hasSelectDomainInput());
        assertFalse(userHome.hasUserWorkspace(TEST_WORKSPACE_TITLE));
        assertFalse(userHome.hasDomainDocument(TEST_FILE_TITLE));
        assertTrue(userHome.hasUserWorkspace(TEST_NEW_WORKSPACE_TITLE));
        assertTrue(userHome.hasDomainDocument(TEST_NEW_FILE_TITLE));

        // permanently delete new domain as admin
        login();
        page = asPage(DocumentBasePage.class).getContentTab().removeDocument(TEST_NEW_DOMAIN_TITLE);
        page.getManageTab().getTrashSubTab().emptyTrash();

        logout();

    }

}
