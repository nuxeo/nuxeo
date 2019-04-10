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