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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Element representing a checkbox widget.
 *
 * @since 7.4
 */
public class CheckboxWidgetElement extends WidgetElement {

    public CheckboxWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    @Override
    public void setInput(WebElement elt, String value) {
        boolean isOn = Boolean.parseBoolean(getInputValue());
        boolean turnOn = Boolean.parseBoolean(value);
        if (isOn != turnOn) {
            elt.click();
        }
    }

    @Override
    public String getInputValue() {
        return Boolean.valueOf(getInputElement().isSelected()).toString();
    }

}
