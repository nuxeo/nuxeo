/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ftest.jsf.hotreload.studio;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Assert;
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;

/**
 * Case helper, with all helper methods to share for all tests.
 *
 * @since 9.3
 */
public abstract class NuxeoITCaseHelper extends AbstractTest {

    protected static final String TEST_WS_TITLE = "Studio Test Workspace";

    protected DocumentBasePage getOrCreateTestWorkspace() {
        DocumentBasePage wsPage = getOrCreateTestWorkspace(TEST_WS_TITLE, "Testing Nuxeo Studio doc type");
        return wsPage;
    }

    protected DocumentBasePage getOrCreateTestWorkspace(String title, String description) {
        asPage(DocumentBasePage.class).getNavigationSubPage().goToDocument(Constants.WORKSPACES_TITLE);

        DocumentBasePage wsPage;
        if (Assert.hasElement(By.linkText(title))) {
            // already exists => only navigate to it
            wsPage = asPage(DocumentBasePage.class).getNavigationSubPage().goToDocument(title);
        } else {
            // create it and navigate to it
            WorkspacesContentTabSubPage subPage;
            subPage = asPage(DocumentBasePage.class).getWorkspacesContentTab();

            WorkspaceCreationFormPage workspaceCreationFormPage = subPage.getWorkspaceCreatePage();
            wsPage = workspaceCreationFormPage.createNewWorkspace(title, description);
        }

        return wsPage;
    }

}
