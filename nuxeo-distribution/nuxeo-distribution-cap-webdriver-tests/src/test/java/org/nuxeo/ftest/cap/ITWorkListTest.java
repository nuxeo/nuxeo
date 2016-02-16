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
 *     Maxime Hilaire
 */
package org.nuxeo.ftest.cap;

import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;

public class ITWorkListTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, "John", "Smith", "Nuxeo", "jsmith@nuxeo.com", "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void userCanAddItemToWorklist() throws Exception {
        DocumentBasePage page = login(TEST_USERNAME, TEST_PASSWORD).getContentTab().goToDocument("Workspaces");

        page.getContentTab().addToWorkList(TEST_WORKSPACE_TITLE);

        logout();

        login(TEST_USERNAME, TEST_PASSWORD).getContentTab().goToDocument("Workspaces");

        DocumentBasePage.findElementWithTimeout(
                By.xpath("//div[@id='clipboardCopy']//a[text()='" + TEST_WORKSPACE_TITLE + "']"));

        logout();
    }
}
