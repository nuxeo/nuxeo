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

package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.tabs.TopicTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class TopicCreationFormPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//input[@id='createThread:title']")
    public WebElement titleTextInput;

    @FindBy(xpath = "//textarea[@id='createThread:description']")
    public WebElement descriptionTextInput;

    @Required
    @FindBy(xpath = "//input[@id='createThread:j_idt586:0']")
    public WebElement moderationYesRadioButton;

    @Required
    @FindBy(xpath = "//input[@id='createThread:j_idt586:1']")
    public WebElement moderationNoRadioButton;

    @Required
    @FindBy(name = "createThread:j_idt611")
    public WebElement createButton;

    @Required
    @FindBy(name = "createThread:j_idt612")
    public WebElement cancelButton;

    public TopicCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public TopicTabSubPage createTopicDocument(String title, String description, Boolean moderation,
            String... usersOrGroups) {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);

        if (moderation) {
            moderationYesRadioButton.click();
        } else {
            moderationNoRadioButton.click();
        }

        if (usersOrGroups != null) {
            Select2WidgetElement selectUsersOrGroups = new Select2WidgetElement(
                    driver,
                    driver.findElement(By.xpath("//div[@id='s2id_createThread:nxl_user_group_prefixed_suggestion:nxw_selection_select2']")),
                    true);
            selectUsersOrGroups.selectValues(usersOrGroups);
        }

        createButton.click();
        return asPage(TopicTabSubPage.class);
    }
}
