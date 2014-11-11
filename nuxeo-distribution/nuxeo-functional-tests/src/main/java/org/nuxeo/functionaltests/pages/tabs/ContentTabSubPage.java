/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests.pages.tabs;

import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The content tab sub page. Most of the time available for folderish documents
 * and displaying the current document's children.
 */
public class ContentTabSubPage extends AbstractPage {

    @Required
    @FindBy(id = "document_content")
    WebElement documentContentForm;

    @FindBy(linkText = "New")
    WebElement newButton;

    @FindBy(xpath = "//input[@value=\"Delete\"]")
    WebElement deleteButton;

    public ContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Clicking on one of the child with the title.
     *
     * @param documentTitle
     * @return
     */
    public DocumentBasePage goToDocument(String documentTitle) {
        documentContentForm.findElement(By.linkText(documentTitle)).click();
        return asPage(DocumentBasePage.class);
    }

    /**
     * Clicks on the new button and select the type of document to create
     *
     * @param docType the document type to create
     * @param pageClassToProxy The page object type to return
     * @return The create form page object
     */
    public <T> T getDocumentCreatePage(String docType, Class<T> pageClassToProxy) {
        newButton.click();
        WebElement link = null;
        for (WebElement element : driver.findElements(By.className("documentType"))) {
            try {
                link = element.findElement(By.linkText(docType));
                break;
            } catch (NoSuchElementException e) {
                // next
            }

        }
        assertNotNull(link);
        link.click();
        return asPage(pageClassToProxy);

    }

    public DocumentBasePage removeDocument(String documentTitle) {
        // get all table item and if the link has the documents title, click
        // (enable) checkbox

        List<WebElement> trelements = documentContentForm.findElements(By.tagName("tr"));
        for (WebElement trItem : trelements) {
            try {
                trItem.findElement(By.linkText(documentTitle));
                WebElement checkBox = trItem.findElement(By.xpath("//input[@type=\"checkbox\"][@name=\"document_content:nxl_document_listing_ajax:nxw_listing_ajax_selection_box_with_current_document\"]"));
                checkBox.click();
                break;
            } catch (NoSuchElementException e) {
                // next
            }
        }

        waitUntilEnabled(deleteButton);
        deleteButton.click();
        driver.switchTo().alert().accept();

        return asPage(DocumentBasePage.class);
    }
}
