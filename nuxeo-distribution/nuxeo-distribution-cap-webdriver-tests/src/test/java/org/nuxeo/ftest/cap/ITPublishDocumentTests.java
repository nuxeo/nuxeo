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
 *     Nelson Silva
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
import org.nuxeo.functionaltests.pages.tabs.PublishTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SectionContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * @since 8.3
 */
public class ITPublishDocumentTests extends AbstractTest {

    protected final static String TEST_SECTION_TITLE = "Test Section " + new Date().getTime();

    private static final String MANAGER_USERNAME = TEST_USERNAME;

    private static final String PUBLISHER_USERNAME = "linnet";

    private static final String READER_USERNAME = "bree";

    private static final String WRITER_USERNAME = "jsmith";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FOLDER_TYPE, TEST_FOLDER_TITLE, "Test folder description");
        RestHelper.createDocument(SECTIONS_PATH, SECTION_TYPE, TEST_SECTION_TITLE, null);
        RestHelper.createUser(TEST_USERNAME, TEST_USERNAME, null, null, null, null, "members");
        RestHelper.createUser(PUBLISHER_USERNAME, PUBLISHER_USERNAME, null, null, null, null, "members");
        RestHelper.createUser(READER_USERNAME, READER_USERNAME, null, null, null, null, "members");
        RestHelper.createUser(WRITER_USERNAME, WRITER_USERNAME, null, null, null, null, "members");
        RestHelper.addPermission(TEST_WORKSPACE_PATH, MANAGER_USERNAME, "Write");
        RestHelper.addPermission(TEST_WORKSPACE_PATH, PUBLISHER_USERNAME, "Write");
        RestHelper.addPermission(SECTIONS_PATH, MANAGER_USERNAME, "Everything");
        RestHelper.addPermission(SECTIONS_PATH, WRITER_USERNAME, "Write");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testPublishDocumentBySectionManager() throws UserNotConnectedException, IOException {
        login(MANAGER_USERNAME, MANAGER_USERNAME);

        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                .createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab();

        publishTab.publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        assertEquals(1, publishTab.getPublishingInfos().size());

        assertEquals("Unpublish",
                publishTab.getPublishingInfos().get(0).findElement(By.xpath(".//a[@class='button']")).getText());

        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));

        assertTrue(asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                .goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());

        logout();

        // Check the publish status for the readers of the section
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);

        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        assertTrue(asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                .goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());

        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionManagerApproval()
            throws UserNotConnectedException, IOException {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                .createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Document is waiting for approval
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        assertTrue(asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                .goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());
        logout();

        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        // The document is not visible until approved
        assertTrue(driver.getPageSource().contains("This folder contains no document"));
        logout();
    }

    @Test
    public void testPublishingApprovalBySectionManager() throws UserNotConnectedException, IOException {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);
        logout();

        // manager has a publication task in the home dashboard
        DocumentBasePage filePage = login(MANAGER_USERNAME, MANAGER_USERNAME);
        UserHomePage homePage = filePage.getUserHome();
        assertFalse(homePage.isUserTasksEmpty());
        assertTrue(homePage.taskExistsOnUserTasks("Publish Document"));

        SummaryTabSubPage doc = homePage.redirectToTask(TEST_FILE_TITLE);

        // check the publish request
        assertTrue(doc.isAwaitingPublication());
        doc = doc.approvePublication();
        assertTrue(doc.isPublished());

        // re-check the dashboard
        homePage = filePage.getUserHome();
        assertTrue(homePage.isUserTasksEmpty());
        logout();

        // check readers can see the published document
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());

        // check the update for the used who published the document
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionWriterApproval()
            throws UserNotConnectedException, IOException {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                .createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Check the document is waiting for approval in the section
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());

        logout();

        // Check that readers do not see the document waiting for approval
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        logout();

        // PublishingApprovalBySectionWriter

        // manager has a publication task in the home dashboard
        DocumentBasePage filePage = login(WRITER_USERNAME, WRITER_USERNAME);
        UserHomePage homePage = filePage.getUserHome();
        assertFalse(homePage.isUserTasksEmpty());
        assertTrue(homePage.taskExistsOnUserTasks("Publish Document"));

        SummaryTabSubPage doc = homePage.redirectToTask(TEST_FILE_TITLE);

        // Check the publication request
        assertTrue(doc.isAwaitingPublication());

        // Approve the publication
        doc = doc.approvePublication();
        assertTrue(doc.isPublished());

        // Check the user tasks in the dashboard are empty
        homePage = filePage.getUserHome();
        assertTrue(homePage.isUserTasksEmpty());
        logout();

        // Check that readers now see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        logout();

        // Check that the published now sees the document as published
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());
        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionWriterReject() throws UserNotConnectedException,
            IOException {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                .createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Check the document is waiting for approval in the section
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());

        logout();

        // Check that readers do not see the document waiting for approval
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        logout();

        // publishingRejectBySectionWriter

        // section writer has a publication task in the home dashboard
        DocumentBasePage filePage = login(WRITER_USERNAME, WRITER_USERNAME);
        UserHomePage homePage = filePage.getUserHome();
        assertFalse(homePage.isUserTasksEmpty());
        assertTrue(homePage.taskExistsOnUserTasks("Publish Document"));

        SummaryTabSubPage doc = homePage.redirectToTask(TEST_FILE_TITLE);

        // Check the publication request
        assertTrue(doc.isAwaitingPublication());

        // Reject the publication
        doc.rejectPublication("refuse");
        section = asPage(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the user tasks in the dashboard are empty
        homePage = section.getUserHome();
        assertTrue(homePage.isUserTasksEmpty());
        logout();

        // Check that readers don't see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));
        logout();

        // Check that the publisher also doesn't see the document
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a publisher
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionManagerReject()
            throws UserNotConnectedException, IOException {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                .createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Check the document is waiting for approval in the section
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());

        logout();

        // Check that readers do not see the document waiting for approval
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        logout();

        // publishingRejectBySectionManager

        // manager has a publication task in the home dashboard
        DocumentBasePage filePage = login(MANAGER_USERNAME, MANAGER_USERNAME);
        UserHomePage homePage = filePage.getUserHome();
        assertFalse(homePage.isUserTasksEmpty());
        assertTrue(homePage.taskExistsOnUserTasks("Publish Document"));

        SummaryTabSubPage doc = homePage.redirectToTask(TEST_FILE_TITLE);

        // Check the publication request
        assertTrue(doc.isAwaitingPublication());

        // Reject the publication
        doc.rejectPublication("refuse");
        section = asPage(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the user tasks in the dashboard are empty
        homePage = section.getUserHome();
        assertTrue(homePage.isUserTasksEmpty());
        logout();

        // Check that readers don't see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));
        logout();

        // Check that the publisher also doesn't see the document
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a publisher
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testUnpublishBySectionManager() throws IOException, UserNotConnectedException {
        login(MANAGER_USERNAME, MANAGER_USERNAME);

        // Publish a file
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab()
                .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // Unpublish it
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        section.unpublishDocument(TEST_FILE_TITLE);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        // Check that readers don't see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a reader
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();

        // Check that writers don't see the document in the section
        login(WRITER_USERNAME, WRITER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a writer
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testUnpublishBySectionWriter() throws UserNotConnectedException, IOException {
        login(MANAGER_USERNAME, MANAGER_USERNAME);

        // Publish a file
        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE));
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                .getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        logout();

        // Unpublish it
        login(WRITER_USERNAME, WRITER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        section.unpublishDocument(TEST_FILE_TITLE);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        // Check that readers don't see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a reader
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();

        // Check that managers don't see the document in the section
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(String.format(Constants.NXPATH_URL_FORMAT, SECTIONS_PATH + TEST_SECTION_TITLE));
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a manager
        open(String.format(Constants.NXPATH_URL_FORMAT,
                TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE));
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }
}
