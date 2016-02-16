/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Tests blob actions
 *
 * @since 7.3
 */
public class ITBlobActionsTest extends AbstractTest {

    public final static String DOCUMENT_NAME = "DocumentTest";

    public final static String DOCUMENT_DESC = "Document Test Description";

    public static final String PREVIEW_FILE_REGEX = ".*openFancyBox\\('.*/api/v1/repo/default/id/.*/@blob/file:content/@preview/'.*\\).*";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testBlobPreviewAction() throws Exception {
        login();
        open(TEST_WORKSPACE_URL);

        // Create a PDF File
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = createFile(asPage(DocumentBasePage.class), DOCUMENT_NAME, DOCUMENT_DESC, true,
                filePrefix, ".pdf", "Webdriver test file content.");

        // Get actions for main blob
        List<WebElement> actions = filePage.getBlobActions(0);
        assertFalse(actions.isEmpty());

        // Check preview action
        Optional<WebElement> preview = actions.stream()
                                              .filter((e) -> e.findElement(By.xpath("//img[@title='Preview']")) != null)
                                              .findFirst();
        assertTrue(preview.isPresent());

        String onclick = preview.get().getAttribute("onclick");
        assertFalse(StringUtils.isEmpty(onclick));
        // Remove escaping
        onclick = onclick.replace("\\\\\\", "");
        assertTrue(onclick.matches(PREVIEW_FILE_REGEX));

        logout();
    }
}
