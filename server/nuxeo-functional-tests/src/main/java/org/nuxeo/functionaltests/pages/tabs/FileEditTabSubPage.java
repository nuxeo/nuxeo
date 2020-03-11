/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.forms.FileWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.openqa.selenium.WebDriver;

/**
 * Edit tab for File document type.
 *
 * @since 10.1
 */
public class FileEditTabSubPage extends EditTabSubPage {

    public FileEditTabSubPage(WebDriver driver) {
        super(driver);
    }

    public FileWidgetElement getFileWidgetElement() {
        LayoutElement layout = new LayoutElement(driver, "document_edit:nxl_file");
        // on file document, a widget template is used => standard file
        // widget is wrapped, hence the duplicate nxw_file id
        return layout.getWidget("nxw_file:nxw_file_file", FileWidgetElement.class);
    }

    public String getSelectedOption() {
        FileWidgetElement fileWidget = getFileWidgetElement();
        return fileWidget.getEditChoice();
    }

    public String getSelectedFilename() {
        FileWidgetElement fileWidget = getFileWidgetElement();
        return fileWidget.getFilename(true);
    }

    public String getSelectedFileErrorMessage() {
        LayoutElement layout = new LayoutElement(driver, "document_edit:nxl_file");
        return layout.getSubElement("nxw_file:nxw_file_message").getText();
    }

}
