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
        edit.getWidget("nxw_dateWidget", DateWidgetElement.class).setInputValue("09/7/2010, 03:14 PM");
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
