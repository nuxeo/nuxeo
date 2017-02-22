/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * All resolutions of sub element are made based on contentForm id.
 *
 * @since 9.1
 */
public abstract class AbstractContentTabSubPage extends DocumentBasePage {

    /**
     * @deprecated since 9.1 use filter methods on {@link ContentViewElement}
     */
    @Deprecated
    @FindBy(id = "cv_document_content_0_quickFilterForm:nxl_document_content_filter:nxw_search_title")
    WebElement filterInput;

    /**
     * @deprecated since 9.1 use filter methods on {@link ContentViewElement}
     */
    @Deprecated
    @FindBy(id = "cv_document_content_0_quickFilterForm:submitFilter")
    WebElement filterButton;

    /**
     * @deprecated since 9.1 use filter methods on {@link ContentViewElement}
     */
    @Deprecated
    @FindBy(id = "cv_document_content_0_resetFilterForm:resetFilter")
    WebElement clearFilterButton;

    public AbstractContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * @return the content view element for this content view tab
     */
    protected abstract WebElement getContentViewElement();

    public ContentViewElement getContentView() {
        return AbstractTest.getWebFragment(getContentViewElement(), ContentViewElement.class);
    }

    public List<WebElement> getChildDocumentRows() {
        return getContentView().getItems();
    }

    public boolean hasDocumentLink(String title) {
        try {
            WebElement element = getContentView().findElement(By.linkText(title));
            return element != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Clicking on one of the child with the title.
     *
     * @param documentTitle the document title
     */
    public DocumentBasePage goToDocument(String documentTitle) {
        getContentView().clickOnItemTitle(documentTitle);
        return asPage(DocumentBasePage.class);
    }

    public DocumentBasePage goToDocumentWithVersion(String title, String version) {
        getContentView().clickOnItemTitleAndVersion(title, version);
        return asPage(DocumentBasePage.class);
    }

    public DocumentBasePage goToDocument(int index) {
        getContentView().clickOnItemIndex(index);
        return asPage(DocumentBasePage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     */
    public DocumentBasePage filterDocument(String filter) {
        return filterDocument(filter, DocumentBasePage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     */
    public <T extends DocumentBasePage> T filterDocument(String filter, Class<T> pageClassToProxy) {
        getContentView().filterDocument(filter);
        return asPage(pageClassToProxy);
    }

    /**
     * Clear the current filter and refresh content view.
     */
    public DocumentBasePage clearFilter() {
        return clearFilter(DocumentBasePage.class);
    }

    /**
     * Clear the current filter and refresh content view.
     */
    public <T extends DocumentBasePage> T clearFilter(Class<T> pageClassToProxy) {
        getContentView().clearFilter();
        return asPage(pageClassToProxy);
    }

    public DocumentBasePage removeDocument(String documentTitle) {
        return getContentView().selectByTitle(documentTitle).delete();
    }

    public <T extends DocumentBasePage> T removeDocument(String documentTitle, Class<T> pageClassToProxy) {
        return getContentView().selectByTitle(documentTitle).delete(pageClassToProxy);
    }

    /**
     * Removes all documents visible on current page.
     */
    public DocumentBasePage removeAllDocuments() {
        return removeAllDocuments(DocumentBasePage.class);
    }
    @SuppressWarnings("unchecked")
    public <T extends DocumentBasePage> T removeAllDocuments(Class<T> pageClassToProxy) {
        ContentViewElement cv = getContentView();
        if (cv.getItems().size() == 0) {
            // no document to remove
            if (pageClassToProxy.isInstance(this)) {
                return (T) this;
            }
            return null;
        }
        return getContentView().selectAll().delete(pageClassToProxy);
    }

}
