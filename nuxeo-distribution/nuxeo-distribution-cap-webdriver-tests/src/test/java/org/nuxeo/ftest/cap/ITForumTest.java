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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FORUM_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FORUM_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;
import static org.nuxeo.functionaltests.pages.tabs.TopicTabSubPage.COMMENT_STATUS_PUBLISHED;
import static org.nuxeo.functionaltests.pages.tabs.TopicTabSubPage.COMMENT_STATUS_WAITING_APPROVAL;
import static org.nuxeo.functionaltests.pages.tabs.TopicTabSubPage.COMMENT_STATUS_REJECTED;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.tabs.TopicTabSubPage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test section structure creation.
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITForumTest extends AbstractTest {

    private static final String TEST_FORUM_DESCRIPTION = "Test Forum Description";

    private static final String TEST_TOPIC_NO_MODERATION_TITLE = "Test topic without moderation";

    private static final String TEST_TOPIC_NO_MODERATION_PATH = TEST_FORUM_PATH
            + TEST_TOPIC_NO_MODERATION_TITLE.substring(0, 24);

    private static final String TEST_TOPIC_NO_MODERATION_URL = String.format(Constants.NXPATH_URL_FORMAT,
            TEST_TOPIC_NO_MODERATION_PATH);

    private static final String TEST_TOPIC_MODERATION_TITLE = "Test topic with moderation";

    private static final String TEST_TOPIC_MODERATION_PATH = TEST_FORUM_PATH
            + TEST_TOPIC_MODERATION_TITLE.substring(0, 24);

    private static final String TEST_TOPIC_MODERATION_URL = String.format(Constants.NXPATH_URL_FORMAT,
            TEST_TOPIC_MODERATION_PATH);

    private static final String TEST_USERNAME_2 = "ojdoe";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createUser(TEST_USERNAME, TEST_USERNAME, null, null, null, null, "members");
        RestHelper.createUser(TEST_USERNAME_2, TEST_USERNAME_2, null, null, null, null, "members");
        RestHelper.addPermission(TEST_WORKSPACE_PATH, TEST_USERNAME, SecurityConstants.READ_WRITE);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testCreateForum() throws UserNotConnectedException {
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_WORKSPACE_URL);
        asPage(DocumentBasePage.class).getContentTab()
                                      .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                      .goToDocumentByBreadcrumb(TEST_WORKSPACE_TITLE)
                                      .getContentTab()
                                      .hasDocumentLink(TEST_FORUM_TITLE);

        logout();

        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_WORKSPACE_URL);
        asPage(DocumentBasePage.class).getContentTab().hasDocumentLink(TEST_FORUM_TITLE);
        logout();
    }

    @Test
    public void testCreateTopicWithoutModeration() throws UserNotConnectedException {
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_WORKSPACE_URL);

        asPage(DocumentBasePage.class).getContentTab()
                                      .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                      .createTopic(TEST_TOPIC_NO_MODERATION_TITLE, "description", false);

        logout();

        login(TEST_USERNAME_2, TEST_USERNAME_2);

        open(String.format(Constants.NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH + TEST_FORUM_TITLE));

        List<WebElement> children = asPage(DocumentBasePage.class).getForumTab().getChildTopicRows();
        assertEquals(1, children.size());
        verifyTopicNameAndModeration(children.get(0), TEST_TOPIC_NO_MODERATION_TITLE, false);

        logout();
    }

    @Test
    public void testCreateTopicWithModeration() throws UserNotConnectedException {
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_WORKSPACE_URL);

        TopicTabSubPage page = asPage(DocumentBasePage.class).getContentTab()
                                                             .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                                             .createTopic(TEST_TOPIC_MODERATION_TITLE, "description",
                                                                     true, TEST_USERNAME_2);

        assertEquals(TEST_TOPIC_MODERATION_TITLE, page.getCurrentDocumentTitle());
        List<WebElement> children = page.goToDocumentByBreadcrumb(TEST_FORUM_TITLE).getForumTab().getChildTopicRows();
        verifyTopicNameAndModeration(children.get(0), TEST_TOPIC_MODERATION_TITLE, true);

        logout();
    }

    @Test
    public void testAddReplyCommentOnTopicWithoutModeration() throws UserNotConnectedException {
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_WORKSPACE_URL);

        TopicTabSubPage topicTab = asPage(DocumentBasePage.class).getContentTab()
                                                                 .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                                                 .createTopic(TEST_TOPIC_NO_MODERATION_TITLE,
                                                                         "description", false);

        // add comment
        assertTrue(topicTab.isCommentFormDisplayed());
        topicTab = topicTab.addComment("test comment", "this is a test comment");
        assertFalse(topicTab.isCommentFormDisplayed());

        // check result
        topicTab.checkComment("test comment", TEST_USERNAME, "this is a test comment", COMMENT_STATUS_PUBLISHED, true,
                false, true);

        // reply to comment
        topicTab = topicTab.reply("test comment", "this is a reply");

        // check result as author
        topicTab.checkComment("Re:test comment", TEST_USERNAME, "this is a reply", COMMENT_STATUS_PUBLISHED, true,
                false, true);
        UserHomePage userHomePage = asPage(DocumentBasePage.class).getUserHome();
        assertTrue(userHomePage.hasUserDocument(TEST_TOPIC_NO_MODERATION_TITLE));
        assertTrue(userHomePage.hasDomainDocument(TEST_TOPIC_NO_MODERATION_TITLE));
        logout();

        // check result as member
        userHomePage = login(TEST_USERNAME_2, TEST_USERNAME_2).getUserHome();
        assertTrue(userHomePage.hasDomainDocument(TEST_TOPIC_NO_MODERATION_TITLE));
        userHomePage.goToDomainDocument(TEST_TOPIC_NO_MODERATION_TITLE);
        topicTab = asPage(TopicTabSubPage.class);
        topicTab.checkComment("test comment", TEST_USERNAME, "this is a test comment", COMMENT_STATUS_PUBLISHED, false,
                false, false);
        topicTab.checkComment("Re:test comment", TEST_USERNAME, "this is a reply", COMMENT_STATUS_PUBLISHED, false,
                false, false);

        logout();
    }

    @Test
    public void testAddReplyApproveRejectCommentOnTopicWithModeration() throws UserNotConnectedException {
        // create topic as admin
        login();
        open(TEST_WORKSPACE_URL);

        TopicTabSubPage topicTab = asPage(DocumentBasePage.class).getContentTab()
                                                                 .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                                                 .createTopic(TEST_TOPIC_MODERATION_TITLE,
                                                                         "description", true);

        // add comment as test user
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        assertTrue(topicTab.isCommentFormDisplayed());
        topicTab = topicTab.addComment("test comment", "this is a test comment");
        assertFalse(topicTab.isCommentFormDisplayed());

        // check result as test user
        topicTab.checkComment("test comment", TEST_USERNAME, "this is a test comment", COMMENT_STATUS_WAITING_APPROVAL,
                false, false, true);

        // check result as member
        login(TEST_USERNAME_2, TEST_USERNAME_2);
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        assertFalse(topicTab.hasComment("test comment"));

        // reject comment
        login();
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        topicTab.checkComment("test comment", TEST_USERNAME, "this is a test comment", COMMENT_STATUS_WAITING_APPROVAL,
                false, true, true);
        topicTab = topicTab.reject("test comment");

        // check result as admin
        topicTab.checkComment("test comment", TEST_USERNAME, "this is a test comment", COMMENT_STATUS_REJECTED, false,
                false, true);

        // check result as member
        login(TEST_USERNAME_2, TEST_USERNAME_2);
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        assertFalse(topicTab.hasComment("test comment"));

        // check result as test user
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        topicTab.checkComment("test comment", TEST_USERNAME, "this is a test comment", COMMENT_STATUS_REJECTED, false,
                false, true);

        // add another comment as test user
        topicTab.showCommentForm();
        topicTab = topicTab.addComment("test comment to be approved", "this is another test comment");
        topicTab.checkComment("test comment to be approved", TEST_USERNAME, "this is another test comment",
                COMMENT_STATUS_WAITING_APPROVAL, false, false, true);

        // approve it as admin
        login();
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        topicTab = topicTab.approve("test comment to be approved");

        // check result as test user
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_TOPIC_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);
        topicTab.checkComment("test comment to be approved", TEST_USERNAME, "this is another test comment",
                COMMENT_STATUS_PUBLISHED, true, false, true);

        // reply to comment
        topicTab = topicTab.reply("test comment to be approved", "this is a reply");

        // check result as author
        topicTab.checkComment("Re:test comment to be approved", TEST_USERNAME, "this is a reply",
                COMMENT_STATUS_WAITING_APPROVAL, false, false, true);
        UserHomePage userHomePage = asPage(DocumentBasePage.class).getUserHome();
        assertTrue(userHomePage.hasDomainDocument(TEST_TOPIC_MODERATION_TITLE));
        logout();

        // check result as member
        userHomePage = login(TEST_USERNAME_2, TEST_USERNAME_2).getUserHome();
        assertTrue(userHomePage.hasDomainDocument(TEST_TOPIC_MODERATION_TITLE));
        userHomePage.goToDomainDocument(TEST_TOPIC_MODERATION_TITLE);
        topicTab = asPage(TopicTabSubPage.class);
        topicTab.checkComment("test comment to be approved", TEST_USERNAME, "this is another test comment",
                COMMENT_STATUS_PUBLISHED, false, false, false);
        assertFalse(topicTab.hasComment("Re:test comment to be approve"));

        logout();

    }

    @Test
    public void testDeleteCommentOnTopicWithoutModeration() throws UserNotConnectedException {
        login(TEST_USERNAME, TEST_USERNAME);
        open(TEST_WORKSPACE_URL);

        TopicTabSubPage topicTab = asPage(DocumentBasePage.class).getContentTab()
                                                                 .createForum(TEST_FORUM_TITLE, TEST_FORUM_DESCRIPTION)
                                                                 .createTopic(TEST_TOPIC_NO_MODERATION_TITLE,
                                                                         "description", false);

        // add 2 comments an 2 replies
        topicTab = topicTab.addComment("test comment 1", "this is the test comment 1");
        topicTab = topicTab.reply("test comment 1", "this is a reply to test comment 1");
        topicTab.showCommentForm();
        topicTab = topicTab.addComment("test comment 2", "this is the test comment 2");
        topicTab = topicTab.reply("test comment 2", "this is a reply to test comment 2");

        // check result
        topicTab.checkComment("test comment 1", TEST_USERNAME, "this is the test comment 1", COMMENT_STATUS_PUBLISHED,
                true, false, true);
        topicTab.checkComment("Re:test comment 1", TEST_USERNAME, "this is a reply to test comment 1",
                COMMENT_STATUS_PUBLISHED, true, false, true);
        topicTab.checkComment("test comment 2", TEST_USERNAME, "this is the test comment 2", COMMENT_STATUS_PUBLISHED,
                true, false, true);
        topicTab.checkComment("Re:test comment 2", TEST_USERNAME, "this is a reply to test comment 2",
                COMMENT_STATUS_PUBLISHED, true, false, true);

        // delete as author
        topicTab = topicTab.delete("test comment 1");
        // check result
        assertFalse(topicTab.hasComment("test comment 1"));
        assertFalse(topicTab.hasComment("Re:test comment 1"));
        logout();

        // log as administrator
        login();
        open(TEST_TOPIC_NO_MODERATION_URL);
        topicTab = asPage(TopicTabSubPage.class);

        // check comments
        assertFalse(topicTab.hasComment("test comment 1"));
        assertFalse(topicTab.hasComment("Re:test comment 1"));
        topicTab.checkComment("test comment 2", TEST_USERNAME, "this is the test comment 2", COMMENT_STATUS_PUBLISHED,
                true, false, true);
        topicTab.checkComment("Re:test comment 2", TEST_USERNAME, "this is a reply to test comment 2",
                COMMENT_STATUS_PUBLISHED, true, false, true);

        // delete as administrator
        topicTab = topicTab.delete("test comment 2");
        // check result
        assertFalse(topicTab.hasComment("test comment 2"));
        assertFalse(topicTab.hasComment("Re:test comment 2"));

        logout();
    }

    private void verifyTopicNameAndModeration(WebElement element, String title, boolean moderated) {
        assertEquals(title, element.findElement(By.xpath("td[3]")).getText());
        assertEquals(moderated ? "Yes" : "No", element.findElement(By.xpath("td[8]")).getText()); // moderation = Yes/No
    }
}
