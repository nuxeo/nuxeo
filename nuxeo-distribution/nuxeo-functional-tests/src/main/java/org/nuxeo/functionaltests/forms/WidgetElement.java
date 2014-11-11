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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a simple widget element, with helper methods to set/get values on
 * it, depending on the mode.
 *
 * @since 5.7
 */
public class WidgetElement extends AbstractWidgetElement {

    public WidgetElement(WebDriver driver, String id) {
        super(driver, id);
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