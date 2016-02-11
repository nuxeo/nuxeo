/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_URL;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ArchivedVersionsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;

/**
 * Tests the Archived versions screen.
 */
public class ITArchivedVersionsTest extends AbstractTest {

    @Before
    public void before() throws DocumentBasePage.UserNotConnectedException {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, "Test File description");

        login();
        open(TEST_FILE_URL);
        FileDocumentBasePage filePage = asPage(FileDocumentBasePage.class);
        // Create version 1.0 of the File
        filePage.getEditTab().edit("Test file: modif 1", null, EditTabSubPage.MAJOR_VERSION_INCREMENT_VALUE);

        // Create version 2.0 of the File
        filePage.getEditTab().edit("Test file: modif 2", null, EditTabSubPage.MAJOR_VERSION_INCREMENT_VALUE);

        logout();
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testArchivedVersions() throws Exception {
        login();
        open(TEST_FILE_URL);

        // Do the tests
        DocumentBasePage filePage = asPage(DocumentBasePage.class);
        filePage = testViewVersions(filePage);
        filePage = testRestoreVersion(filePage);
        filePage = testDeleteVersions(filePage);

        // Logout
        logout();
    }

    /**
     * Tests view versions.
     *
     * @param docPage the current doc page
     * @return the current doc page
     */
    protected DocumentBasePage testViewVersions(DocumentBasePage docPage) {

        // Go to archived versions sub tab
        ArchivedVersionsSubPage archivedVersionsPage = docPage.getHistoryTab().getArchivedVersionsSubTab();

        // Check version labels
        List<String> versionLabels = archivedVersionsPage.getVersionLabels();
        List<String> expectedVersionLabels = new ArrayList<String>();
        expectedVersionLabels.add("1.0");
        expectedVersionLabels.add("2.0");
        assertEquals(expectedVersionLabels, versionLabels);

        // View version 1.0 and check its title
        DocumentBasePage versionPage = archivedVersionsPage.viewVersion("1.0");
        versionPage.checkDocTitle("Test file: modif 1 (Version 1.0)");

        // Go back to doc
        docPage = versionPage.goToDocumentByBreadcrumb("Test file: modif 2");

        // Go to archived versions sub tab
        archivedVersionsPage = docPage.getHistoryTab().getArchivedVersionsSubTab();

        // View version 2.0 and check its title
        versionPage = archivedVersionsPage.viewVersion("2.0");
        versionPage.checkDocTitle("Test file: modif 2 (Version 2.0)");

        // Go back to doc and return it
        return versionPage.goToDocumentByBreadcrumb("Test file: modif 2");
    }

    /**
     * Tests restore version.
     *
     * @param docPage the current doc page
     * @return the current doc page
     */
    protected DocumentBasePage testRestoreVersion(DocumentBasePage docPage) {

        // Go to archived versions sub tab
        ArchivedVersionsSubPage archivedVersionsPage = docPage.getHistoryTab().getArchivedVersionsSubTab();

        // Restore version 1.0 and check its title
        DocumentBasePage restoredVersionPage = archivedVersionsPage.restoreVersion("1.0");
        restoredVersionPage.checkDocTitle("Test file: modif 1");

        // Return doc
        return restoredVersionPage;
    }

    /**
     * Tests delete versions.
     *
     * @param docPage the current doc page
     * @return the current doc page
     */
    protected DocumentBasePage testDeleteVersions(DocumentBasePage docPage) {

        // Go to archived versions sub tab
        ArchivedVersionsSubPage archivedVersionsPage = docPage.getHistoryTab().getArchivedVersionsSubTab();

        // Check cannot delete versions since none is selected
        archivedVersionsPage.checkCanRemoveSelectedVersions(false);

        // Select version 1.0
        archivedVersionsPage = archivedVersionsPage.selectVersion("1.0");

        // Check cannot delete version 1.0 since it's the base version for the restored document
        archivedVersionsPage.checkCanRemoveSelectedVersions(false);

        // Deselect version 1.0
        archivedVersionsPage = archivedVersionsPage.selectVersion("1.0");

        // Select version 2.0
        archivedVersionsPage = archivedVersionsPage.selectVersion("2.0");

        // Check can delete version 2.0
        archivedVersionsPage.checkCanRemoveSelectedVersions(true);

        // Delete selected version
        archivedVersionsPage = archivedVersionsPage.removeSelectedVersions();

        // Check version labels, there should be one left
        List<String> versionLabels = archivedVersionsPage.getVersionLabels();
        assertEquals(Collections.singletonList("1.0"), versionLabels);

        // Go back to doc and return it
        return archivedVersionsPage.goToDocumentByBreadcrumb("Test file: modif 1");
    }
}
