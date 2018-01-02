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
 *     Yannis JULIENNE
 */

package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.3
 */
public class TopicTabSubPage extends DocumentBasePage {

    private static final String COMMENT_XPATH_BASE = ".//div[@class='commentAuthor' and starts-with(text(),'%s')]/..";

    public static final String COMMENT_STATUS_PUBLISHED = "Published";

    public static final String COMMENT_STATUS_WAITING_APPROVAL = "Waiting for approval";

    public static final String COMMENT_STATUS_REJECTED = "Rejected";

    @Required
    @FindBy(xpath = "//ul[contains(@class,'commentsOutput')]")
    public WebElement commentsList;

    @FindBy(xpath = "//input[contains(@id,'post_title')]")
    public WebElement titleInput;

    @FindBy(xpath = "//textarea[contains(@id,'post_description')]")
    public WebElement descriptionInput;

    @FindBy(xpath = "//input[@type='submit' and @value='Add']")
    public WebElement addButton;

    @FindBy(xpath = "//input[@type='submit' and @value='Cancel']")
    public WebElement cancelButton;

    public TopicTabSubPage(WebDriver driver) {
        super(driver);
    }

    public void checkComment(String title, String author, String description, String status, boolean canReply,
            boolean canApproveOrReject, boolean canDelete) {
        WebElement comment = getComment(title);
        checkAuthor(comment, author);
        checkDescription(comment, description);
        checkStatus(comment, status);
        assertEquals(canReply, hasReplyLink(comment));
        assertEquals(canApproveOrReject, hasApproveLink(comment) && hasRejectLink(comment));
        assertEquals(canDelete, hasDeleteLink(comment));
    }

    public boolean hasComment(String title) {
        try {
            return getComment(title) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isCommentFormDisplayed() {
        try {
            return titleInput.isDisplayed() && descriptionInput.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public TopicTabSubPage showCommentForm() {
        if (!isCommentFormDisplayed()) {
            addButton.click();
            return asPage(TopicTabSubPage.class);
        }
        return this;
    }

    public TopicTabSubPage addComment(String title, String description) {
        if (StringUtils.isNotBlank(title)) {
            titleInput.sendKeys(title);
        }
        if (description != null) {
            descriptionInput.sendKeys(description);
        }
        addButton.click();
        return asPage(TopicTabSubPage.class);
    }

    public TopicTabSubPage reply(String title, String description) {
        getReplyLink(getComment(title)).click();
        TopicTabSubPage page = asPage(TopicTabSubPage.class);
        return page.addComment(null, description);
    }

    public TopicTabSubPage delete(String title) {
        getDeleteLink(getComment(title)).click();
        return asPage(TopicTabSubPage.class);
    }

    public TopicTabSubPage reject(String title) {
        getRejectLink(getComment(title)).click();
        return asPage(TopicTabSubPage.class);
    }

    public TopicTabSubPage approve(String title) {
        getApproveLink(getComment(title)).click();
        return asPage(TopicTabSubPage.class);
    }

    public WebElement getComment(String title) {
        String xpath = String.format(COMMENT_XPATH_BASE, title);
        return commentsList.findElement(By.xpath(xpath));
    }

    private WebElement getReplyLink(WebElement comment) {
        return comment.findElement(By.linkText("Reply"));
    }

    private WebElement getApproveLink(WebElement comment) {
        return comment.findElement(By.linkText("Approve"));
    }

    private WebElement getRejectLink(WebElement comment) {
        return comment.findElement(By.linkText("Reject"));
    }

    private WebElement getDeleteLink(WebElement comment) {
        return comment.findElement(By.linkText("Delete"));
    }

    private boolean hasReplyLink(WebElement comment) {
        try {
            return getReplyLink(comment) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean hasApproveLink(WebElement comment) {
        try {
            return getApproveLink(comment) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean hasRejectLink(WebElement comment) {
        try {
            return getRejectLink(comment) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean hasDeleteLink(WebElement comment) {
        try {
            return getDeleteLink(comment) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private void checkAuthor(WebElement comment, String author) {
        assertTrue(comment.findElement(By.className("commentAuthor")).getText().contains("by " + author));
    }

    private void checkDescription(WebElement comment, String description) {
        assertTrue(comment.findElement(By.className("commentQuote")).getText().equals(description));
    }

    private void checkStatus(WebElement comment, String status) {
        assertTrue(comment.findElement(By.className("commentCreationDate")).getText().contains(status));
    }

}
