/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.functionaltests.forms;

import java.io.IOException;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a file widget, with helper methods to retrieve/check its own elements.
 *
 * @since 5.7
 */
public class FileWidgetElement extends AbstractWidgetElement {

    public FileWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    enum InputFileChoice {
        none, keep, upload, delete, tempKeep,
    }

    /**
     * @since 7.1
     */
    public String getEditChoice() {
        for (InputFileChoice choice : InputFileChoice.values()) {
            String subid = "choice" + choice.name();
            if (hasSubElement(subid) && getSubElement(subid).isSelected()) {
                return choice.name();
            }
        }
        return null;
    }

    public String getFilename(boolean isEdit) {
        WebElement link;
        if (isEdit) {
            link = getSubElement("default_download:download");
        } else {
            link = getSubElement("download");
        }
        return link.getText();
    }

    public void uploadTestFile(String prefix, String suffix, String content) throws IOException {
        String fileToUploadPath = AbstractTest.getTmpFileToUploadPath(prefix, suffix, content);
        uploadFile(fileToUploadPath);
    }

    /**
     * Uploads file with given path.
     *
     * @throws IOException
     * @since 9.1
     */
    public void uploadFile(String filePath) throws IOException {
        WebElement upRadioButton = getSubElement("choiceupload");
        Locator.waitUntilEnabledAndClick(upRadioButton);
        WebElement fileInput = getSubElement("upload");
        fileInput.sendKeys(filePath);
    }

    public void removeFile() {
        if (hasSubElement("choicenone")) {
            WebElement delRadioButton = getSubElement("choicenone");
            delRadioButton.click();
        } else if (hasSubElement("choicedelete")) {
            WebElement delRadioButton = getSubElement("choicedelete");
            delRadioButton.click();
        } else {
            throw new NoSuchElementException("No delete choice available on widget");
        }
    }

}
