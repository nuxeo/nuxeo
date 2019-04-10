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
        assertEquals("'lalala' is not a number. Example: 98765432.",
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
