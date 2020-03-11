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
 *     Gabriel Barata
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.functionaltests.Constants.SECTION_TYPE;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.forms.DublinCoreCreationDocumentFormPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Section content tab.
 *
 * @since 8.3
 */
public class SectionContentTabSubPage extends AbstractContentTabSubPage {

    /**
     * @deprecated since 9.1 not used
     */
    @Deprecated
    @Required
    @FindBy(id = "section_content")
    WebElement contentForm;

    @Required
    @FindBy(id = "cv_section_content_0_panel")
    WebElement contentView;

    @FindBy(linkText = "New")
    WebElement newButton;

    public SectionContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected WebElement getContentViewElement() {
        return contentView;
    }

    public DublinCoreCreationDocumentFormPage getSectionCreatePage() {
        newButton.click();
        WebElement fancyBox = AbstractPage.getFancyBoxContent();
        // find the link to doc type that needs to be created
        WebElement link = fancyBox.findElement(By.linkText(SECTION_TYPE));
        assertNotNull(link);
        link.click();
        return asPage(DublinCoreCreationDocumentFormPage.class);
    }

    public SectionContentTabSubPage unpublishDocument(String documentTitle) {
        getContentView().selectByTitle(documentTitle).clickOnActionByTitle("Unpublish");
        return asPage(SectionContentTabSubPage.class);
    }

    protected ContentViewElement getElement() {
        return AbstractTest.getWebFragment(By.id("cv_section_content_0_panel"), ContentViewElement.class);
    }

    /**
     * @since 8.3
     */
    public boolean hasNewButton() {
        try {
            return newButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @since 8.3
     */
    public SectionContentTabSubPage refreshContent() {
        getContentView().getUpperActions().refreshContent();
        return asPage(SectionContentTabSubPage.class);
    }

}
