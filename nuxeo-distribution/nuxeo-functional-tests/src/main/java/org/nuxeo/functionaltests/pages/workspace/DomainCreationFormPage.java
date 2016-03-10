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
package org.nuxeo.functionaltests.pages.workspace;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class DomainCreationFormPage extends DocumentBasePage {

    @FindBy(id = "document_create:nxl_heading:nxw_title")
    WebElement titleTextInput;

    @FindBy(id = "document_create:nxl_heading:nxw_description")
    WebElement descriptionTextInput;

    @Required
    @FindBy(id = "document_create:nxw_documentCreateButtons_CREATE_DOCUMENT")
    public WebElement createButton;

    public DomainCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public void create() {
        createButton.click();
    }

    protected void fillDublinCoreFieldsAndCreate(String title, String description) {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);
        create();
    }

    public DocumentBasePage createDomain(String title, String description) {
        fillDublinCoreFieldsAndCreate(title, description);
        return asPage(DocumentBasePage.class);
    }

}
