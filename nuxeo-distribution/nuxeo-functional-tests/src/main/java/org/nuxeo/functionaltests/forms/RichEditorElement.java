/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.functionaltests.forms;

import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represent a rich editor widget.
 *
 * @since 5.9.4
 */
public class RichEditorElement extends WidgetElement {

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
    @Override
    public void setInputValue(String content) {
        Locator.waitUntilElementPresent(By.className("mce-tinymce"));
        // Define the script which sets the content of the editor
        String scriptToExecute = String.format("tinyMCE.editors['%s'].insertContent('%s')", id, content);
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

    /**
     * @since 7.1
     */
    public String getRawContent() {
        String scriptToExecute = String.format("return tinyMCE.editors['%s'].getBody().textContent", id);
        String result = (String) ((JavascriptExecutor) driver).executeScript(scriptToExecute);
        if (result == null) {
            return "";
        }
        return result.replaceAll("[\uFEFF-\uFFFF]", "");
    }

    /**
     * @since 7.1
     */
    public String getHtmlContent() {
        String scriptToExecute = String.format("return tinyMCE.editors['%s'].getContent()", id);
        String result = (String) ((JavascriptExecutor) driver).executeScript(scriptToExecute);
        return result;
    }

    @Override
    public String getInputValue() {
        return getHtmlContent();
    }

}
