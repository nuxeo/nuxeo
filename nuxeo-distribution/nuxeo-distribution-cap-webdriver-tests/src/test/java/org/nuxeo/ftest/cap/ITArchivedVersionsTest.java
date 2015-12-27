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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ArchivedVersionsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;

/**
 * Tests the Archived versions screen.
 */
public class ITArchivedVersionsTest extends AbstractTest {
    @Test
    public void testArchivedVersions() throws Exception {

        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a File and its archived versions
        DocumentBasePage filePage = initRepository(defaultDomainPage);

        // Do the tests
        filePage = testViewVersions(filePage);
        filePage = testRestoreVersion(filePage);
        filePage = testDeleteVersions(filePage);

        // Clean up repository
        cleanRepository(filePage);

        // Logout
        logout();
    }

    /**
     * Inits the repository with a File document and makes two versions of it.
     *
     * @param currentPage the current page
     * @return the created File document page
     * @throws Exception if initializing repository fails
     */
    @Override
    protected DocumentBasePage initRepository(DocumentBasePage currentPage) throws Exception {

        // Create test Workspace
        DocumentBasePage workspacePage = super.initRepository(currentPage);

        // Create test File
        FileDocumentBasePage filePage = createFile(workspacePage, "Test file", "Test File description", false, null,
                null, null);

        // Create version 1.0 of the File
        filePage.getEditTab().edit("Test file: modif 1", null, EditTabSubPage.MAJOR_VERSION_INCREMENT_VALUE);

        // Create version 2.0 of the File
        filePage.getEditTab().edit("Test file: modif 2", null, EditTabSubPage.MAJOR_VERSION_INCREMENT_VALUE);

        return filePage;
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
        assertEquals(Arrays.asList("1.0"), versionLabels);

        // Go back to doc and return it
        return archivedVersionsPage.goToDocumentByBreadcrumb("Test file: modif 1");
    }
}
