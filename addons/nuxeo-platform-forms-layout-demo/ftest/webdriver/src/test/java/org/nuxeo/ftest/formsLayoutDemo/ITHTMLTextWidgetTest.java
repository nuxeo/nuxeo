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