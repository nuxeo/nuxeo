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
public class ITContainerWidgetTest extends AbstractWidgetPageTest {

    public ITContainerWidgetTest() {
        super("containerWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        String viewIdPrefix = "containerWidgetLayout_view_form:nxl_containerWidgetLayout_1:";
        String editIdPrefix = "containerWidgetLayout_edit_form:nxl_containerWidgetLayout:";

        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_textWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_dateWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_booleanWidget_1")).getText());

        submitDemo();
        Locator.waitForTextPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertEquals(VALUE_REQUIRED, driver.findElement(By.id(editIdPrefix + "nxw_textWidget_message")).getText());

        LayoutElement edit = new LayoutElement(driver, editIdPrefix);
        edit.getWidget("nxw_textWidget").setInputValue("test");
        edit.getWidget("nxw_dateWidget", DateWidgetElement.class).setInputValue("09/7/2010 03:14 PM");
        edit.getWidget("nxw_intWidget").setInputValue("42");

        submitDemo();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertEquals("test", driver.findElement(By.id(viewIdPrefix + "nxw_textWidget_1")).getText());
        assertEquals("9/7/2010", driver.findElement(By.id(viewIdPrefix + "nxw_dateWidget_1")).getText());
        assertEquals("42", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_booleanWidget_1")).getText());

        get(HomePage.URL + pageId, Page.class);
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertEquals("test", driver.findElement(By.id(viewIdPrefix + "nxw_textWidget_1")).getText());
        assertEquals("9/7/2010", driver.findElement(By.id(viewIdPrefix + "nxw_dateWidget_1")).getText());
        assertEquals("42", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_booleanWidget_1")).getText());

        navigateTo(pageId);
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_textWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_dateWidget_1")).getText());
        assertEquals("", driver.findElement(By.id(viewIdPrefix + "nxw_intWidget_1")).getText());
        assertEquals("No", driver.findElement(By.id(viewIdPrefix + "nxw_booleanWidget_1")).getText());
    }

}