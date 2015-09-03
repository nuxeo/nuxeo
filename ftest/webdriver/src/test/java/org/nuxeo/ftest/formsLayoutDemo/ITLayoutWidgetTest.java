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
package org.nuxeo.ftest.formsLayoutDemo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.DateWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;

/**
 * @since 7.4
 */
public class ITLayoutWidgetTest extends AbstractWidgetPageTest {

    public ITLayoutWidgetTest() {
        super("layoutWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), "Value is required");

        String viewIdPrefix = "layoutWidgetLayout_view_form:nxl_layoutWidgetLayout_1:nxl_demoLayout_1:";
        String editIdPrefix = "layoutWidgetLayout_edit_form:nxl_layoutWidgetLayout:nxl_demoLayout:";

        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_textareaWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_datetimeWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_checkboxWidget_1")).getText());

        submitDemo();
        Locator.waitForTextPresent(driver.findElement(By.xpath("//html")), "Value is required");
        assertEquals("Value is required",
                driver.findElement(By.id(editIdPrefix + "nxw_textareaWidget_message")).getText());
        assertEquals("Value is required",
                driver.findElement(By.id(editIdPrefix + "nxw_datetimeWidget_message")).getText());
        assertEquals("Value is required", driver.findElement(By.id(editIdPrefix + "nxw_intWidget_message")).getText());
        assertEquals("", driver.findElement(By.id(editIdPrefix + "nxw_checkboxWidget_message")).getText());

        LayoutElement edit = new LayoutElement(driver, editIdPrefix);
        edit.getWidget("nxw_textareaWidget").setInputValue("test");
        edit.getWidget("nxw_datetimeWidget", DateWidgetElement.class).setInputValue("09/7/2010 03:14 PM");
        edit.getWidget("nxw_intWidget").setInputValue("42");

        submitDemo();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), "Value is required");
        assertEquals("test", driver.findElement(By.id(viewIdPrefix + "nxw_textareaWidget_1")).getText());
        assertEquals("9/7/2010", driver.findElement(By.id(viewIdPrefix + "nxw_datetimeWidget_1")).getText());
        assertEquals("42", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_checkboxWidget_1")).getText());

        get(HomePage.URL + pageId, Page.class);
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), "Value is required");
        assertEquals("test", driver.findElement(By.id(viewIdPrefix + "nxw_textareaWidget_1")).getText());
        assertEquals("9/7/2010", driver.findElement(By.id(viewIdPrefix + "nxw_datetimeWidget_1")).getText());
        assertEquals("42", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_checkboxWidget_1")).getText());

        navigateTo(pageId);
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_textareaWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_datetimeWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_checkboxWidget_1")).getText());
    }

}