/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.forms;

import java.io.IOException;

import org.nuxeo.functionaltests.AbstractTest;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a file widget, with helper methods to retrieve/check its own
 * elements.
 *
 * @since 5.7
 */
public class FileWidgetElement extends AbstractWidgetElement {

    public FileWidgetElement(WebDriver driver, String id) {
        super(driver, id);
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

    public void uploadTestFile(String prefix, String suffix, String content)
            throws IOException {
        String fileToUploadPath = AbstractTest.getTmpFileToUploadPath(prefix, suffix,
                content);
        WebElement upRadioButton = getSubElement("choiceupload");
        upRadioButton.click();
        WebElement fileInput = getSubElement("upload");
        fileInput.sendKeys(fileToUploadPath);
    }

    public void removeFile() {
        if (hasSubElement("choicenone")) {
            WebElement delRadioButton = getSubElement("choicenone");
            delRadioButton.click();
        } else if (hasSubElement("choicedelete")) {
            WebElement delRadioButton = getSubElement("choicedelete");
            delRadioButton.click();
        } else {
            throw new NoSuchElementException(
                    "No delete choice available on widget");
        }
    }

}
