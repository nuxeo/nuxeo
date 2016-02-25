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
 *     Gabriel Barata
 */

package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;

import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

/**
 * Test section structure creation.
 *
 * @since 8.2
 */
public class ITForumTest extends AbstractTest {

    private static final String TEST_FORUM_TITLE = "Test Forum";

    private static final String TEST_FORUM_DESCRIPTION = "Test Forum Description";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testCreateForum() throws UserNotConnectedException {
        login();

        open(TEST_WORKSPACE_URL);
        asPage(DocumentBasePage.class).getContentTab()
                                      .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                      .goToDocumentByBreadcrumb(TEST_WORKSPACE_TITLE)
                                      .getContentTab()
                                      .hasDocumentLink(TEST_FORUM_TITLE);

        logout();

        login();

        open(TEST_WORKSPACE_URL);
        asPage(DocumentBasePage.class).getContentTab().hasDocumentLink(TEST_FORUM_TITLE);

        logout();
    }
}
