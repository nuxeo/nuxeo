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
        getEditWidget(DateWidgetElement.class).setInputValue("09/7/2010, 03:14 PM");
        submitDemo();
        checkValueRequired(false);
        assertEquals("", getEditWidgetMessage());
        assertEquals("9/7/2010", getViewWidget(DateWidgetElement.class).getValue(false));
        navigateTo(pageId);
        assertEquals("", getViewWidget(DateWidgetElement.class).getValue(false));
        assertEquals("", getEditWidget(DateWidgetElement.class).getValue(true));
    }

}
