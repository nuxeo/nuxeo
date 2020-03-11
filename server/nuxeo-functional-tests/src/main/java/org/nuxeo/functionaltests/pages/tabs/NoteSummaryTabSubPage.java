/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The Nuxeo summary tab of a note document.
 *
 * @since 5.9.4
 */
public class NoteSummaryTabSubPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//div[@class=\"content_block\"]//td[@class=\"fieldColumn\"]")
    WebElement mainContentViewField;

    @FindBy(xpath = "//div[@class=\"textBlock note_content_block\"]")
    WebElement textBlockViewField;

    @FindBy(xpath = "//span[@class=\"versionNumber\"]")
    WebElement versionNumberField;

    /**
     * @param driver
     */
    public NoteSummaryTabSubPage(WebDriver driver) {
        super(driver);
    }

    public String getMainContentFileText() {
        return mainContentViewField.getText();
    }

    public String getTextBlockContentText() {
        return textBlockViewField.getText();
    }

    public WebElement getTextBlockViewField() {
        return textBlockViewField;
    }

    /**
     * @since 8.3
     */
    public String getVersionNumberText() {
        return versionNumberField.getText();
    }
}
