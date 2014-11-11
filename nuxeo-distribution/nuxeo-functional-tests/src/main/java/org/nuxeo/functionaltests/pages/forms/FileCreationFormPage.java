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

import java.io.IOException;

import org.nuxeo.functionaltests.forms.FileWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.openqa.selenium.WebDriver;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class FileCreationFormPage extends DublinCoreCreationDocumentFormPage {

    /**
     * @param driver
     */
    public FileCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public FileDocumentBasePage createFileDocument(String title,
            String description, boolean uploadBlob, String filePrefix,
            String fileSuffix, String fileContent) throws IOException {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);

        if (uploadBlob) {
            LayoutElement layout = new LayoutElement(driver,"document_create:nxl_file");
            // on file document, a widget template is used => standard file
            // widget is wrapped, hence the duplicate nxw_file id
            FileWidgetElement fileWidget = layout.getWidget(
                    "nxw_file:nxw_file_file", FileWidgetElement.class);
            fileWidget.uploadTestFile(filePrefix, fileSuffix, fileContent);
        }

        create();
        return asPage(FileDocumentBasePage.class);
    }
}
