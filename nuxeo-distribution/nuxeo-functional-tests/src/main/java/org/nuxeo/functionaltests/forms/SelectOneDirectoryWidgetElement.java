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
import org.openqa.selenium.support.ui.Select;

/**
 * Element representing a select one directory widget.
 *
 * @since 7.4
 */
public class SelectOneDirectoryWidgetElement extends WidgetElement {

    public SelectOneDirectoryWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    @Override
    public void setInput(WebElement elt, String value) {
        Select select = new Select(elt);
        if (value != null) {
            select.selectByVisibleText(value);
        } else {
            select.deselectAll();
        }
    }

    @Override
    public WebElement getOutputElement() {
        throw new UnsupportedOperationException("Output element cannot be retrived by id");
    }

}
