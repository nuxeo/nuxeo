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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a simple widget element, with helper methods to set/get values on it, depending on the mode.
 *
 * @since 5.7
 */
public class WidgetElement extends AbstractWidgetElement {

    public WidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    public WidgetElement(WebDriver driver, WebElement element) {
        super(driver, element.getAttribute("id"));
    }

    public WebElement getInputElement() {
        return getElement(id);
    }

    public WebElement getOutputElement() {
        return getElement(id);
    }

    public String getValue(boolean isEdit) {
        if (isEdit) {
            return getInputValue();
        } else {
            return getOutputValue();
        }
    }

    public String getOutputValue() {
        return getOutputElement().getText();
    }

    public void setInputValue(String value) {
        setInput(getInputElement(), value);
    }

    public String getInputValue() {
        return getInputElement().getAttribute("value");
    }

}
