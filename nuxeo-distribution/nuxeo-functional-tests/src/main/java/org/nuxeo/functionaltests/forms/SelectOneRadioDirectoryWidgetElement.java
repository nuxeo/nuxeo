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
