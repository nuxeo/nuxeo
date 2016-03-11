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
 *     Kevin Leturc
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.pages.FolderDocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class FolderEditTabSubPage extends EditTabSubPage {

    @FindBy(id = "document_edit:nxl_dublincore:nxw_language")
    WebElement languageInputText;

    public FolderEditTabSubPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public FolderEditTabSubPage setTitle(String title) {
        super.setTitle(title);
        return this;
    }
    @Override
    public FolderEditTabSubPage setDescription(String description) {
        super.setDescription(description);
        return this;
    }

    public String getLanguage() {
        return languageInputText.getAttribute("value");
    }

    public FolderEditTabSubPage setLanguage(String language) {
        languageInputText.clear();
        languageInputText.sendKeys(language);
        return this;
    }

    @Override
    public FolderDocumentBasePage save() {
        return super.save().asPage(FolderDocumentBasePage.class);
    }

}
