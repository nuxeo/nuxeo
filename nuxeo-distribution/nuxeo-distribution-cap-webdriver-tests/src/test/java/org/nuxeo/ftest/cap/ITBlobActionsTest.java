/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.ftest.cap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.NavigationSubPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
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

    public static final String PREVIEW_FILE_REGEX = "http://.*/api/v1/id/.*/@blob/blobholder:0";

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

        URI previewUri = new URI(previewUrl);
        assertEquals("/nuxeo/viewer/web/viewer.html", previewUri.getPath());

        List<NameValuePair> params = URLEncodedUtils.parse(previewUri, "UTF-8");
        assertEquals(1, params.size());
        assertEquals("file", params.get(0).getName());

        String fileUrl = params.get(0).getValue();
        assertTrue(fileUrl.matches(PREVIEW_FILE_REGEX));

        logout();
    }
}
