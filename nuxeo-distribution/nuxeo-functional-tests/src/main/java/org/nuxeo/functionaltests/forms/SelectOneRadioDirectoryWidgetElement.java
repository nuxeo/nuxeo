/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Element representing a select one radio directory widget.
 *
 * @since 7.4
 */
public class SelectOneRadioDirectoryWidgetElement extends WidgetElement {

    public SelectOneRadioDirectoryWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    /**
     * Value should be the id of the option to select
     */
    @Override
    public void setInput(WebElement elt, String value) {
        List<WebElement> options = getInputElement().findElements(By.xpath(".//input[@type='radio']"));
        for (WebElement option : options) {
            if (option.getAttribute("value").equals(value)) {
                option.click();
                break;
            }
        }
    }

    @Override
    public String getInputValue() {
        List<WebElement> options = getInputElement().findElements(By.xpath(".//input[type='radio']"));
        for (WebElement option : options) {
            if (option.isSelected()) {
                return option.getAttribute("value");
            }
        }
        return "";
    }

    @Override
    public WebElement getOutputElement() {
        throw new UnsupportedOperationException("Output element cannot be retrived by id");
    }

}
