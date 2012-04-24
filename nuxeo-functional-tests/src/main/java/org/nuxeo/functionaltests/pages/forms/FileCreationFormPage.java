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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class FileCreationFormPage extends AbstractPage {

    @FindBy(id = "document_create:nxl_heading:nxw_title")
    public WebElement titleTextInput;

    @FindBy(id = "document_create:nxl_heading:nxw_description")
    public WebElement descriptionTextInput;

    @FindBy(id = "document_create:create_doc_CREATE_DOCUMENT")
    public WebElement createButton;

    @FindBy(id = "document_create:nxl_file:nxw_file:nxw_file_file:upload")
    public WebElement fileInput;

    @FindBy(id = "document_create:nxl_file:nxw_file:nxw_file_file:choiceupload")
    public WebElement uploadFileRadioButton;

    /**
     * @param driver
     */
    public FileCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public FileDocumentBasePage createFileDocument(String title,
            String description, String fileToUploadPath) {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);
        if (fileToUploadPath != null) {
            uploadFileRadioButton.click();
            fileInput.sendKeys(fileToUploadPath);
        }
        createButton.click();
        return asPage(FileDocumentBasePage.class);
    }

}
