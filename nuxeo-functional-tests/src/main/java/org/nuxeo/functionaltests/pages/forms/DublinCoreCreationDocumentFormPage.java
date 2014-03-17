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
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class DublinCoreCreationDocumentFormPage extends AbstractPage {


    @Required
    @FindBy(id = "document_create:nxl_heading:nxw_title")
    public WebElement titleTextInput;

    @Required
    @FindBy(id = "document_create:nxl_heading:nxw_description")
    public WebElement descriptionTextInput;

    @Required
    @FindBy(id = "document_create:nxw_documentCreateButtons_CREATE_DOCUMENT")
    public WebElement createButton;

    public DublinCoreCreationDocumentFormPage(WebDriver driver) {
        super(driver);
    }

   public void create() {
       createButton.click();
   }

   protected void fillDublinCoreFieldsAndCreate(String title,
           String description) {
       titleTextInput.sendKeys(title);
       descriptionTextInput.sendKeys(description);
       create();
   }

   public DocumentBasePage createDocument(String title,
           String description) {
       fillDublinCoreFieldsAndCreate(title, description);
       return asPage(DocumentBasePage.class);
   }


}
