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
import org.nuxeo.functionaltests.forms.CheckboxWidgetElement;

/**
 * @since 7.4
 */
public class ITCheckboxWidgetTest extends AbstractWidgetPageTest {

    public ITCheckboxWidgetTest() {
        super("checkboxWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals("No", getViewWidget(CheckboxWidgetElement.class).getValue(false));
        submitDemo();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        getEditWidget(CheckboxWidgetElement.class).setInputValue("true");
        arm.waitForAjaxRequests();
        submitDemo();
        assertEquals("Yes", getViewWidget(CheckboxWidgetElement.class).getValue(false));
        arm.watchAjaxRequests();
        getEditWidget(CheckboxWidgetElement.class).setInputValue("false");
        arm.waitForAjaxRequests();
        submitDemo();
        assertEquals("No", getViewWidget(CheckboxWidgetElement.class).getValue(false));
        navigateTo(pageId);
        assertEquals("No", getViewWidget(CheckboxWidgetElement.class).getValue(false));
        assertEquals("false", getEditWidget(CheckboxWidgetElement.class).getValue(true));
    }

}
