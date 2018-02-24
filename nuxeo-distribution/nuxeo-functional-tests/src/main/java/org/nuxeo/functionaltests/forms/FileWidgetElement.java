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

    public enum InputFileChoice {
        none, keep, upload, delete, tempKeep;

        public String getOptionId() {
            return "choice" + name();
        }
    }

    /**
     * @since 7.1
     */
    public String getEditChoice() {
        for (InputFileChoice choice : InputFileChoice.values()) {
            String subid = choice.getOptionId();
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
        WebElement upRadioButton = getSubElement("choiceupload");
        upRadioButton.click();
        uploadFile(fileToUploadPath);
    }

    /**
     * Uploads file with given path.
     *
     * @throws IOException
     * @since 9.1
     */
    public void uploadFile(String filePath) throws IOException {
        selectChoice(InputFileChoice.upload);
        WebElement fileInput = getSubElement("upload");
        fileInput.sendKeys(filePath);
    }

    /**
     * Selects available option that should result in no file being present on the document.
     */
    public void removeFile() {
        if (hasChoice(InputFileChoice.none)) {
            selectChoice(InputFileChoice.none);
        } else if (hasChoice(InputFileChoice.delete)) {
            selectChoice(InputFileChoice.delete);
        } else {
            throw new NoSuchElementException("No delete choice available on widget");
        }
    }

    /**
     * Selects available option that should result in keeping selected file.
     *
     * @since 10.1
     */
    public void keepFile() {
        if (hasChoice(InputFileChoice.keep)) {
            selectChoice(InputFileChoice.keep);
        } else if (hasChoice(InputFileChoice.tempKeep)) {
            selectChoice(InputFileChoice.tempKeep);
        } else {
            throw new NoSuchElementException("No keep choice available on widget");
        }
    }

    /**
     * Returns true if given option is presented on file widget radio options.
     *
     * @since 10.1
     */
    public boolean hasChoice(InputFileChoice choice) {
        return hasSubElement(choice.getOptionId());
    }

    /**
     * Select given choice on file widget radio options.
     *
     * @since 10.1
     * @throws NoSuchElementException if option is not available.
     */
    public void selectChoice(InputFileChoice choice) {
        String id = choice.getOptionId();
        if (hasSubElement(id)) {
            Locator.waitUntilEnabled(getSubElement(id));
            getSubElement(id).click();
        } else {
            throw new NoSuchElementException("No '" + choice.name() + "' choice available on widget");
        }
    }

}
