/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ftest.cap.TestConstants.TEST_NOTE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.functionaltests.Constants.NOTE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.contentView.PageNavigationControls;
import org.nuxeo.functionaltests.pages.DocumentBasePage;

/**
 * Tests the navigation on document list which is backuped by a page provider. Tests added due to
 * <a href="https://jira.nuxeo.com/browse/NXP-21674">NXP-21674</a> regression.
 *
 * @since 9.1
 */
public class ITPageProviderNavigation extends AbstractTest {

    private static final int NB_NOTES = 20;

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        for (int i = 0; i < NB_NOTES; i++) {
            RestHelper.createDocument(TEST_WORKSPACE_PATH, NOTE_TYPE, TEST_NOTE_TITLE + i,
                    String.format("Test Note%s description", i));
        }
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testPageNavigation() throws Exception {
        DocumentBasePage page = login();
        // browse to workspace
        DocumentBasePage wsPage = page.goToWorkspaces()
                                      .goToDocumentWorkspaces()
                                      .getNavigationSubPage()
                                      .goToDocument(TEST_WORKSPACE_TITLE);
        // list only NB_NOTES / 4 elements in order to have first / previous / next / last actions
        ContentViewElement contentView = wsPage.getContentTab()
                                               .getContentView()
                                               .getUpperActions()
                                               .selectPageSize(NB_NOTES / 4)
                                               .getContentView();
        assertEquals(1, contentView.getPaginationControls().getCurrentPage());

        contentView = contentView.navigation(PageNavigationControls::last);
        assertEquals(4, contentView.getPaginationControls().getCurrentPage());

        contentView = contentView.navigation(PageNavigationControls::previous);
        assertEquals(3, contentView.getPaginationControls().getCurrentPage());

        contentView = contentView.navigation(PageNavigationControls::first);
        assertEquals(1, contentView.getPaginationControls().getCurrentPage());

        contentView = contentView.navigation(PageNavigationControls::next);
        assertEquals(2, contentView.getPaginationControls().getCurrentPage());

    }

}
