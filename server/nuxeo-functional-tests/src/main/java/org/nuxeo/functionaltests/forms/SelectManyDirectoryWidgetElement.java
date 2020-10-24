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
 * Element representing a select many directory widget.
 *
 * @since 7.4
 */
public class SelectManyDirectoryWidgetElement extends WidgetElement {

    public SelectManyDirectoryWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    /**
     * Overridden to make sure the select element is reloaded correctly (for use cases where a selection triggers a
     * submit of the form in ajax).
     *
     * @since 8.1
     */
    @Override
    public void setInputValue(String value) {
        Select select = new Select(getInputElement());
        if (value != null) {
            String[] split = value.split(",");
            for (String v : split) {
                select.selectByVisibleText(v);
            }
        }
    }

    @Override
    public void setInput(WebElement elt, String value) {
        throw new UnsupportedOperationException("Use #setInputValue(String) instead");
    }

}
