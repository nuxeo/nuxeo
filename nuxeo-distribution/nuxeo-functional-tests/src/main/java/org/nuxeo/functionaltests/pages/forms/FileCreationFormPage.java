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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.forms;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.functionaltests.forms.FileWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.openqa.selenium.WebDriver;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class FileCreationFormPage extends DublinCoreCreationDocumentFormPage {

    public FileCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public FileDocumentBasePage createFileDocument(String title, String description, boolean uploadBlob,
            String filePrefix, String fileSuffix, String fileContent) throws IOException {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);

        if (uploadBlob) {
            uploadBlob(filePrefix, fileSuffix, fileContent);
        }

        create();
        return asPage(FileDocumentBasePage.class);
    }

    /**
     * Create a file document referencing an existing file path.
     *
     * @since 9.1
     */
    public FileDocumentBasePage createFileDocument(String title, String description, String filePath)
            throws IOException {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);

        if (!StringUtils.isBlank(filePath)) {
            FileWidgetElement fileWidget = getFileWidgetElement();
            fileWidget.uploadFile(filePath);
        }

        create();
        return asPage(FileDocumentBasePage.class);
    }

    protected FileWidgetElement getFileWidgetElement() {
        LayoutElement layout = new LayoutElement(driver, "document_create:nxl_file");
        // on file document, a widget template is used => standard file
        // widget is wrapped, hence the duplicate nxw_file id
        return layout.getWidget("nxw_file:nxw_file_file", FileWidgetElement.class);
    }

    protected void uploadBlob(String filePrefix, String fileSuffix, String fileContent) throws IOException {
        FileWidgetElement fileWidget = getFileWidgetElement();
        fileWidget.uploadTestFile(filePrefix, fileSuffix, fileContent);
    }

    /**
     * @since 7.1
     */
    public FileCreationFormPage createFileDocumentWithoutTitle(String filePrefix, String fileSuffix, String fileContent)
            throws IOException {
        uploadBlob(filePrefix, fileSuffix, fileContent);
        create();
        return asPage(FileCreationFormPage.class);
    }

    /**
     * @since 7.1
     */
    public String getSelectedOption() {
        FileWidgetElement fileWidget = getFileWidgetElement();
        return fileWidget.getEditChoice();
    }

    /**
     * @since 7.1
     */
    public String getSelectedFilename() {
        FileWidgetElement fileWidget = getFileWidgetElement();
        return fileWidget.getFilename(true);
    }

    /**
     * Returns the element error message if any.
     *
     * @since 10.1
     */
    public String getSelectedFileErrorMessage() {
        LayoutElement layout = new LayoutElement(driver, "document_create:nxl_file");
        return layout.getSubElement("nxw_file:nxw_file_message").getText();
    }

}
