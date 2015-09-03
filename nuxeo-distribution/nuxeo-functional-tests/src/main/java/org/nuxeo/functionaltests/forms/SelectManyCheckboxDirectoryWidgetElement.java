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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Element representing a select many checkbox directory widget.
 *
 * @since 7.4
 */
public class SelectManyCheckboxDirectoryWidgetElement extends WidgetElement {

    public SelectManyCheckboxDirectoryWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    @Override
    public void setInput(WebElement elt, String value) {
        List<WebElement> options = getInputElement().findElements(By.xpath(".//input[@type='checkbox']"));
        List<String> ids = new ArrayList<String>();
        if (StringUtils.isBlank(value)) {
            for (WebElement option : options) {
                if (option.isSelected()) {
                    ids.add(option.getAttribute("id"));
                }
            }
        } else {
            String[] split = value.split(",");
            for (String v : split) {
                for (WebElement option : options) {
                    if (option.getAttribute("value").equals(v) && !option.isSelected()) {
                        ids.add(option.getAttribute("id"));
                    }
                }
            }
        }
        // click options by id in case widget is ajaxified
        for (String id : ids) {
            driver.findElement(By.id(id)).click();
        }
    }

    @Override
    public String getInputValue() {
        StringBuilder res = new StringBuilder();
        List<WebElement> options = getInputElement().findElements(By.xpath(".//input[type='checkbox']"));
        for (WebElement option : options) {
            if (option.isSelected()) {
                if (res.length() != 0) {
                    res.append(",");
                }
                res.append(option.getAttribute("value"));
            }
        }
        return res.toString();
    }

}
