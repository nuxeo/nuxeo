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
import org.nuxeo.functionaltests.forms.RichEditorElement;

/**
 * @since 7.4
 */
public class ITHTMLTextWidgetTest extends AbstractWidgetPageTest {

    public ITHTMLTextWidgetTest() {
        super("htmltextWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals("", getViewWidget(RichEditorElement.class).getValue(false));
        checkValueRequired(false);
        submitDemo();
        checkValueRequired(true);
        getEditWidget(RichEditorElement.class).setInputValue("<b>Bold</b> content");
        submitDemo();
        checkValueRequired(false);
        assertEquals("Bold content", getViewWidget(RichEditorElement.class).getValue(false));
        navigateTo(pageId);
        assertEquals("", getViewWidget(RichEditorElement.class).getValue(false));
        assertEquals("", getEditWidget(RichEditorElement.class).getValue(true));
    }

}
