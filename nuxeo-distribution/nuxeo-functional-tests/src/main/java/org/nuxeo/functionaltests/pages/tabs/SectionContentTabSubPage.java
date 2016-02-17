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

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.DublinCoreCreationDocumentFormPage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.nuxeo.functionaltests.Constants.SECTION_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Section content tab.
 *
 * @since 8.2
 */
public class SectionContentTabSubPage extends DocumentBasePage {

    @Required
    @FindBy(id = "section_content")
    WebElement contentForm;

    @FindBy(linkText = "New")
    WebElement newButton;

    public SectionContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    public ContentViewElement getContentView() {
        return AbstractTest.getWebFragment(By.id("cv_section_content_0_panel"), ContentViewElement.class);
    }

    public DublinCoreCreationDocumentFormPage getSectionCreatePage() {
        newButton.click();
        WebElement fancyBox = getFancyBoxContent();
        // find the link to doc type that needs to be created
        WebElement link = fancyBox.findElement(By.linkText(SECTION_TYPE));
        assertNotNull(link);
        link.click();
        return asPage(DublinCoreCreationDocumentFormPage.class);
    }

    public DocumentBasePage removeDocument(String documentTitle) {
        getContentView().checkByTitle(documentTitle).getSelectionActionByTitle(ContentTabSubPage.DELETE).click();
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete selected document(s)?", alert.getText());
        alert.accept();
        return asPage(DocumentBasePage.class);
    }

}
