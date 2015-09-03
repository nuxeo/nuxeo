/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
