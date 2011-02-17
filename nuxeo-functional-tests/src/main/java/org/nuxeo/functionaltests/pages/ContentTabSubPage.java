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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * The content tab sub page. Most of the time available for folderish documents
 * and displaying the current document's children
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class ContentTabSubPage extends AbstractPage {

    @FindBy(id="document_content")
    WebElement documentContentForm;
    
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
        return PageFactory.initElements(driver, DocumentBasePage.class);
    }

    public <T> T getNewDocumentPage(String string, Class<T> class1) {
        return null;
    }

}
