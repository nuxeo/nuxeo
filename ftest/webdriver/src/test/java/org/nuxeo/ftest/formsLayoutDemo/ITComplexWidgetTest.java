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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 7.4
 */
public class ITComplexWidgetTest extends AbstractWidgetPageTest {

    public ITComplexWidgetTest() {
        super("complexWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        String tableStruct = "./table/tbody/tr/td[2]/div/table/tbody/";
        String viewFormId = "complexWidgetLayout_view_form";
        WebElement viewEl = driver.findElement(By.id(viewFormId));
        assertEquals("String item", viewEl.findElement(By.xpath(tableStruct + "tr[1]")).getText());
        assertEquals("Date item", viewEl.findElement(By.xpath(tableStruct + "tr[2]")).getText());
        assertEquals("Int item", viewEl.findElement(By.xpath(tableStruct + "tr[3]")).getText());
        assertEquals("Boolean item Yes", viewEl.findElement(By.xpath(tableStruct + "tr[4]")).getText());

        submitDemo();
        Locator.waitForTextPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        String editId = "complexWidgetLayout_edit_form:nxl_complexWidgetLayout:nxw_complexWidget";
        LayoutElement edit = new LayoutElement(driver, editId);
        edit.getWidget("nxw_stringComplexItem").setInputValue("test");
        edit.getWidget("nxw_dateComplexItem", DateWidgetElement.class).setInputValue("09/7/2010 03:14 PM");
        edit.getWidget("nxw_intComplexItem").setInputValue("lalala");
        edit.getWidget("nxw_booleanComplexItem").setInputValue("false");

        submitDemo();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertEquals("'lalala' is not a number. Example: 99.",
                driver.findElement(By.id(editId + ":nxw_intComplexItem_message")).getText());
        edit = new LayoutElement(driver, editId);
        edit.getWidget("nxw_intComplexItem").setInputValue("5");

        submitDemo();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        viewEl = driver.findElement(By.id(viewFormId));
        assertEquals("test", viewEl.findElement(By.xpath(tableStruct + "tr[1]/td[2]")).getText());
        assertEquals("9/7/2010", viewEl.findElement(By.xpath(tableStruct + "tr[2]/td[2]")).getText());
        assertEquals("5", viewEl.findElement(By.xpath(tableStruct + "tr[3]/td[2]")).getText());
        assertEquals("No", viewEl.findElement(By.xpath(tableStruct + "tr[4]/td[2]")).getText());
    }

}