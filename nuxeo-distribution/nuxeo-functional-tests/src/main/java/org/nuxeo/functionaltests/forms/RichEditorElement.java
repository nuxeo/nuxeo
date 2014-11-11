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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.functionaltests.forms;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represent a rich editor widget.
 *
 * @since 5.9.4
 */
public class RichEditorElement extends AbstractWidgetElement {

    /**
     * @param driver
     * @param id
     */
    public RichEditorElement(WebDriver driver, String id) {
        super(driver, id);
    }

    /**
     * Insert content in the editor of the document.
     *
     * @param content The content to define in the document.
     */
    public void insertContent(String content) {
        // Define the script which sets the content of the editor
        String scriptToExecute = String.format("tinyMCE.activeEditor.insertContent('%s')",
                content);
        // Set the content of the editor
        ((JavascriptExecutor) driver).executeScript(scriptToExecute);
    }

    /**
     * Actions a click on the "Bold" button in the editor
     */
    public void clickBoldButton() {
        // Get the bold button
        WebElement button = driver.findElement(By.cssSelector(".mce-btn[aria-label='Bold'] button"));
        button.click();
    }

    public void clickItalicButton() {
        // Get the italic button
        WebElement button = driver.findElement(By.cssSelector(".mce-btn[aria-label='Italic'] button"));
        button.click();
    }
}
