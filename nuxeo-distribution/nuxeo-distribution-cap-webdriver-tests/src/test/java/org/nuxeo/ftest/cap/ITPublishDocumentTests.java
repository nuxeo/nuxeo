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
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.tabs.SectionContentTabSubPage;
import org.openqa.selenium.By;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FOLDER_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.functionaltests.Constants.FOLDER_TYPE;
import static org.nuxeo.functionaltests.Constants.SECTIONS_PATH;
import static org.nuxeo.functionaltests.Constants.SECTION_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

/**
 * @since 8.2
 */
public class ITPublishDocumentTests extends AbstractTest {

    protected final static String TEST_SECTION_TITLE = "Test Section " + new Date().getTime();
    private static final String TEST_USERNAME_2 = "linnet";
    private static final String TEST_USERNAME_3 = "bree";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FOLDER_TYPE, TEST_FOLDER_TITLE, "Test folder description");
        RestHelper.createDocument(SECTIONS_PATH, SECTION_TYPE, TEST_SECTION_TITLE, null);
        RestHelper.createUser(TEST_USERNAME, TEST_USERNAME, null, null, null, null, "members");
        RestHelper.createUser(TEST_USERNAME_2, TEST_USERNAME_2, null, null, null, null, "members");
        RestHelper.createUser(TEST_USERNAME_3, TEST_USERNAME_3, null, null, null, null, "members");
        RestHelper.addPermission(TEST_WORKSPACE_PATH, TEST_USERNAME, "Write");
        RestHelper.addPermission(TEST_WORKSPACE_PATH, TEST_USERNAME_2, "Write");
        RestHelper.addPermission(SECTIONS_PATH, TEST_USERNAME, "Everything");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testPublishDocumentBySectionManager() throws UserNotConnectedException, IOException {
        login(TEST_USERNAME, TEST_USERNAME);

        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));

        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getFilePublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        assertEquals(
                "Unpublish",
                asPage(DocumentBasePage.class).findElementWithTimeout(
                        By.xpath("//div[@id='publishTreeForm:publishingInfoList'] //a[@class='button']")).getText());

        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));

        assertEquals(
                "This document is published.",
                asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                        .goToDocument(TEST_FILE_TITLE)
                        .findElementWithTimeout(
                                By.xpath("//div[@class='publication_block'] //div //div"))
                        .getText());

        logout();

        login(TEST_USERNAME_2, TEST_USERNAME_2);

        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));

        assertEquals(
                "This document is published.",
                asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                        .goToDocument(TEST_FILE_TITLE)
                        .findElementWithTimeout(
                                By.xpath("//div[@class='publication_block'] //div //div"))
                        .getText());

        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionManagerApproval() throws UserNotConnectedException, IOException {
        login(TEST_USERNAME_2, TEST_USERNAME_2);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getFilePublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);
        // No unpublish button
        assertEquals(0, driver.findElements(By.xpath("//div[@id='publishTreeForm:publishingInfoList'] //a[@class='button']")).size());
        // Document is waiting for approval
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        String publicationSatus = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                .goToDocument(TEST_FILE_TITLE)
                .findElementWithTimeout(By.xpath("//div[@class='publication_block'] //div"))
                .getText();
        assertTrue(publicationSatus, publicationSatus.contains("This document is waiting for a publication approval"));
        logout();

        login(TEST_USERNAME_3, TEST_USERNAME_3);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        // The document is not visible until approved
        assertTrue(driver.getPageSource().contains("This folder contains no document"));
        logout();
    }

    @Test
    public void testPublishingApprovalBySectionManager() throws UserNotConnectedException, IOException {
        login(TEST_USERNAME_2, TEST_USERNAME_2);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getFilePublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        logout();

        // manager has a publication task
        DocumentBasePage filePage = login(TEST_USERNAME, TEST_USERNAME);
        UserHomePage homePage = filePage.getUserHome();
        // TODO this does not work
        // assertFalse(homePage.isTaskGadgetEmpty());

        // TODO approve document

        // TODO check dashboard my task is empty

        logout();

        // TODO login as bree

        // TODO check that the document is published

    }
}
