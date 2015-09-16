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
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.DateWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 7.4
 */
public class ITToggleableLayoutWidgetTest extends AbstractWidgetPageTest {

    public ITToggleableLayoutWidgetTest() {
        super("toggleableLayoutWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        String viewIdPrefix = "nxl_toggleableLayout:nxw_toggleableLayoutWidget_initialForm:";
        String editIdPrefix = "nxl_toggleableLayout:nxw_toggleableLayoutWidget_toggledForm:";

        assertEquals("My title", driver.findElement(By.id(viewIdPrefix + "nxl_heading:nxw_title")).getText());
        assertEquals("My description",
                driver.findElement(By.id(viewIdPrefix + "nxl_heading:nxw_description")).getText());

        toggleToEdit();

        // check cancel actions in both header and form
        WebElement action = checkToggleButton(false);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        action.click();
        arm.waitForAjaxRequests();

        checkToggleButton(true);
        toggleToEdit();
        String buttonId = editIdPrefix + "nxw_toggleableLayoutWidget_cancelToggleAction";
        action = driver.findElement(By.id(buttonId));
        assertEquals("Cancel", action.getAttribute("value"));
        arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        action.click();
        arm.waitForAjaxRequests();

        checkToggleButton(true);
        toggleToEdit();

        // check toggled form submit
        saveToggleLayout(editIdPrefix);
        checkToggleButton(false);
        Locator.waitForTextPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertEquals(VALUE_REQUIRED,
                driver.findElement(By.id(editIdPrefix + "nxl_demoToggleableLayout:nxw_htmlWidget_message")).getText());
        assertEquals(
                VALUE_REQUIRED,
                driver.findElement(By.id(editIdPrefix + "nxl_demoToggleableLayout:nxw_datetimeWidget_message")).getText());
        assertEquals(VALUE_REQUIRED,
                driver.findElement(By.id(editIdPrefix + "nxl_demoToggleableLayout:nxw_intWidget_message")).getText());

        LayoutElement edit = new LayoutElement(driver, editIdPrefix);
        edit.getWidget("nxl_heading_1:nxw_title_1").setInputValue("My title changed");
        edit.getWidget("nxl_heading_1:nxw_description_1").setInputValue("My description changed");
        edit.getWidget("nxl_demoToggleableLayout:nxw_htmlWidget", RichEditorElement.class).setInputValue(
                "<b>Bold</b> content");
        edit.getWidget("nxl_demoToggleableLayout:nxw_datetimeWidget", DateWidgetElement.class).setInputValue(
                "09/7/2010 03:14 PM");
        edit.getWidget("nxl_demoToggleableLayout:nxw_intWidget").setInputValue("42");

        saveToggleLayout(editIdPrefix);
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        assertEquals("My title changed", driver.findElement(By.id(viewIdPrefix + "nxl_heading:nxw_title")).getText());
        assertEquals("My description changed",
                driver.findElement(By.id(viewIdPrefix + "nxl_heading:nxw_description")).getText());

        navigateTo(pageId);
        assertEquals("My title", driver.findElement(By.id(viewIdPrefix + "nxl_heading:nxw_title")).getText());
        assertEquals("My description",
                driver.findElement(By.id(viewIdPrefix + "nxl_heading:nxw_description")).getText());
    }

    protected WebElement checkToggleButton(boolean isEdit) {
        String buttonId = "nxl_toggleableLayout:nxw_toggleableLayoutWidget_headerForm:nxw_toggleableLayoutWidget_header_toggleAction";
        WebElement action = driver.findElement(By.id(buttonId));
        if (isEdit) {
            assertEquals("Edit", action.getText());
        } else {
            assertEquals("Cancel", action.getText());
        }
        return action;
    }

    protected void toggleToEdit() {
        WebElement action = checkToggleButton(true);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        action.click();
        arm.waitForAjaxRequests();
    }

    protected void saveToggleLayout(String editIdPrefix) {
        String buttonId = editIdPrefix + "nxw_toggleableLayoutWidget_saveDemoToggleableLayout";
        WebElement action = driver.findElement(By.id(buttonId));
        assertEquals("Save", action.getAttribute("value"));
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        action.click();
        arm.waitForAjaxRequests();
    }
}