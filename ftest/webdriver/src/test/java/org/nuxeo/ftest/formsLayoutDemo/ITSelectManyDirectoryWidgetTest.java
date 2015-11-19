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