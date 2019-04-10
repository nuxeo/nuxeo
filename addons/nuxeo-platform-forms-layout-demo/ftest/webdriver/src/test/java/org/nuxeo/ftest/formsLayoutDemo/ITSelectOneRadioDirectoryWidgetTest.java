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
import org.nuxeo.functionaltests.forms.SelectOneRadioDirectoryWidgetElement;
import org.openqa.selenium.By;

/**
 * @since 7.4
 */
public class ITSelectOneRadioDirectoryWidgetTest extends AbstractWidgetPageTest {

    public ITSelectOneRadioDirectoryWidgetTest() {
        super("selectOneRadioDirectoryWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals(
                "",
                driver.findElement(
                        By.xpath("//form[@id='selectOneRadioDirectoryWidgetLayout_view_form']/table/tbody/tr/td[2]")).getText());
        checkValueRequired(false);
        submitDemo();
        checkValueRequired(true);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        getEditWidget(SelectOneRadioDirectoryWidgetElement.class).setInputValue("cartman");
        arm.waitForAjaxRequests();
        submitDemo();
        checkValueRequired(false);
        assertEquals(
                "Eric Cartman",
                driver.findElement(
                        By.xpath("//form[@id='selectOneRadioDirectoryWidgetLayout_view_form']/table/tbody/tr/td[2]")).getText());
        navigateTo(pageId);
        assertEquals(
                "",
                driver.findElement(
                        By.xpath("//form[@id='selectOneRadioDirectoryWidgetLayout_view_form']/table/tbody/tr/td[2]")).getText());
        assertEquals("", getEditWidget(SelectOneRadioDirectoryWidgetElement.class).getValue(true));
    }

}
