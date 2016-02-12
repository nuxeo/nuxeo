/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;

/**
 * The content tab sub page. Most of the time available for folderish documents and displaying the current document's
 * children.
 */
public class ContentTabSubPage extends DocumentBasePage {

    private static final String COPY_BUTTON_XPATH = "//input[@value=\"Copy\"]";

    private static final String PASTE_BUTTON_XPATH = "//input[@value=\"Paste\"]";

    private static final String DELETE_BUTTON_XPATH = "//input[@value=\"Delete\"]";

    private static final String ADD_TO_WORKLIST_BUTTON_XPATH = "//input[@value=\"Add to Worklist\"]";

    private static final String SELECT_ALL_BUTTON_XPATH = "//input[@type=\"checkbox\" and @title=\"Select all / deselect all\"]";

    private static final String CHECK_BOX_XPATH = "td/input[@type=\"checkbox\"]";

    private static final String DOCUMENT_TITLE_XPATH = "td//span[@id[starts-with(.,'title_')]]";

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

        List<WebElement> trelements = documentContentForm.findElement(By.tagName("tbody")).findElements(
                By.tagName("tr"));
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
        deleteSelectedDocuments();

        return asPage(DocumentBasePage.class);
    }

    protected void deleteSelectedDocuments() {
        findElementWaitUntilEnabledAndClick(By.xpath(DELETE_BUTTON_XPATH));
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete selected document(s)?", alert.getText());
        alert.accept();
    }

    public DocumentBasePage addToWorkList(String documentTitle) {
        // get all table item and if the link has the documents title, click
        // (enable) checkbox

        selectByTitle(documentTitle);
        findElementWaitUntilEnabledAndClick(By.xpath(ADD_TO_WORKLIST_BUTTON_XPATH));

        return asPage(DocumentBasePage.class);
    }

    /**
     * Removes all documents visible on current page.
     */
    public ContentTabSubPage removeAllDocuments() {
        ContentTabSubPage page = asPage(ContentTabSubPage.class);
        By locator = By.xpath(SELECT_ALL_BUTTON_XPATH);
        if (!hasElement(locator)) {
            // no document to remove
            return page;
        }
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        findElementWaitUntilEnabledAndClick(By.xpath(SELECT_ALL_BUTTON_XPATH));
        arm.end();
        deleteSelectedDocuments();

        return asPage(ContentTabSubPage.class);
    }

    /**
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     * @param expectedNbOfDisplayedResult
     * @since 5.7.2
     */
    public ContentTabSubPage filterDocument(final String filter, final int expectedNbOfDisplayedResult,
            final int timeout) {
        filterInput.clear();
        filterInput.sendKeys(filter);
        filterButton.click();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return getChildDocumentRows().size() == expectedNbOfDisplayedResult;
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
        });
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Reset the filter.
     *
     * @param expectedNbOfDisplayedResult
     * @param timeout
     * @since 5.7.2
     */
    public ContentTabSubPage clearFilter(final int expectedNbOfDisplayedResult, final int timeout) {
        clearFilterButton.click();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return getChildDocumentRows().size() == expectedNbOfDisplayedResult;
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
        });
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their index in the content view.
     *
     * @since 5.7.8
     * @deprecated since 8.1, use {@link #selectByIndex(int...)} instead.
     */
    @Deprecated
    public ContentTabSubPage selectDocumentByIndex(int... indexes) {
        return selectByIndex(indexes);
    }

    /**
     * Selects documents by their index in the content view.
     *
     * @since 8.1
     */
    public ContentTabSubPage selectByIndex(int... indexes) {
        AjaxRequestManager a = new AjaxRequestManager(driver);
        for (int i : indexes) {
            a.watchAjaxRequests();
            getChildDocumentRows().get(i).findElement(By.xpath(CHECK_BOX_XPATH)).click();
            a.waitForAjaxRequests();
        }
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
        return selectByIndex(convertToIndexes(titles));
    }

    protected int[] convertToIndexes(String... titles) {
        List<String> titleList = Arrays.asList(titles);
        List<Integer> temp = new ArrayList<Integer>();
        int index = 0;
        for (WebElement row : childDocumentRows) {
            String docTitle = row.findElement(By.xpath(DOCUMENT_TITLE_XPATH)).getText();
            if (docTitle != null && titleList.contains(docTitle)) {
                temp.add(index);
            }
            index++;
        }
        int[] result = new int[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            result[i] = temp.get(i);
        }
        return result;
    }

    /**
     * Selects documents by their index in the content view and copy them in the clipboard.
     *
     * @param indexes
     * @since 5.7.8
     */
    public ContentTabSubPage copyByIndex(int... indexes) {
        selectByIndex(indexes);
        findElementWaitUntilEnabledAndClick(By.xpath(COPY_BUTTON_XPATH));
        return asPage(ContentTabSubPage.class);
    }

    /**
     * Selects documents by their title in the content view and copy them in the clipboard.
     *
     * @param indexes
     * @since 5.7.8
     */
    public ContentTabSubPage copyByTitle(String... titles) {
        return copyByIndex(convertToIndexes(titles));
    }

    /**
     * Pastes the content of the clip board.
     *
     * @since 5.7.8
     */
    public ContentTabSubPage paste() {
        findElementWaitUntilEnabledAndClick(By.xpath(PASTE_BUTTON_XPATH));
        return asPage(ContentTabSubPage.class);
    }

    /**
     * @since 5.9.3
     */
    public DocumentBasePage goToDocument(final int index) {
        getChildDocumentRows().get(index).findElement(By.xpath("td[3]/div/a[1]")).click();
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 8.2
     */
    public boolean hasDocumentLink(String title) {
        try {
            WebElement element = documentContentForm.findElement(By.linkText(title));
            return element != null;
        } catch(NoSuchElementException e) {
            return false;
        }
    }
}
