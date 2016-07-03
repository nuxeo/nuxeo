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
 *     Yannis JULIENNE
 */

package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PublishTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SectionContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Date;
import java.util.List;
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
@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITPublishDocumentTests extends AbstractTest {

    protected final static String TEST_SECTION_TITLE = "Test Section " + new Date().getTime();

    protected final static String TEST_NOTE_TITLE = "Test note to be versionned";

    protected final static String TEST_SECTION_URL = String.format(Constants.NXPATH_URL_FORMAT,
            SECTIONS_PATH + TEST_SECTION_TITLE);

    protected final static String TEST_FOLDER_URL = String.format(Constants.NXPATH_URL_FORMAT,
            TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE);

    protected final static String TEST_FILE_URL = String.format(Constants.NXPATH_URL_FORMAT,
            TEST_WORKSPACE_PATH + TEST_FOLDER_TITLE + "/" + TEST_FILE_TITLE);

    protected final static String TEST_FILE_IN_SECTION_URL = String.format(Constants.NXPATH_URL_FORMAT,
            SECTIONS_PATH + TEST_SECTION_TITLE + "/" + TEST_FILE_TITLE);

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
        RestHelper.removePermissions(SECTIONS_PATH, MANAGER_USERNAME);
        RestHelper.removePermissions(SECTIONS_PATH, WRITER_USERNAME);
        RestHelper.cleanup();
    }

    @Test
    public void testPublishDocumentBySectionManager() throws Exception {
        login(MANAGER_USERNAME, MANAGER_USERNAME);

        open(TEST_FOLDER_URL);
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false,
                null, null, null).getPublishTab();

        publishTab.publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        assertEquals(1, publishTab.getPublishingInfos().size());

        assertEquals("Unpublish",
                publishTab.getPublishingInfos().get(0).findElement(By.xpath(".//a[@class='button']")).getText());

        open(TEST_SECTION_URL);

        assertTrue(asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                                                 .goToDocument(TEST_FILE_TITLE)
                                                 .getSummaryTab()
                                                 .isPublished());

        logout();

        // Check the publish status for the readers of the section
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);

        open(TEST_SECTION_URL);
        assertTrue(asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                                                 .goToDocument(TEST_FILE_TITLE)
                                                 .getSummaryTab()
                                                 .isPublished());

        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionManagerApproval() throws Exception {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FOLDER_URL);
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                                                                     .createFile(TEST_FILE_TITLE, "description", false,
                                                                             null, null, null)
                                                                     .getPublishTab()
                                                                     .publish("Local Sections (Domain)", "None",
                                                                             TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Document is waiting for approval
        open(TEST_SECTION_URL);
        assertTrue(
                asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class)
                                              .goToDocument(TEST_FILE_TITLE)
                                              .getSummaryTab()
                                              .isAwaitingPublication());
        logout();

        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        // The document is not visible until approved
        assertTrue(driver.getPageSource().contains("This folder contains no document"));
        logout();
    }

    @Test
    public void testPublishingApprovalBySectionManager() throws Exception {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                                      .getPublishTab()
                                      .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);
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
        open(TEST_SECTION_URL);
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());

        // check the update for the used who published the document
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionWriterApproval() throws Exception {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FOLDER_URL);
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                                                                     .createFile(TEST_FILE_TITLE, "description", false,
                                                                             null, null, null)
                                                                     .getPublishTab()
                                                                     .publish("Local Sections (Domain)", "None",
                                                                             TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Check the document is waiting for approval in the section
        open(TEST_SECTION_URL);
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());

        logout();

        // Check that readers do not see the document waiting for approval
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
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
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        logout();

        // Check that the published now sees the document as published
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isPublished());
        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionWriterReject() throws Exception {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FOLDER_URL);
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                                                                     .createFile(TEST_FILE_TITLE, "description", false,
                                                                             null, null, null)
                                                                     .getPublishTab()
                                                                     .publish("Local Sections (Domain)", "None",
                                                                             TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Check the document is waiting for approval in the section
        open(TEST_SECTION_URL);
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());

        logout();

        // Check that readers do not see the document waiting for approval
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
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
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));
        logout();

        // Check that the publisher also doesn't see the document
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a publisher
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testPublishDocumentBySectionReaderForSectionManagerReject() throws Exception {
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FOLDER_URL);
        PublishTabSubPage publishTab = asPage(DocumentBasePage.class)
                                                                     .createFile(TEST_FILE_TITLE, "description", false,
                                                                             null, null, null)
                                                                     .getPublishTab()
                                                                     .publish("Local Sections (Domain)", "None",
                                                                             TEST_SECTION_TITLE);

        // No unpublish button
        assertEquals(1, publishTab.getPublishingInfos().size());
        assertTrue(publishTab.getPublishingInfos().get(0).findElements(By.xpath(".//a[@class='button']")).isEmpty());

        // Check the document is waiting for approval in the section
        open(TEST_SECTION_URL);
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        assertTrue(section.goToDocument(TEST_FILE_TITLE).getSummaryTab().isAwaitingPublication());

        logout();

        // Check that readers do not see the document waiting for approval
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
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
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));
        logout();

        // Check that the publisher also doesn't see the document
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a publisher
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testUnpublishBySectionManager() throws Exception {
        login(MANAGER_USERNAME, MANAGER_USERNAME);

        // Publish a file
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                                      .getPublishTab()
                                      .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // Unpublish it
        open(TEST_SECTION_URL);
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        section.unpublishDocument(TEST_FILE_TITLE);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        // Check that readers don't see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a reader
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();

        // Check that writers don't see the document in the section
        login(WRITER_USERNAME, WRITER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a writer
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testUnpublishBySectionWriter() throws Exception {
        login(MANAGER_USERNAME, MANAGER_USERNAME);

        // Publish a file
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null)
                                      .getPublishTab()
                                      .publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        logout();

        // Unpublish it
        login(WRITER_USERNAME, WRITER_USERNAME);
        open(TEST_SECTION_URL);
        SectionContentTabSubPage section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertTrue(section.hasDocumentLink(TEST_FILE_TITLE));
        section.unpublishDocument(TEST_FILE_TITLE);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        // Check that readers don't see the document in the section
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a reader
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();

        // Check that managers don't see the document in the section
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(TEST_SECTION_URL);
        section = asPage(DocumentBasePage.class).getContentTab(SectionContentTabSubPage.class);
        assertFalse(section.hasDocumentLink(TEST_FILE_TITLE));

        // Check the document as a manager
        open(TEST_FILE_URL);
        assertEquals(0, asPage(DocumentBasePage.class).getPublishTab().getPublishingInfos().size());

        logout();
    }

    @Test
    public void testMultiplePublications() throws Exception {
        // create file as admin
        login();
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).createFile(TEST_FILE_TITLE, "description", false, null, null, null);

        // publish as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_FILE_URL);
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check result as reader
        open(TEST_FILE_IN_SECTION_URL);
        SummaryTabSubPage summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isAwaitingPublication());
        assertFalse(summaryTab.hasRejectPublicationComment());
        assertFalse(summaryTab.hasApprovePublicationButton());
        assertFalse(summaryTab.hasRejectPublicationButton());

        // publish as publisher
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FILE_URL);
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check result as publisher
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isAwaitingPublication());
        assertFalse(summaryTab.hasRejectPublicationComment());
        assertFalse(summaryTab.hasApprovePublicationButton());
        assertFalse(summaryTab.hasRejectPublicationButton());

        // check result as writer
        login(WRITER_USERNAME, WRITER_USERNAME);
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isAwaitingPublication());
        assertTrue(summaryTab.hasRejectPublicationComment());
        assertTrue(summaryTab.hasApprovePublicationButton());
        assertTrue(summaryTab.hasRejectPublicationButton());

        // publish over as writer
        open(TEST_FILE_URL);
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check result as writer
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isPublished());
        assertFalse(summaryTab.hasRejectPublicationComment());
        assertFalse(summaryTab.hasApprovePublicationButton());
        assertFalse(summaryTab.hasRejectPublicationButton());

        // check result as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isPublished());

        // check result as publisher
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isPublished());

        // unpublish as manager
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(TEST_FILE_URL);
        asPage(DocumentBasePage.class).getPublishTab().unpublish(TEST_SECTION_TITLE, "0.1");

        // check result on homepage as manager
        UserHomePage homePage = asPage(DocumentBasePage.class).getUserHome();
        assertTrue(homePage.isUserTasksEmpty());

        // publish as publisher
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FILE_URL);
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // publish as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_FILE_URL);
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check result on homepage as manager
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        homePage = asPage(DocumentBasePage.class).getUserHome();
        assertTrue(homePage.taskExistsOnUserTasks("Publish Document"));

        // approve publication by task as manager
        summaryTab = homePage.redirectToTask(TEST_FILE_TITLE);
        summaryTab.approvePublication();

        // check result on homepage as manager
        homePage = asPage(DocumentBasePage.class).getUserHome();
        assertTrue(homePage.isUserTasksEmpty());

        // check result as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isPublished());

        // check result as publisher
        login(PUBLISHER_USERNAME, PUBLISHER_USERNAME);
        open(TEST_FILE_IN_SECTION_URL);
        summaryTab = asPage(SummaryTabSubPage.class);
        assertTrue(summaryTab.isPublished());

        logout();
    }

    @Test
    @Ignore("Until NXP-19709 is resolved")
    public void testMultipleVersionsPublicationsByApproval() throws Exception {
        // create file to be versionned and published
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).createNote(TEST_NOTE_TITLE, "first version of the note", false, null);

        // publish note as manager
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // edit note
        asPage(DocumentBasePage.class).getEditTab().edit(TEST_NOTE_TITLE, "second version of the note",
                EditTabSubPage.MINOR_VERSION_INCREMENT_VALUE);

        // check only version 0.1 is published in test section as manager
        open(TEST_SECTION_URL);
        SectionContentTabSubPage sectionPage = asPage(SectionContentTabSubPage.class);
        List<WebElement> items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.1",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // check only version 0.1 is published in test section as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.1",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // publish version 0.2 as reader
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).getContentTab().goToDocument(TEST_NOTE_TITLE).getPublishTab().publish(
                "Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check version 0.2 is waiting for publication approval as reader (need refresh)
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class).refreshContent();
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(2, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());
        assertEquals("0.1",
                items.get(1)
                     .findElement(By.id(
                             "section_content:section_content_repeat:1:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // check both version 0.1 and 0.2 are listed in test section as manager
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(2, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());
        assertEquals("0.1",
                items.get(1)
                     .findElement(By.id(
                             "section_content:section_content_repeat:1:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // approve version 0.2 as manager
        sectionPage.goToDocument(TEST_NOTE_TITLE);
        asPage(SummaryTabSubPage.class).approvePublication();

        // check only 0.2 are published in test section as manager (need refresh)
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class).refreshContent();
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // check result as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // unpublish as admin
        login();
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        sectionPage.unpublishDocument(TEST_NOTE_TITLE);

        // permanent delete as admin
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).getContentTab().removeDocument(TEST_NOTE_TITLE);
        asPage(DocumentBasePage.class).getManageTab().getTrashSubTab().emptyTrash();

    }

    @Test
    @Ignore("Until NXP-19709 is resolved")
    public void testMultipleVersionsPublicationsByPublishOver() throws Exception {
        // create file to be versionned and published
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).createNote(TEST_NOTE_TITLE, "first version of the note", false, null);

        // publish note as manager
        asPage(DocumentBasePage.class).getPublishTab().publish("Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // edit note
        asPage(DocumentBasePage.class).getEditTab().edit(TEST_NOTE_TITLE, "second version of the note",
                EditTabSubPage.MINOR_VERSION_INCREMENT_VALUE);

        // check only version 0.1 is published in test section as manager
        open(TEST_SECTION_URL);
        SectionContentTabSubPage sectionPage = asPage(SectionContentTabSubPage.class);
        List<WebElement> items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.1",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // check only version 0.1 is published in test section as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.1",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // publish version 0.2 as reader
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).getContentTab().goToDocument(TEST_NOTE_TITLE).getPublishTab().publish(
                "Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check version 0.2 is waiting for publication approval as reader (need refresh)
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class).refreshContent();
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(2, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());
        assertEquals("0.1",
                items.get(1)
                     .findElement(By.id(
                             "section_content:section_content_repeat:1:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // check both version 0.1 and 0.2 are listed in test section as manager
        login(MANAGER_USERNAME, MANAGER_USERNAME);
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(2, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());
        assertEquals("0.1",
                items.get(1)
                     .findElement(By.id(
                             "section_content:section_content_repeat:1:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // publish over version 0.2 as manager
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).getContentTab().goToDocument(TEST_NOTE_TITLE).getPublishTab().publish(
                "Local Sections (Domain)", "None", TEST_SECTION_TITLE);

        // check only 0.2 are published in test section as manager (need refresh)
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class).refreshContent();
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // check result as reader
        login(READER_USERNAME, READER_USERNAME);
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        items = sectionPage.getContentView().getItems();
        assertTrue(sectionPage.hasDocumentLink(TEST_NOTE_TITLE));
        assertEquals(1, items.size());
        assertEquals("0.2",
                items.get(0)
                     .findElement(By.id(
                             "section_content:section_content_repeat:0:nxl_document_listing_table_1:nxw_listing_version"))
                     .getText());

        // unpublish as admin
        login();
        open(TEST_SECTION_URL);
        sectionPage = asPage(SectionContentTabSubPage.class);
        sectionPage.unpublishDocument(TEST_NOTE_TITLE);

        // permanent delete as admin
        open(TEST_FOLDER_URL);
        asPage(DocumentBasePage.class).getContentTab().removeDocument(TEST_NOTE_TITLE);
        asPage(DocumentBasePage.class).getManageTab().getTrashSubTab().emptyTrash();

    }
}
