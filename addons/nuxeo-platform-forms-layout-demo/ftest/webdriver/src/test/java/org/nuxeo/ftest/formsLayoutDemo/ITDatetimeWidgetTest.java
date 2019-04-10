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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.forms.DateWidgetElement;

/**
 * @since 7.4
 */
public class ITDatetimeWidgetTest extends AbstractWidgetPageTest {

    public ITDatetimeWidgetTest() {
        super("datetimeWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals("", getViewWidget(DateWidgetElement.class).getValue(false));
        checkValueRequired(false);
        submitDemo();
        checkValueRequired(true);
        getEditWidget(DateWidgetElement.class).setInputValue("test");
        submitDemo();
        checkValueRequired(false);
        assertTrue(getEditWidgetMessage().startsWith("'test' could not be understood as a date. Example:"));
        getEditWidget(DateWidgetElement.class).setInputValue("09/7/2010 03:14 PM");
        submitDemo();
        checkValueRequired(false);
        assertEquals("", getEditWidgetMessage());
        assertEquals("9/7/2010", getViewWidget(DateWidgetElement.class).getValue(false));
        navigateTo(pageId);
        assertEquals("", getViewWidget(DateWidgetElement.class).getValue(false));
        assertEquals("", getEditWidget(DateWidgetElement.class).getValue(true));
    }

}