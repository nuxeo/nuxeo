/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class TrashSubPage extends AbstractPage {

    private static final String SELECT_ALL_BUTTON_ID = "document_trash_content:nxl_document_listing_table:listing_table_selection_box_with_current_document_header";

    private static final String PERMANENT_DELETE_BUTTON_ID= "document_trash_content_buttons:nxw_CURRENT_SELECTION_DELETE_form:nxw_CURRENT_SELECTION_DELETE";

    @Required
    @FindBy(id = "cv_document_trash_content_0_resultsPanel")
    protected WebElement documentContentForm;

    public TrashSubPage(WebDriver driver) {
        super(driver);
    }

    public TrashSubPage purgeAllDocuments() {
        TrashSubPage page = asPage(TrashSubPage.class);
        By locator = By.id(SELECT_ALL_BUTTON_ID);
        if (!hasElement(locator)) {
            // no document to remove
            return page;
        }
        findElementWaitUntilEnabledAndClick(By.id(SELECT_ALL_BUTTON_ID));
        deleteSelectedDocuments();

        try {
            documentContentForm.findElement(By.tagName("tbody"));
        } catch (NoSuchElementException e) {
            // no more document to remove
            return page;
        }
        return purgeAllDocuments();
    }

    protected void deleteSelectedDocuments() {
        findElementWaitUntilEnabledAndClick(By.id(PERMANENT_DELETE_BUTTON_ID));
        Alert alert = driver.switchTo().alert();
        assertEquals("Permanently delete selected document(s)?", alert.getText());
        alert.accept();
    }

}
