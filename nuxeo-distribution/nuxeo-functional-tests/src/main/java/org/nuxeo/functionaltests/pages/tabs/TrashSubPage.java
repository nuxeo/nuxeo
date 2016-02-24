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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 *     <a href="mailto:gbarata@nuxeo.com">Gabriel</a>
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class TrashSubPage extends AbstractPage {

    protected static final String SELECT_ALL_BUTTON_ID = "document_trash_content:nxl_document_listing_table:listing_table_selection_box_with_current_document_header";

    protected static final String PERMANENT_DELETE_BUTTON_ID = "document_trash_content_buttons:nxw_CURRENT_SELECTION_DELETE_form:nxw_CURRENT_SELECTION_DELETE";

    protected static final String RESTORE_BUTTON_ID = "document_trash_content_buttons:nxw_CURRENT_SELECTION_UNDELETE_form:nxw_CURRENT_SELECTION_UNDELETE";

    protected static final String EMPTY_TRASH_BUTTON_ID = "document_trash_content_buttons:nxw_CURRENT_SELECTION_EMPTY_TRASH_form:nxw_CURRENT_SELECTION_EMPTY_TRASH";

    @FindBy(xpath = "//form[@id=\"document_trash_content\"]//tbody//tr")
    List<WebElement> childDocumentRows;

    public List<WebElement> getChildDocumentRows() {
        return childDocumentRows;
    }

    @Required
    @FindBy(id = "cv_document_trash_content_0_resultsPanel")
    protected WebElement documentContentForm;

    public TrashSubPage(WebDriver driver) {
        super(driver);
    }

    public TrashSubPage emptyTrash() {
        findElementWaitUntilEnabledAndClick(By.id(EMPTY_TRASH_BUTTON_ID));
        Alert alert = driver.switchTo().alert();
        assertEquals("Permanently delete all documents in trash?", alert.getText());
        alert.accept();
        return asPage(TrashSubPage.class);
    }

    /**
     * Removes all documents visible on current page.
     */
    public TrashSubPage purgeAllDocuments() {
        TrashSubPage page = asPage(TrashSubPage.class);
        By locator = By.id(SELECT_ALL_BUTTON_ID);
        if (!hasElement(locator)) {
            // no documents to remove
            return page;
        }
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        findElementWaitUntilEnabledAndClick(By.id(SELECT_ALL_BUTTON_ID));
        arm.end();

        deleteSelectedDocuments();

        return asPage(TrashSubPage.class);
    }

    /**
     * @since 8.2
     */
    public TrashSubPage goToDocument(final int index) {
        getChildDocumentRows().get(index).findElement(By.xpath("td[3]/div/a[1]")).click();
        return asPage(TrashSubPage.class);
    }

    /**
     * @since 8.2
     */
    public TrashSubPage goToDocument(String documentTitle) {
        getElement().clickOnItemTitle(documentTitle);
        return asPage(TrashSubPage.class);
    }

    /**
     * @since 8.2
     */
    public TrashSubPage selectByTitle(String... titles) {
        getElement().checkByTitle(titles);
        return asPage(TrashSubPage.class);
    }

    /**
     * @since 8.2
     */
    public DocumentBasePage restoreDocument(String... titles) {
        getElement().checkByTitle(titles);
        restoreSelectedDocuments();
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 8.2
     */
    public TrashSubPage purgeDocument(String... titles) {
        getElement().checkByTitle(titles);
        deleteSelectedDocuments();
        return asPage(TrashSubPage.class);
    }

    /**
     * @since 8.2
     */
    protected ContentViewElement getElement() {
        return AbstractTest.getWebFragment(By.id("cv_document_trash_content_0_panel"), ContentViewElement.class);
    }

    /**
     * @since 8.2
     */
    protected void restoreSelectedDocuments() {
        findElementWaitUntilEnabledAndClick(By.id(RESTORE_BUTTON_ID));
        Alert alert = driver.switchTo().alert();
        assertEquals("Undelete selected document(s)?", alert.getText());
        alert.accept();
    }

    protected void deleteSelectedDocuments() {
        findElementWaitUntilEnabledAndClick(By.id(PERMANENT_DELETE_BUTTON_ID));
        Alert alert = driver.switchTo().alert();
        assertEquals("Permanently delete selected document(s)?", alert.getText());
        alert.accept();
    }

}
