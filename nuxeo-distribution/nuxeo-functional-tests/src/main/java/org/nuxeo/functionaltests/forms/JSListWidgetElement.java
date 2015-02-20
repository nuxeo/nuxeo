/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.functionaltests.forms;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Represents a list widget, with helper method to retrieve/check its subwidgets.
 *
 * @since 5.7
 */
public class JSListWidgetElement extends AbstractWidgetElement {

    public JSListWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    protected String getListSubElementSuffix(String subId, int index) {
        return String.format("%s:%s", Integer.valueOf(index), subId);
    }

    public void addNewElement() {
        WebElement addElement = getElement(id + "_add");
        addElement.click();
    }

    public void removeElement(int index) {
        WebElement delElement = getRowActions(index).findElement(By.className("deleteBtn"));
        delElement.click();
    }

    public void waitForSubWidget(String id, int index) {
        getSubElement(getListSubElementSuffix(id, index), true);
    }

    public WidgetElement getSubWidget(String id, int index) {
        return getSubWidget(id, index, false);
    }

    public WidgetElement getSubWidget(String id, int index, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementSuffix(id, index));
    }

    public <T> T getSubWidget(String id, int index, Class<T> widgetClassToProxy, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementSuffix(id, index), widgetClassToProxy);
    }

    @Override
    public String getMessageValue() {
        return getMessageValue("_message");
    }

    protected WebElement getRowActions(int i) {
        String wid = getWidgetId();
        return getSubElement(getListSubElementSuffix(wid + "_actions", i));
    }

    public String getSubWidgetMessageValue(String id, int idx) {
        return getSubWidgetMessageValue(id, idx, 0);
    }

    public String getSubWidgetMessageValue(String id, int idx, int msgIdx) {
        String subId = id + "_message";
        if (msgIdx != 0) {
            subId += "_" + msgIdx;
        }
        return getMessageValue(":" + getListSubElementSuffix(subId, idx));
    }

    public List<WebElement> getRows() {
        return driver.findElements(By.cssSelector(getRowsCssSelector()));
    }

    private String getElementCssSelector() {
        return "#" + id.replaceAll(":", "\\\\:") + "_panel";
    }

    private String getRowsCssSelector() {
        return getElementCssSelector() + " > .listItem";
    }
}