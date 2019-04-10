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
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        getEditWidget().setInputValue("test");
        arm.end();
        submitDemo();
        checkValueRequired(false);
        assertEquals("'test' is not a number. Example: 98765432.", getEditWidgetMessage());
        arm.begin();
        getEditWidget().setInputValue("3");
        arm.end();
        submitDemo();
        checkValueRequired(false);
        assertEquals("", getEditWidgetMessage());
        assertEquals("3", getViewWidget().getValue(false));
        navigateTo(pageId);
        assertEquals("", getViewWidget().getValue(false));
        assertEquals("", getEditWidget().getValue(true));
    }

}
