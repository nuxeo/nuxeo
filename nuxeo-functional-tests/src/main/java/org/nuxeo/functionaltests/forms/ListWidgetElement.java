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
 * Represents a list widget, with helper method to retrieve/check its
 * subwidgets.
 *
 * @since 5.7
 */
public class ListWidgetElement extends AbstractWidgetElement {

    public ListWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    protected String getListElementId() {
        return String.format("%s_input", getWidgetId());
    }

    protected String getListSubElementId(String subId, int index) {
        return String.format("%s:%s:%s", getListElementId(),
                Integer.valueOf(index), subId);
    }

    public void addNewElement() {
        String wid = getWidgetId();
        WebElement addElement = getSubElement(wid + "_add");
        addElement.click();
    }

    public void removeElement(int index) {
        String wid = getWidgetId();
        String delId = String.format("%s:%s:%s_delete", getListElementId(),
                Integer.valueOf(index), wid);
        WebElement delElement = getSubElement(delId);
        delElement.click();
    }

    public void waitForSubWidget(String id, int index) {
        getSubElement(getListSubElementId(id, index), true);
    }

    public WidgetElement getSubWidget(String id, int index, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementId(id, index));
    }

    public <T> T getSubWidget(String id, int index,
            Class<T> widgetClassToProxy, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementId(id, index), widgetClassToProxy);
    }

}