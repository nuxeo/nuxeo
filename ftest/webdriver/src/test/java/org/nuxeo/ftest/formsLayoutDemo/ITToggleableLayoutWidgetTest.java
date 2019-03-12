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
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.DateWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
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
        Locator.waitUntilEnabledAndClick(action);
        arm.waitForAjaxRequests();

        checkToggleButton(true);
        toggleToEdit();

        // check toggled form submit
        saveToggleLayout(editIdPrefix);
        checkToggleButton(false);
        Locator.waitForTextPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);
        assertEquals(VALUE_REQUIRED,
                driver.findElement(By.id(editIdPrefix + "nxl_demoToggleableLayout:nxw_htmlWidget_message")).getText());
        assertEquals(VALUE_REQUIRED, driver.findElement(
                By.id(editIdPrefix + "nxl_demoToggleableLayout:nxw_datetimeWidget_message")).getText());
        assertEquals(VALUE_REQUIRED,
                driver.findElement(By.id(editIdPrefix + "nxl_demoToggleableLayout:nxw_intWidget_message")).getText());

        LayoutElement edit = new LayoutElement(driver, editIdPrefix);
        edit.getWidget("nxl_heading_1:nxw_title_1").setInputValue("My title changed");
        edit.getWidget("nxl_heading_1:nxw_description_1").setInputValue("My description changed");
        edit.getWidget("nxl_demoToggleableLayout:nxw_htmlWidget", RichEditorElement.class).setInputValue(
                "<b>Bold</b> content");
        edit.getWidget("nxl_demoToggleableLayout:nxw_datetimeWidget", DateWidgetElement.class).setInputValue(
                "09/7/2010, 03:14 PM");
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

    /**
     * Non regression test for toggleable layout as defined on Studio (holding an expression as initial layout name).
     *
     * @since 7.10
     */
    @Test
    public void testStudioWidget() {
        navigateTo(pageId);
        checkNoError();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        Select2WidgetElement viewSuggest = new Select2WidgetElement(driver,
                "nxl_toggleableLayoutStudio:nxw_toggleableLayoutStudioWidget_initialForm:nxl_demoToggleableLayout_view:nxw_l10ncoverage_select2");
        assertEquals("Europe/France", viewSuggest.getText());

        String toggleButtonId = "nxl_toggleableLayoutStudio:nxw_toggleableLayoutStudioWidget_headerForm:nxw_toggleableLayoutStudioWidget_header_toggleAction";
        WebElement toggleAction = driver.findElement(By.id(toggleButtonId));
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        Locator.waitUntilEnabledAndClick(toggleAction);
        arm.waitForAjaxRequests();

        // check toggled form submit
        String editIdPrefix = "nxl_toggleableLayoutStudio:nxw_toggleableLayoutStudioWidget_toggledForm:";
        LayoutElement edit = new LayoutElement(driver, editIdPrefix);
        Select2WidgetElement suggest = new Select2WidgetElement(driver,
                "s2id_nxl_toggleableLayoutStudio:nxw_toggleableLayoutStudioWidget_toggledForm:nxl_demoToggleableLayout_edit:nxw_l10ncoverage_1_select2");
        suggest.selectValue("Germany", true);
        edit.getWidget("nxl_demoToggleableLayout_edit:nxw_datetimeWidget_2", DateWidgetElement.class).setInputValue(
                "09/7/2010, 03:14 PM");
        edit.getWidget("nxl_demoToggleableLayout_edit:nxw_intWidget_2").setInputValue("42");

        String saveButtonId = editIdPrefix + "nxw_toggleableLayoutStudioWidget_saveDemoToggleableLayout";
        WebElement saveAction = driver.findElement(By.id(saveButtonId));
        assertEquals("Save", saveAction.getAttribute("value"));
        arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        Locator.scrollAndForceClick(saveAction);
        arm.waitForAjaxRequests();
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), VALUE_REQUIRED);

        viewSuggest = new Select2WidgetElement(driver,
                "nxl_toggleableLayoutStudio:nxw_toggleableLayoutStudioWidget_initialForm:nxl_demoToggleableLayout_view:nxw_l10ncoverage_select2");
        assertEquals("Europe/Germany", viewSuggest.getText());

        navigateTo(pageId);
        viewSuggest = new Select2WidgetElement(driver,
                "nxl_toggleableLayoutStudio:nxw_toggleableLayoutStudioWidget_initialForm:nxl_demoToggleableLayout_view:nxw_l10ncoverage_select2");
        assertEquals("Europe/France", viewSuggest.getText());
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
        Locator.waitUntilEnabledAndClick(action);
        arm.waitForAjaxRequests();
    }

    protected void saveToggleLayout(String editIdPrefix) {
        String buttonId = editIdPrefix + "nxw_toggleableLayoutWidget_saveDemoToggleableLayout";
        WebElement action = driver.findElement(By.id(buttonId));
        assertEquals("Save", action.getAttribute("value"));
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        Locator.waitUntilEnabledAndClick(action);
        arm.waitForAjaxRequests();
    }
}
