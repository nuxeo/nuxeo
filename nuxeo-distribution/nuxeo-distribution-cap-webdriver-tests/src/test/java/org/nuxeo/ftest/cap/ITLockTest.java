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
import java.util.ArrayList;
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

import static org.junit.Assert.assertTrue;

/**
 * @since 8.2
 */
public class ITLockTest extends AbstractTest {

    public static final String DOCUMENT_LOCKED = "Locked";

    private List<String> usersToDelete = new ArrayList<>();

    private List<String> documentsToDelete = new ArrayList<>();

    @Before
    public void before() throws Exception {
        usersToDelete.add(RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members"));
        usersToDelete.add(RestHelper.createUser("bree", "bree1", null, null, null, null, "members"));
        documentsToDelete.add(RestHelper.createDocument("/default-domain/workspaces", "Workspace", "ws", null));
        RestHelper.createDocument("/default-domain/workspaces/ws", "File", "file", null);
        RestHelper.addPermission("/default-domain/workspaces/ws/file", TEST_USERNAME, "Write");
    }

    @After
    public void after() throws IOException {
        for (String user : usersToDelete) {
            RestHelper.deleteUser(user);
        }
        for (String id : documentsToDelete) {
            RestHelper.deleteDocument(id);
        }
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
