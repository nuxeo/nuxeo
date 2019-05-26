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
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.forms.SelectManyDirectoryWidgetElement;
import org.nuxeo.functionaltests.forms.WidgetElement;

/**
 * @since 7.4
 */
public class ITSelectManyDirectoryWidgetTest extends AbstractWidgetPageTest {

    public ITSelectManyDirectoryWidgetTest() {
        super("selectManyDirectoryWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals("", getViewWidget(SelectManyDirectoryWidgetElement.class).getValue(false));
        checkValueRequired(false);
        submitDemo();
        checkValueRequired(true);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        getEditWidget(SelectManyDirectoryWidgetElement.class).setInputValue("Stan Marsh");
        arm.waitForAjaxRequests();
        arm.watchAjaxRequests();
        getEditWidget(SelectManyDirectoryWidgetElement.class).setInputValue("Eric Cartman");
        arm.waitForAjaxRequests();
        submitDemo();
        checkValueRequired(false);
        assertEquals("Eric Cartman\nStan Marsh", getViewWidget(SelectManyDirectoryWidgetElement.class).getValue(false));
        navigateTo(pageId);
        assertEquals("", getViewWidget(SelectManyDirectoryWidgetElement.class).getValue(false));
        assertEquals("", getEditWidget(SelectManyDirectoryWidgetElement.class).getValue(true));
    }

    @Override
    protected WidgetElement getViewWidget(Class<? extends WidgetElement> widgetClassToProxy) {
        LayoutElement view = new LayoutElement(driver, pageId + "Layout_view_form:nxl_" + pageId + "Layout_1");
        WidgetElement w = view.getWidget(pageId, widgetClassToProxy);
        return w;
    }

}
