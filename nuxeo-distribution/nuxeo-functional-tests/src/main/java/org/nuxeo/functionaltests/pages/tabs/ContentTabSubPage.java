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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Clock;
import org.openqa.selenium.support.ui.SystemClock;

/**
 * The content tab sub page. Most of the time available for folderish documents
 * and displaying the current document's children.
 */
public class ContentTabSubPage extends DocumentBasePage {

    private static final Log log = LogFactory.getLog(ContentTabSubPage.class);

    private static final String DELETE_BUTTON_XPATH = "//input[@value=\"Delete\"]";

    @Required
    @FindBy(id = "document_content")
    WebElement documentContentForm;

    @FindBy(linkText = "New")
    WebElement newButton;

    @FindBy(id = "cv_document_content_0_quickFilterForm:nxl_document_content_filter:nxw_search_title")
    WebElement filterInput;

    @FindBy(id = "cv_document_content_0_quickFilterForm:submitFilter")
    WebElement filterButton;

    @FindBy(id = "cv_document_content_0_resetFilterForm:resetFilter")
    WebElement clearFilterButton;

    @FindBy(xpath = "//form[@id=\"document_content\"]//tbody//tr")
    List<WebElement> childDocumentRows;

    public List<WebElement> getChildDocumentRows() {
        return childDocumentRows;
    }

    public ContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Clicking on one of the child with the title.
     *
     * @param documentTitle
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
        WebElement fancyBox = getFancyBoxContent();
        // find the link to doc type that needs to be created
        WebElement link = fancyBox.findElement(By.linkText(docType));
        assertNotNull(link);
        link.click();
        return asPage(pageClassToProxy);
    }

    public DocumentBasePage removeDocument(String documentTitle) {
        // get all table item and if the link has the documents title, click
        // (enable) checkbox

        List<WebElement> trelements = documentContentForm.findElement(
                By.tagName("tbody")).findElements(By.tagName("tr"));
        for (WebElement trItem : trelements) {
            try {
                trItem.findElement(By.linkText(documentTitle));
                WebElement checkBox = trItem.findElement(By.xpath("td/input[@type=\"checkbox\"]"));
                checkBox.click();
                break;
            } catch (NoSuchElementException e) {
                // next
            }
        }

        findElementWaitUntilEnabledAndClick(By.xpath(DELETE_BUTTON_XPATH));
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete selected document(s)?", alert.getText());
        // Trying Thread.sleep on failing alert.accept on some machines:
        // org.openqa.selenium.WebDriverException:
        // a.document.getElementsByTagName("dialog")[0] is undefined
        try {
            Thread.sleep(AbstractTest.LOAD_SHORT_TIMEOUT_SECONDS * 1000);
        } catch (InterruptedException ie) {
            // ignore
        }
        alert.accept();
        return asPage(DocumentBasePage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     * @param expectedDisplayedElt
     * @param timeout
     *
     * @since 5.7.2
     */
    public void filterDocument(final String filter,
            final int expectedNbOfDisplayedResult, final int timeout) {
        filterInput.clear();
        filterInput.sendKeys(filter);
        filterButton.click();
        Clock clock = new SystemClock();
        long end = clock.laterBy(timeout);
        while (clock.isNowBefore(end)) {
            try {
                if (getChildDocumentRows().size() == expectedNbOfDisplayedResult) {
                    return;
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            } catch (NoSuchElementException ex) {
                // ignore
            }
        }

    }

    /**
     * Reset the filter.
     *
     * @param expectedNbOfDisplayedResult
     * @param timeout
     *
     * @since 5.7.2
     */
    public void clearFilter(final int expectedNbOfDisplayedResult,
            final int timeout) {
        clearFilterButton.click();
        Clock clock = new SystemClock();
        long end = clock.laterBy(timeout);
        while (clock.isNowBefore(end)) {
            try {
                if (getChildDocumentRows().size() == expectedNbOfDisplayedResult) {
                    return;
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            } catch (NoSuchElementException ex) {
                // ignore
            }
        }
    }
}
