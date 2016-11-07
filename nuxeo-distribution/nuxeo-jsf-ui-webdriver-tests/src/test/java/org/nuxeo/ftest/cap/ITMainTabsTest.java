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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;

/**
 * Test main tabs management.
 *
 * @since 7.3
 */
public class ITMainTabsTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, "ws", null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    /**
     * Non-regression test for NXP-17207
     */
    /*
     * We explicitly create a workspace to find something in the search results. Just relying on the
     * automatically-created user workspace is not enough as its creation does not come from a user action so its
     * indexing is asynchronous and in some rare cases we won't find it in the search.
     */
    @Test
    public void testSearchTabSelection() throws Exception {
        DocumentBasePage documentBasePage = login();
        assertTrue(documentBasePage.isMainTabSelected(documentBasePage.documentManagementLink));
        assertFalse(documentBasePage.isMainTabSelected(documentBasePage.searchPageLink));
        documentBasePage.goToSearchPage();
        assertFalse(documentBasePage.isMainTabSelected(documentBasePage.documentManagementLink));
        assertTrue(documentBasePage.isMainTabSelected(documentBasePage.searchPageLink));
        // click on first search result, at least the test workspace should be there
        Locator.findElementWaitUntilEnabledAndClick(By.cssSelector("span[class='documentTitle']"));
        documentBasePage = AbstractTest.asPage(DocumentBasePage.class);
        // check that workspace main tab is now selected
        assertTrue(documentBasePage.isMainTabSelected(documentBasePage.documentManagementLink));
        assertFalse(documentBasePage.isMainTabSelected(documentBasePage.searchPageLink));
    }
}
