/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class EditTabSubPage extends AbstractPage {

    public static final String MAJOR_VERSION_INCREMENT_VALUE = "ACTION_INCREMENT_MAJOR";

    public static final String MINOR_VERSION_INCREMENT_VALUE = "ACTION_INCREMENT_MINOR";

    @FindBy(id = "document_edit:nxl_heading:nxw_title")
    WebElement titleInputText;

    @FindBy(id = "document_edit:nxl_heading:nxw_description")
    WebElement descriptionInputText;

    @Required
    @FindBy(id = "document_edit:nxw_documentEditButtons_EDIT_CURRENT_DOCUMENT")
    WebElement save;

    public EditTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * By default
     *
     * @param title
     * @param description
     */
    public DocumentBasePage edit(String title, String description, String versionIncrementLabel) {

        if (title != null) {
            titleInputText.clear();
            titleInputText.sendKeys(title);
        }
        if (description != null) {
            descriptionInputText.clear();
            descriptionInputText.sendKeys(description);
        }
        if (versionIncrementLabel != null) {
            WebElement versionIncrementRadio = driver.findElement(By.xpath("//input[@value=\"" + versionIncrementLabel
                    + "\"]"));
            versionIncrementRadio.click();
        }

        save.click();

        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 8.2
     */
    public String getTitle() {
        return titleInputText.getAttribute("value");
    }

    /**
     * @since 8.2
     */
    public EditTabSubPage setTitle(String title) {
        titleInputText.clear();
        titleInputText.sendKeys(title);
        return this;
    }

    /**
     * @since 8.2
     */
    public String getDescription() {
        return descriptionInputText.getText();
    }

    /**
     * @since 8.2
     */
    public EditTabSubPage setDescription(String description) {
        descriptionInputText.clear();
        descriptionInputText.sendKeys(description);
        return this;
    }

    /**
     * Save the modifications.
     *
     * @since 5.7.3
     */
    public DocumentBasePage save() {
        save.click();
        return asPage(DocumentBasePage.class);
    }

}
