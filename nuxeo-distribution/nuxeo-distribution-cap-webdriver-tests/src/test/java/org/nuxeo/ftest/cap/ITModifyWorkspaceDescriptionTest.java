/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_URL;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;

/**
 * <p>
 * Test Modifying a workspace description in Nuxeo DM.
 * </p>
 * <p>
 * Requirements: the user TEST_USERNAME is created
 * </p>
 * <ol>
 * <li>loginAs TEST_USERNAME</li>
 * <li>followLink to testWorkspace1</li>
 * <li>modifyWorkspaceDescription</li>
 * <li>logout</li>
 * </ol>
 */
public class ITModifyWorkspaceDescriptionTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, "John", "Smith", "Nuxeo", "jsmith@nuxeo.com", "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.addPermission(TEST_WORKSPACE_PATH, TEST_USERNAME, "Everything");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testModifyWsDescription() throws Exception {
        login();
        open(TEST_WORKSPACE_URL);

        // Modify Workspace description
        String descriptionModified = "Description modified";
        DocumentBasePage workspacePage = asPage(DocumentBasePage.class).getEditTab().edit(null, descriptionModified,
                null);

        assertEquals(descriptionModified, workspacePage.getCurrentFolderishDescription());
        assertEquals(TEST_WORKSPACE_TITLE, workspacePage.getCurrentDocumentTitle());

        logout();
    }

}
