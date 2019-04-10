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
import static org.junit.Assert.assertNotEquals;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 7.4
 */
public abstract class AbstractWidgetPageTest extends AbstractTest {

    protected String pageId;

    public AbstractWidgetPageTest(String pageId) {
        super();
        this.pageId = pageId;
    }

    protected void navigateTo(String pageId) {
        driver.get(HomePage.URL);
        get(HomePage.URL + pageId, Page.class);
    }

    protected WidgetElement getViewWidget() {
        return getViewWidget(WidgetElement.class);
    }

    protected WidgetElement getViewWidget(Class<? extends WidgetElement> widgetClassToProxy) {
        LayoutElement view = new LayoutElement(driver, pageId + "Layout_view_form:nxl_" + pageId + "Layout_1");
        WidgetElement w = view.getWidget("nxw_" + pageId + "_1", widgetClassToProxy);
        return w;
    }

    protected WidgetElement getEditWidget() {
        return getEditWidget(WidgetElement.class);
    }

    protected WidgetElement getEditWidget(Class<? extends WidgetElement> widgetClassToProxy) {
        LayoutElement edit = new LayoutElement(driver, pageId + "Layout_edit_form:nxl_" + pageId + "Layout");
        WidgetElement w = edit.getWidget("nxw_" + pageId, widgetClassToProxy);
        return w;
    }

    protected String getEditWidgetMessage() {
        WebElement message = driver.findElement(By.id(pageId + "Layout_edit_form:nxl_" + pageId + "Layout:nxw_"
                + pageId + "_message"));
        if (message != null) {
            return message.getText();
        }
        return "";
    }

    protected void checkNoError() {
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), "ERROR");
    }

    protected void checkValueRequired(boolean present) {
        if (present) {
            assertEquals("Value is required", getEditWidgetMessage());
        } else {
            assertNotEquals("Value is required", getEditWidgetMessage());
        }

    }

    protected void submitDemo() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        doSubmitDemo();
        arm.waitForAjaxRequests();
    }

    protected void doSubmitDemo() {
        driver.findElement(By.xpath("//input[@value='Submit']")).click();
    }

}
