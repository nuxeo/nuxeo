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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.CommentsTabSubPage;

import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_URL;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 8.2
 */
public class ITCommentTest extends AbstractTest {

    @Before
    public void before() throws Exception {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createUser("bree", "bree1", null, null, null, null, "members");
        RestHelper.createUser("lbramard", "lbramard1", null, null, null, null, "members");
        RestHelper.createUser("jsmith", "jsmith1", null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
    }

    @After
    public void after() throws IOException {
        RestHelper.cleanup();
    }

    @Test
    public void testAddComment() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_FILE_URL);

        CommentsTabSubPage page = asPage(DocumentBasePage.class).getCommentsTab();
        page = page.addComment("Comment number 1");
        assertTrue(page.hasComment("Comment number 1"));
    }

    @Test
    public void testReplyToComment() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open(TEST_FILE_URL);

        CommentsTabSubPage page = asPage(DocumentBasePage.class).getCommentsTab();
        page = page.addComment("Comment number 1");
        assertTrue(page.hasComment("Comment number 1"));

        login("bree", "bree1");
        open(TEST_FILE_URL);
        page = asPage(DocumentBasePage.class).getCommentsTab();
        page = page.reply("Answer number 1");
        assertTrue(page.hasComment("Answer number 1"));

        page = page.addComment("Comment number 2");
        assertTrue(page.hasComment("Comment number 2"));
    }

    @Test
    public void testDeleteComment() throws DocumentBasePage.UserNotConnectedException {
        login("lbramard", "lbramard1");
        open(TEST_FILE_URL);

        CommentsTabSubPage page = asPage(DocumentBasePage.class).getCommentsTab();
        page = page.addComment("Comment number 3");
        page = page.addComment("Comment number 4");
        assertTrue(page.hasComment("Comment number 3"));
        assertTrue(page.hasComment("Comment number 4"));

        page.delete();
        assertFalse(page.hasComment("Comment number 3"));
        assertTrue(page.hasComment("Comment number 4"));

        login("jsmith", "jsmith1");
        open(TEST_FILE_URL);
        page = asPage(DocumentBasePage.class).getCommentsTab();
        assertFalse(page.canDelete());
    }
}
