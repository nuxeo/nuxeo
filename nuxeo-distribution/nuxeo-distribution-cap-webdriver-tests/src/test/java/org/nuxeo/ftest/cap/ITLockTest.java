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

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.nuxeo.functionaltests.pages.tabs.CommentsTabSubPage;

/**
 * @since 8.2
 */
public class ITLockTest extends AbstractTest {

    private static final String DOCUMENT_LOCKED = "Locked";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createUser("bree", "bree1", null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
        RestHelper.addPermission(TEST_FILE_PATH, TEST_USERNAME, "Write");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testLockAndUnlockDocumentByLockOwner() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");

        ContextualActions actions = asPage(FileDocumentBasePage.class).getContextualActions();
        actions = actions.clickOnButton(actions.lockButton);
        List<String> states = asPage(FileDocumentBasePage.class).getCurrentStates();
        assertTrue(states.contains(DOCUMENT_LOCKED));

        login("bree", "bree1");
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");
        states = asPage(FileDocumentBasePage.class).getCurrentStates();
        assertTrue(states.contains(DOCUMENT_LOCKED));

        login(TEST_USERNAME, TEST_PASSWORD);
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");

        actions.clickOnButton(actions.unlockButton);
        states = asPage(FileDocumentBasePage.class).getCurrentStates();
        Assert.assertFalse(states.contains(DOCUMENT_LOCKED));

        login("bree", "bree1");
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");
        states = asPage(FileDocumentBasePage.class).getCurrentStates();
        Assert.assertFalse(states.contains(DOCUMENT_LOCKED));
    }

    @Test
    public void testLockAndUnlockDocumentByManager() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");

        ContextualActions actions = asPage(FileDocumentBasePage.class).getContextualActions();
        actions.clickOnButton(actions.lockButton);
        List<String> states = asPage(FileDocumentBasePage.class).getCurrentStates();
        assertTrue(states.contains(DOCUMENT_LOCKED));

        login(TEST_USERNAME, TEST_PASSWORD);
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");

        actions = asPage(FileDocumentBasePage.class).getContextualActions();
        actions.clickOnButton(actions.unlockButton);
        states = asPage(FileDocumentBasePage.class).getCurrentStates();
        Assert.assertFalse(states.contains(DOCUMENT_LOCKED));

        login(TEST_USERNAME, TEST_PASSWORD);
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");

        states = asPage(FileDocumentBasePage.class).getCurrentStates();
        Assert.assertFalse(states.contains(DOCUMENT_LOCKED));
    }

    @Test
    public void testCommentOnLockedDocument() throws DocumentBasePage.UserNotConnectedException {
        login(TEST_USERNAME, TEST_PASSWORD);
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");

        ContextualActions actions = asPage(FileDocumentBasePage.class).getContextualActions();
        actions.clickOnButton(actions.lockButton);
        List<String> states = asPage(FileDocumentBasePage.class).getCurrentStates();
        assertTrue(states.contains(DOCUMENT_LOCKED));

        CommentsTabSubPage page = asPage(DocumentBasePage.class).getCommentsTab();
        page = page.addComment("First comment on a locked document");
        assertTrue(page.hasComment("First comment on a locked document"));

        login("bree", "bree1");
        open("/nxpath/default/default-domain/workspaces/ws/file@view_documents");
        page = asPage(DocumentBasePage.class).getCommentsTab();
        page = page.reply("First answer on a locked document");
        assertTrue(page.hasComment("First answer on a locked document"));

        page = page.addComment("Second comment on a locked document");
        assertTrue(page.hasComment("Second comment on a locked document"));
    }

}
