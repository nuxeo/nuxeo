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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.forms.DublinCoreCreationDocumentFormPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class SectionsContentTabSubPage extends ContentTabSubPage {

    @Required
    @FindBy(id = "nxw_newSection_form:nxw_newSection")
    WebElement createNewSectionLink;

    public SectionsContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    public DublinCoreCreationDocumentFormPage getSectionCreatePage() {
        createNewSectionLink.click();
        return asPage(DublinCoreCreationDocumentFormPage.class);
    }

}
