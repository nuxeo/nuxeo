/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The content tab sub page. Most of the time available for folderish documents and displaying the current document's
 * children.
 */
public class ContentTabSubPage extends DocumentBasePage {

    /**
     * @since 8.3
     */
    public static final String COPY = "Copy";

    /**
     * @since 8.3
     */
    public static final String PASTE = "Paste";

    /**
     * @since 8.3
     */
    public static final String DELETE = "Delete";

    /**
     * @since 8.3
     */
    public static final String ADD_TO_WORKLIST = "Add to Worklist";

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

    public ContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * @since 9.1
     */
    public ContentViewElement getContentViewElement() {
        return AbstractTest.getWebFragment(By.id("cv_document_content_0_panel"), ContentViewElement.class);
    }

    public List<WebElement> getChildDocumentRows() {
        return getContentViewElement().getItems();
    }

    /**
     * Clicking on one of the child with the title.
     *
     * @param documentTitle the document title
     */
    public DocumentBasePage goToDocument(String documentTitle) {
        getContentViewElement().clickOnItemTitle(documentTitle);
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
        waitUntilEnabledAndClick(newButton);
        WebElement fancyBox = AbstractPage.getFancyBoxContent();
        // find the link to doc type that needs to be created
        WebElement link = fancyBox.findElement(By.linkText(docType));
        assertNotNull(link);
        link.click();
        return asPage(pageClassToProxy);
    }

    public DocumentBasePage removeDocument(String documentTitle) {
        getContentViewElement().checkByTitle(documentTitle);
        deleteSelectedDocuments();
        return asPage(DocumentBasePage.class);
    }

    protected void deleteSelectedDocuments() {
        waitUntilEnabledAndClick(getContentViewElement().getSelectionActionByTitle(DELETE));
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete selected document(s)?", alert.getText());
        alert.accept();
    }

    public DocumentBasePage addToWorkList(String documentTitle) {
        getContentViewElement().checkByTitle(documentTitle);
        waitUntilEnabledAndClick(getContentViewElement().getSelectionActionByTitle(ADD_TO_WORKLIST));
        return asPage(DocumentBasePage.class);
    }

    /**
     * Removes all documents visible on current page.
     */
    public ContentTabSubPage removeAllDocuments() {
        ContentViewElement cv = getContentViewElement();
        if (cv.getItems().size() == 0) {
            // no document to remove
            return this;
        }
        cv.checkAllItems();
        deleteSelectedDocuments();
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     * @since 5.7.2
     * @deprecated since 9.1 use {@link ContentTabSubPage#filterDocument(String)} instead and assert in your
     * test the expected number of results.
     */
    @Deprecated
    public ContentTabSubPage filterDocument(final String filter, final int expectedNbOfDisplayedResult,
            final int timeout) {
        filterDocument(filter);
        assertEquals(expectedNbOfDisplayedResult, getChildDocumentRows().size());
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     * @since 9.1
     */
    public ContentTabSubPage filterDocument(final String filter) {
        filterInput.clear();
        filterInput.sendKeys(filter);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        filterButton.click();
        arm.end();
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Reset the filter.
     *
     * @since 5.7.2
     */
    public ContentTabSubPage clearFilter(final int expectedNbOfDisplayedResult, final int timeout) {
        Locator.waitUntilEnabledAndClick(clearFilterButton);
        Locator.waitUntilGivenFunction(driver -> {
            try {
                return getChildDocumentRows().size() == expectedNbOfDisplayedResult;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                return false;
            }
        });
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their index in the content view.
     *
     * @since 8.1
     */
    public ContentTabSubPage selectByIndex(int... indexes) {
        getContentViewElement().checkByIndex(indexes);
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their title in the content view.
     *
     * @since 5.7.8
     * @deprecated since 8.1 use {@link #selectByTitle(String...)}
     */
    @Deprecated
    public ContentTabSubPage selectDocumentByTitles(String... titles) {
        return selectByTitle(titles);
    }

    /**
     * Selects documents by title in the content view.
     *
     * @since 8.1
     */
    public ContentTabSubPage selectByTitle(String... titles) {
        getContentViewElement().checkByTitle(titles);
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their index in the content view and copy them in the clipboard.
     *
     * @param indexes the indexes
     * @since 5.7.8
     */
    public ContentTabSubPage copyByIndex(int... indexes) {
        getContentViewElement().checkByIndex(indexes);
        getContentViewElement().getSelectionActionByTitle(COPY).click();
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their title in the content view and copy them in the clipboard.
     *
     * @param titles the titles
     * @since 5.7.8
     */
    public ContentTabSubPage copyByTitle(String... titles) {
        getContentViewElement().checkByTitle(titles);
        getContentViewElement().getSelectionActionByTitle(COPY).click();
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Pastes the content of the clip board.
     *
     * @since 5.7.8
     */
    public ContentTabSubPage paste() {
        getContentViewElement().getSelectionActionByTitle(PASTE).click();
        return asPage(ContentTabSubPage.class);
    }

    /**
     * @since 5.9.3
     */
    public DocumentBasePage goToDocument(final int index) {
        waitUntilEnabledAndClick(getChildDocumentRows().get(index).findElement(By.xpath("td[3]/div/a[1]")));
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 8.3
     */
    public boolean hasDocumentLink(String title) {
        try {
            WebElement element = documentContentForm.findElement(By.linkText(title));
            return element != null;
        } catch (NoSuchElementException e) {
            return false;
        }
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
}
