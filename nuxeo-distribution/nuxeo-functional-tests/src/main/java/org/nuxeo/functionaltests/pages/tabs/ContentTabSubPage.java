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

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.contentView.ContentViewSelectionActions;
import org.nuxeo.functionaltests.fragment.AddAllToCollectionForm;
import org.nuxeo.functionaltests.pages.AbstractPage;
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
public class ContentTabSubPage extends AbstractContentTabSubPage {

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
     * @deprecated since 9.1 not used anymore as we now have {@link ContentViewSelectionActions#delete()}
     */
    public static final String DELETE = "Delete";

    /**
     * @since 8.3
     */
    public static final String ADD_TO_WORKLIST = "Add to Worklist";

    /**
     * @since 9.1
     */
    public static final String ADD_TO_COLLECTION = "Add to collection";

    @Required
    @FindBy(id = "document_content")
    WebElement documentContentForm;

    @Required
    @FindBy(id = "cv_document_content_0_panel")
    WebElement contentView;

    @FindBy(linkText = "New")
    WebElement newButton;

    public ContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected WebElement getContentViewElement() {
        return contentView;
    }

    /**
     * @since 9.1 use {@link #getContentView()} instead.
     */
    @Deprecated
    protected ContentViewElement getElement() {
        return getContentView();
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

    /**
     * @deprecated since 9.1 no need - use {@link ContentViewElement#selectByTitle(String...)} then the action instead
     */
    @Deprecated
    protected void deleteSelectedDocuments() {
        getContentView().getSelectionActions().delete();
    }

    public ContentTabSubPage addToWorkList(String documentTitle) {
        return getContentView().selectByTitle(documentTitle).clickOnActionByTitle(ADD_TO_WORKLIST,
                ContentTabSubPage.class);
    }

    /**
     * Removes all documents visible on current page.
     */
    @Override
    public ContentTabSubPage removeAllDocuments() {
        return removeAllDocuments(ContentTabSubPage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     * @since 5.7.2
     * @deprecated since 9.1 use {@link ContentTabSubPage#filterDocument(String)} instead and assert in your test the
     *             expected number of results.
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
    @Override
    public ContentTabSubPage filterDocument(final String filter) {
        return filterDocument(filter, ContentTabSubPage.class);
    }

    @Override
    public ContentTabSubPage clearFilter() {
        return clearFilter(ContentTabSubPage.class);
    }

    /**
     * Reset the filter.
     *
     * @since 5.7.2
     * @deprecated since 9.1 use {@link #clearFilter()} instead and assert the expected number of result in your test
     */
    @Deprecated
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
        getContentView().selectByIndex(indexes);
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
        getContentView().selectByTitle(titles);
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their index in the content view and copy them in the clipboard.
     *
     * @param indexes the indexes
     * @since 5.7.8
     */
    public ContentTabSubPage copyByIndex(int... indexes) {
        return getContentView().selectByIndex(indexes).clickOnActionByTitle(COPY, ContentTabSubPage.class);
    }

    /**
     * Selects documents by their title in the content view and copy them in the clipboard.
     *
     * @param titles the titles
     * @since 5.7.8
     */
    public ContentTabSubPage copyByTitle(String... titles) {
        return getContentView().selectByTitle(titles).clickOnActionByTitle(COPY, ContentTabSubPage.class);
    }

    /**
     * Pastes the content of the clip board.
     *
     * @since 5.7.8
     */
    public ContentTabSubPage paste() {
        return getContentView().getSelectionActions().clickOnActionByTitle(PASTE, ContentTabSubPage.class);
    }

    public AddAllToCollectionForm addToCollectionByIndex(int... indexes) {
        getContentView().selectByIndex(indexes).clickOnActionByTitle(ADD_TO_COLLECTION);
        WebElement elt = AbstractPage.getFancyBoxContent();
        return getWebFragment(elt, AddAllToCollectionForm.class);
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
