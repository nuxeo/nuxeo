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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;

/**
 * @since 7.4
 */
public class ITActionsWidgetTest extends AbstractWidgetPageTest {

    public ITActionsWidgetTest() {
        super("actionsWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        String idPrefix = "actionsWidgetLayout_view_form:nxl_actionsWidgetLayout_1:";
        String linkXpath = "//a[@id='" + idPrefix + "nxw_actionsWidget_1_layoutDemoLink']/span";
        String actionsXpath = "//div[@id='" + idPrefix + "nxw_actionsWidget_1_panel']/div/a/span";

        String fancyXPath = "//a[@id='" + idPrefix + "nxw_actionsWidget_1_layoutDemoFancyBox_link']/span";
        String widgetId = "actionsWidgetLayout_view_form:nxl_actionsWidgetLayout_1:nxw_actionTextWidget_1";
        assertNotNull(driver.findElement(By.xpath(linkXpath)));
        assertNotNull(driver.findElement(By.xpath(actionsXpath)));
        assertNotNull(driver.findElement(By.xpath(fancyXPath)));
        assertEquals("", driver.findElement(By.id(widgetId)).getText());
        doSubmitDemo();

        assertEquals(VALUE_REQUIRED,
                driver.findElement(
                        By.id("actionsWidgetLayout_edit_form:nxl_actionsWidgetLayout:nxw_actionTextWidget_message"))
                      .getText());
        driver.findElement(By.id("actionsWidgetLayout_edit_form:nxl_actionsWidgetLayout:nxw_actionTextWidget"))
              .sendKeys("test");
        doSubmitDemo();

        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertNotNull(driver.findElement(By.xpath(linkXpath)));
        assertNotNull(driver.findElement(By.xpath(actionsXpath)));
        assertNotNull(driver.findElement(By.xpath(fancyXPath)));
        assertEquals("test", driver.findElement(By.id(widgetId)).getText());

        get(HomePage.URL + pageId, Page.class);
        assertNotNull(driver.findElement(By.xpath(linkXpath)));
        assertNotNull(driver.findElement(By.xpath(actionsXpath)));
        assertNotNull(driver.findElement(By.xpath(fancyXPath)));
        driver.findElement(By.xpath(fancyXPath)).click();
        Locator.waitUntilElementPresent(By.cssSelector("#nxw_actionsWidget_1_layoutDemoFancyBox_after_view_box > div > h3"));
        assertEquals("Fancy box sample",
                driver.findElement(By.cssSelector("#nxw_actionsWidget_1_layoutDemoFancyBox_after_view_box > div > h3")).getText());
        assertNotNull(driver.findElement(By.id("fancybox-close")));
        driver.findElement(By.id("fancybox-close")).click();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), "Fancy box sample");
        assertEquals("test", driver.findElement(By.id(widgetId)).getText());

        navigateTo(pageId);
        assertNotNull(driver.findElement(By.xpath(linkXpath)));
        assertNotNull(driver.findElement(By.xpath(actionsXpath)));
        assertNotNull(driver.findElement(By.xpath(fancyXPath)));
        assertEquals("", driver.findElement(By.id(widgetId)).getText());
    }

}