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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.forms.DublinCoreCreationDocumentFormPage;
import org.nuxeo.functionaltests.pages.tabs.SectionContentTabSubPage;

import static org.nuxeo.functionaltests.Constants.SECTIONS_PATH;

import static org.junit.Assert.assertEquals;

/**
 * Test section structure creation.
 *
 * @since 8.2
 */
public class ITSectionTest extends AbstractTest {

    protected final static String SECTION_TITLE = "Test Section " + new Date().getTime();

    protected final static String SUB_SECTION_TITLE = "Test Section " + new Date().getTime();

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.addPermission(SECTIONS_PATH, TEST_USERNAME, "Write");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testSectionStructure() throws UserNotConnectedException, IOException {
        try {
            DocumentBasePage documentBasePage = loginAsTestUser();
            documentBasePage.createSection(SECTION_TITLE, "my desc");
            checkAvailableTabs();
            DublinCoreCreationDocumentFormPage sectionCreationPage = asPage(DocumentBasePage.class).getContentTab(
                    SectionContentTabSubPage.class).getSectionCreatePage();
            sectionCreationPage.createDocument("My section", "section desc");
            checkAvailableTabs();
            // create another section then cancel, check it's not there
            DublinCoreCreationDocumentFormPage otherSectionCreationPage = asPage(DocumentBasePage.class).getContentTab(
                    SectionContentTabSubPage.class).getSectionCreatePage();
            otherSectionCreationPage.cancel();
            assertEquals(0, asPage(SectionContentTabSubPage.class).getContentView().getItems().size());
        } finally {
            asPage(DocumentBasePage.class).getNavigationSubPage().goToDocument(Constants.SECTIONS_TITLE);
            asPage(SectionContentTabSubPage.class).removeDocument(SECTION_TITLE);
            logout();
        }
    }

    protected void checkAvailableTabs() {
        asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        asPage(DocumentBasePage.class).getEditTab();
        // click on "permissions" tab, but do not require addition/removal right
        DocumentBasePage page = asPage(DocumentBasePage.class);
        page.clickOnDocumentTabLink(page.permissionsTabLink, false);
        asPage(DocumentBasePage.class).getHistoryTab();
    }

}
