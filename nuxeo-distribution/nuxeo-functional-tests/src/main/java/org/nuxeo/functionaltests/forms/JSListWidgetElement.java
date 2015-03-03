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

import java.util.List;

import org.nuxeo.functionaltests.JSListRequestManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a list widget, with helper method to retrieve/check its subwidgets.
 *
 * @since 7.2
 */
public class JSListWidgetElement extends AbstractWidgetElement {

    public static enum Display {
        BLOCK_LEFT, BLOCK_TOP, TABLE, INLINE
    }

    /** The display attribute controls the rendering of subwidgets. */
    private final Display display;

    public JSListWidgetElement(WebDriver driver, String id) {
        this(driver, id, Display.BLOCK_LEFT);
    }

    public JSListWidgetElement(WebDriver driver, String id, Display display) {
        super(driver, id);
        this.display = display;
    }

    protected String getListSubElementSuffix(String subId, int index) {
        return String.format("%s:%s", Integer.valueOf(index), subId);
    }

    public void addNewElement() {
        WebElement addElement = getElement(id + "_add");
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        addElement.click();
        rm.end();
    }

    public void removeElement(int index) {
        WebElement delElement = getRowActions(index).findElement(By.className("deleteBtn"));
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        delElement.click();
        rm.end();
    }

    public void moveUpElement(int index) {
        WebElement moveElement = getRowActions(index).findElement(By.className("moveUpBtn"));
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        moveElement.click();
        rm.end();
    }


    public void moveDownElement(int index) {
        WebElement moveElement = getRowActions(index).findElement(By.className("moveDownBtn"));
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        moveElement.click();
        rm.end();
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
        if (display == Display.TABLE || display == Display.INLINE) {
            return driver.findElement(By.cssSelector(getRowCssSelector(i) + " > .listWidgetActions"));
        }
        return getSubElement(getListSubElementSuffix(getWidgetId() + "_actions", i));
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
        String path = getElementCssSelector();
        if (display == Display.TABLE || display == Display.INLINE) {
            path += " > table > tbody";
        }
        return path + " > .listItem";
    }

    private String getRowCssSelector(int i) {
        return getRowsCssSelector() + ":nth-of-type(" + (i + 1) + ")";
    }
}