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

/**
 * @since 7.4
 */
public class ITIntWidgetTest extends AbstractWidgetPageTest {

    public ITIntWidgetTest() {
        super("intWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals("", getViewWidget().getValue(false));
        checkValueRequired(false);
        submitDemo();
        checkValueRequired(true);
        getEditWidget().setInputValue("test");
        submitDemo();
        checkValueRequired(false);
        assertEquals("'test' is not a number. Example: 99.", getEditWidgetMessage());
        getEditWidget().setInputValue("3");
        submitDemo();
        checkValueRequired(false);
        assertEquals("", getEditWidgetMessage());
        assertEquals("3", getViewWidget().getValue(false));
        navigateTo(pageId);
        assertEquals("", getViewWidget().getValue(false));
        assertEquals("", getEditWidget().getValue(true));
    }

}