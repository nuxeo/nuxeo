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
 *     Thomas Roger
 *
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Representation of a Comments tab page.
 */
public class CommentsTabSubPage extends DocumentBasePage {

    @Required
    @FindBy(linkText = "Add a Comment")
    WebElement addComment;

    @FindBy(xpath = "//textarea")
    WebElement commentTextarea;

    @FindBy(xpath = "//input[@value='Add']")
    WebElement add;

    @FindBy(linkText = "Reply")
    WebElement replyLink;

    @FindBy(linkText = "Delete")
    WebElement deleteLink;

    public CommentsTabSubPage(WebDriver driver) {
        super(driver);
    }

    public CommentsTabSubPage addComment(String comment) {
        return addComment(comment, false);
    }

    public CommentsTabSubPage addComment(String comment, boolean reply) {
        if (reply) {
            replyLink.click();
        } else {
            addComment.click();
        }
        commentTextarea.clear();
        commentTextarea.sendKeys(comment);
        add.click();
        return asPage(CommentsTabSubPage.class);
    }

    public boolean hasComment(String comment) {
        List<WebElement> elements = driver.findElements(By.xpath("//div[contains(@class, 'commentQuote')]"));
        for (WebElement element : elements) {
            if (element.getText().trim().equals(comment)) {
                return true;
            }
        }
        return false;
    }

    public CommentsTabSubPage reply(String reply) {
        return addComment(reply, true);
    }

    /**
     * Deletes first comment.
     */
    public CommentsTabSubPage delete() {
        deleteLink.click();
        return asPage(CommentsTabSubPage.class);
    }

    public boolean canDelete() {
        try {
            return deleteLink.isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
