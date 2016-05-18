/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.JSListWidgetElement.Display;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a list widget, with helper method to retrieve/check its subwidgets.
 *
 * @since 5.7
 * @deprecated since 7.2: {@link JSListWidgetElement} should now be used, this class is kept for compatibility.
 */
@Deprecated
public class ListWidgetElement extends AbstractWidgetElement {

    /**
     * The display attribute controls the rendering of subwidgets.
     *
     * @since 7.2
     */
    private final Display display;

    public ListWidgetElement(WebDriver driver, String id) {
        this(driver, id, Display.BLOCK_LEFT);
    }

    /**
     * @since 7.2
     */
    public ListWidgetElement(WebDriver driver, String id, JSListWidgetElement.Display display) {
        super(driver, id);
        this.display = display;
    }

    /**
     * @since 7.2
     */
    public String getListElementId() {
        return String.format("%s_input", getWidgetId());
    }

    protected String getListSubElementId(String subId, int index) {
        return String.format("%s:%s:%s", getListElementId(), Integer.valueOf(index), subId);
    }

    /**
     * @since 7.2
     */
    protected String getListSubElementSuffix(String subId, int index) {
        return String.format("%s:%s:%s", getListElementId(), Integer.valueOf(index), subId);
    }

    public void addNewElement() {
        String wid = getWidgetId();
        WebElement addElement = getSubElement(wid + "_add");
        AjaxRequestManager arm = new AjaxRequestManager(AbstractTest.driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(addElement);
        arm.end();
    }

    public void removeElement(int index) {
        String wid = getWidgetId();
        String delId = String.format("%s:%s:%s_delete", getListElementId(), Integer.valueOf(index), wid);
        WebElement delElement = getSubElement(delId);
        AjaxRequestManager arm = new AjaxRequestManager(AbstractTest.driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(delElement);
        arm.end();
    }

    /**
     * @since 7.2
     */
    public void moveUpElement(int index) {
        WebElement moveElement = getRowActions(index).findElement(By.className("moveUpBtn"));
        AjaxRequestManager arm = new AjaxRequestManager(AbstractTest.driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(moveElement);
        arm.end();
    }

    /**
     * @since 7.2
     */
    public void moveDownElement(int index) {
        WebElement moveElement = getRowActions(index).findElement(By.className("moveDownBtn"));
        AjaxRequestManager arm = new AjaxRequestManager(AbstractTest.driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(moveElement);
        arm.end();
    }

    public void waitForSubWidget(String id, int index) {
        getSubElement(getListSubElementId(id, index), true);
    }

    /**
     * @since 7.2
     */
    public WidgetElement getSubWidget(String id, int index) {
        return getSubWidget(id, index, false);
    }

    /**
     * @since 8.3
     */
    public String getSubWidgetId(String id, int index) {
        return getSubWidget(id, index, false).getId();
    }

    public WidgetElement getSubWidget(String id, int index, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementId(id, index));
    }

    public <T> T getSubWidget(String id, int index, Class<T> widgetClassToProxy, boolean wait) {
        if (wait) {
            waitForSubWidget(id, index);
        }
        return getWidget(getListSubElementId(id, index), widgetClassToProxy);
    }

    @Override
    public String getMessageValue() {
        return getMessageValue(":" + getWidgetId() + "_message");
    }

    /**
     * @since 7.2
     */
    public List<WebElement> getRows() {
        return driver.findElements(By.cssSelector(getRowsCssSelector()));
    }

    private String getElementCssSelector() {
        return "#" + getId().replaceAll(":", "\\\\:") + "\\:" + getWidgetId() + "_panel";
    }

    private String getRowsCssSelector() {
        String path = getElementCssSelector();
        if (display == Display.TABLE || display == Display.INLINE) {
            path += " > table > tbody";
        }
        return path + " > .listItem";
    }

    protected WebElement getRowActions(int i) {
        if (display == Display.TABLE || display == Display.INLINE) {
            return driver.findElement(By.cssSelector(getRowCssSelector(i) + " > .listWidgetActions"));
        }
        return getSubElement(getListSubElementSuffix(getWidgetId() + "_actions", i));
    }

    private String getRowCssSelector(int i) {
        return getRowsCssSelector() + ":nth-of-type(" + (i + 1) + ")";
    }

    /**
     * @since 7.2
     */
    public String getSubWidgetMessageValue(String id, int idx) {
        return getSubWidgetMessageValue(id, idx, 0);
    }

    /**
     * @since 7.2
     */
    public String getSubWidgetMessageValue(String id, int idx, int msgIdx) {
        String subId = id + "_message";
        if (msgIdx != 0) {
            subId += "_" + msgIdx;
        }
        return getMessageValue(":" + getListSubElementSuffix(subId, idx));
    }

}
