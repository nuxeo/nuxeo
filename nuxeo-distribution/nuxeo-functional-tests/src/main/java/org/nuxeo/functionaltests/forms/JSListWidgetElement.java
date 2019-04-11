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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.functionaltests.forms;

import java.util.List;

import org.nuxeo.functionaltests.JSListRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a list widget, with helper method to retrieve/check its subwidgets.
 *
 * @since 7.2
 */
public class JSListWidgetElement extends AbstractWidgetElement {

    public enum Display {
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
        Locator.waitUntilEnabledAndClick(addElement);
        rm.end();
    }

    public void removeElement(int index) {
        WebElement delElement = getRowActions(index).findElement(By.className("deleteBtn"));
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        Locator.waitUntilEnabledAndClick(delElement);
        rm.end();
    }

    public void moveUpElement(int index) {
        WebElement moveElement = getRowActions(index).findElement(By.className("moveUpBtn"));
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        Locator.waitUntilEnabledAndClick(moveElement);
        rm.end();
    }

    public void moveDownElement(int index) {
        WebElement moveElement = getRowActions(index).findElement(By.className("moveDownBtn"));
        JSListRequestManager rm = new JSListRequestManager(driver);
        rm.begin();
        Locator.waitUntilEnabledAndClick(moveElement);
        rm.end();
    }

    public void waitForSubWidget(String id, int index) {
        getSubElement(getListSubElementSuffix(id, index), true);
    }

    public WidgetElement getSubWidget(String id, int index) {
        return getSubWidget(id, index, false);
    }

    /**
     * @since 8.3
     */
    public String getSubWidgetId(String id, int index) {
        return getSubWidgetId(id, index, false);
    }

    public WidgetElement getSubWidget(String id, int index, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementSuffix(id, index));
    }

    /**
     * @since 8.3
     */
    public String getSubWidgetId(String id, int index, boolean wait) {
        return getSubWidget(id, index, wait).getId();
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
        return driver.findElement(By.cssSelector(getRowCssSelector(i) + " .listWidgetActionsTable"));
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
