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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
