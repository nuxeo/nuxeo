/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Nelson Silva
 */
package org.nuxeo.ftest.cap;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.NavigationSubPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests blob actions
 *
 * @since 7.3
 */
public class ITBlobActionsTest extends AbstractTest {

    public final static String WORKSPACE_ROOT = "Workspaces";

    public final static String WORKSPACE_NAME = "WorkspaceTest";

    public final static String WORKSPACE_DESC = "Workspace Test Description";

    public final static String DOCUMENT_NAME = "DocumentTest";

    public final static String DOCUMENT_DESC = "Document Test Description";

    public static final String PREVIEW_FILE_REGEX = "http://.*/api/v1/repo/default/id/.*/@blob/file:content/@preview/";

    @Test
    public void testBlobPreviewAction() throws Exception {

        // Login
        DocumentBasePage home = login();

        // Navigate to workspaces root
        NavigationSubPage domainContent = home.getNavigationSubPage();
        DocumentBasePage workspacesPage = domainContent.goToDocument(WORKSPACE_ROOT);
        WorkspacesContentTabSubPage workspacesContent = workspacesPage.getWorkspacesContentTab();

        // Create a workspace and navigate into it
        WorkspaceFormPage workspaceFormPage = workspacesContent.getWorkspaceCreatePage();
        DocumentBasePage workspacePage = workspaceFormPage.createNewWorkspace(WORKSPACE_NAME, WORKSPACE_DESC);

        // Create a PDF File
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = createFile(workspacePage, DOCUMENT_NAME, DOCUMENT_DESC, true, filePrefix,
                ".pdf", "Webdriver test file content.");

        // Get actions for main blob
        List<WebElement> actions = filePage.getBlobActions(0);
        assertFalse(actions.isEmpty());

        // Check preview action
        Optional<WebElement> preview = actions.stream()
            .filter((e) -> e.findElement(By.xpath("//img[@title='Preview']")) != null)
            .findFirst();
        assertTrue(preview.isPresent());

        String previewUrl = preview.get().getAttribute("href");
        assertFalse(StringUtils.isEmpty(previewUrl));
        assertTrue(previewUrl.matches(PREVIEW_FILE_REGEX));

        logout();
    }
}
