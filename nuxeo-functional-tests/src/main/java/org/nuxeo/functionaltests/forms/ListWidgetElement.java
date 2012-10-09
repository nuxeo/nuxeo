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

import org.nuxeo.functionaltests.AbstractTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a list widget, with helper method to retrieve/check its
 * subwidgets.
 *
 * @since 5.7
 */
public class ListWidgetElement extends WidgetElement {

    public ListWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    public void addNewElement() {
        String wid = getWidgetId();
        WebElement addElement = getSubElement(wid + "_add");
        addElement.click();
    }

    public void addElement(String subId, String value) {
        addNewElement();
        WebElement elt = AbstractTest.findElementWithTimeout(By.id(getSubElementId(subId)));
        if (value == null) {
            elt.sendKeys("");
        } else {
            elt.sendKeys(value);
        }
    }

    public void removeElement(int index) {
        String wid = getWidgetId();
        String delId = String.format("%s_input:%s:%s_delete", wid,
                Integer.valueOf(index), wid);
        WebElement delElement = getSubElement(delId);
        delElement.click();
    }

}