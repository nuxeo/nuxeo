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
import static org.junit.Assert.assertNotEquals;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.nuxeo.functionaltests.formsLayoutDemo.page.ValidationPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 7.4
 */
public abstract class AbstractWidgetPageTest extends AbstractTest {

    protected final static String VALUE_REQUIRED = ValidationPage.VALUE_REQUIRED;

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
        WebElement message = Locator.findElementAndWaitUntilEnabled(
                By.id(pageId + "Layout_edit_form:nxl_" + pageId + "Layout:nxw_" + pageId + "_message"));
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
            assertEquals(VALUE_REQUIRED, getEditWidgetMessage());
        } else {
            assertNotEquals(VALUE_REQUIRED, getEditWidgetMessage());
        }

    }

    protected void submitDemo() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        doSubmitDemo();
        arm.waitForAjaxRequests();
    }

    protected void doSubmitDemo() {
        Locator.findElementWaitUntilEnabledAndClick(By.xpath("//input[@value='Submit']"));
    }

}
