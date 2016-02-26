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

package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.TopicCreationFormPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * @since 8.2
 */
public class ForumTabSubPage extends DocumentBasePage {

    @FindBy(xpath = "//a[@id='nxw_newForumThread_form:nxw_newForumThread']")
    public WebElement newTopicButtonLink;

    @Required
    @FindBy(id = "forum_content")
    WebElement forumContentForm;

    public ForumTabSubPage(WebDriver driver) {
        super(driver);
    }

    protected ContentViewElement getElement() {
        return AbstractTest.getWebFragment(By.id("cv_forum_content_0_panel"), ContentViewElement.class);
    }

    public List<WebElement> getChildTopicRows() {
        return getElement().getItems();
    }

    public TopicCreationFormPage getTopicCreatePage() {
        // Create a Topic
        newTopicButtonLink.click();
        return asPage(TopicCreationFormPage.class);
    }

    public TopicTabSubPage createTopic(String topicTitle, String topicDescription, Boolean moderated,
            String... usersOrGroups) {
        // Create a Topic
        return getTopicCreatePage().createTopicDocument(topicTitle, topicDescription, moderated, usersOrGroups);
    }

    public boolean hasTopicLink(String title) {
        try {
            WebElement element = forumContentForm.findElement(By.linkText(title));
            return element != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
