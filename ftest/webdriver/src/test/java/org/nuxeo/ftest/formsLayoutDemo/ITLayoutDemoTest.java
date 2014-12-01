/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 *
 */

package org.nuxeo.ftest.formsLayoutDemo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.forms.ListWidgetElement;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets.ListStandardWidgetPage;

/**
 * Tests layout demo.
 *
 * @since 7.1
 */
public class ITLayoutDemoTest extends AbstractTest {

    private final static String DUMMY_HTML_TEXT_CONTENT_1 = "dummy content";

    private final static String S2_SELECTION_1 = "France";

    private final static String DUMMY_HTML_TEXT_CONTENT_2 = "dummy content 2";

    private final static String S2_SELECTION_2 = "Germany";

    private final static String S2_PREFIX = "s2id_";

    @Test
    public void testListWidgets() {

        ListStandardWidgetPage listStandardWidgetPage = get(NUXEO_URL
                + "/layoutDemo/listWidget", ListStandardWidgetPage.class);

        // Test select2 + html text (tiny_mce) in complex list widget
        ListWidgetElement s2HtmlTextComplexListEditWidget = listStandardWidgetPage.getS2HtmlTextComplexListEditWidget();
        assertNotNull(s2HtmlTextComplexListEditWidget);
        s2HtmlTextComplexListEditWidget.addNewElement();

        Select2WidgetElement select2WidgetElement = s2HtmlTextComplexListEditWidget.getSubWidget(
                "nxw_suggest_select2", 0, Select2WidgetElement.class, true);
        // TODO fix getSubWidget, I have to reload the select2 somehow
        select2WidgetElement = new Select2WidgetElement(driver,
                S2_PREFIX + select2WidgetElement.getId());
        select2WidgetElement.selectValue(S2_SELECTION_1, true);
        RichEditorElement richEditorElement = s2HtmlTextComplexListEditWidget.getSubWidget(
                "nxw_htmlTextItem", 0, RichEditorElement.class, true);
        richEditorElement.insertContent(DUMMY_HTML_TEXT_CONTENT_1);

        s2HtmlTextComplexListEditWidget.addNewElement();
        select2WidgetElement = s2HtmlTextComplexListEditWidget.getSubWidget(
                "nxw_suggest_select2", 1, Select2WidgetElement.class, true);
        // TODO fix getSubWidget, I have to reload the select2 somehow
        select2WidgetElement = new Select2WidgetElement(driver,
                S2_PREFIX + select2WidgetElement.getId());
        select2WidgetElement.selectValue(S2_SELECTION_2, true);

        richEditorElement = s2HtmlTextComplexListEditWidget.getSubWidget(
                "nxw_htmlTextItem", 1, RichEditorElement.class, true);
        richEditorElement.insertContent(DUMMY_HTML_TEXT_CONTENT_2);

        listStandardWidgetPage.submitS2HtmlTextComplexListWidget();

        ListWidgetElement s2HtmlTextComplexListViewWidget = listStandardWidgetPage.getS2HtmlTextComplexListViewWidget();
        select2WidgetElement = s2HtmlTextComplexListViewWidget.getSubWidget(
                "nxw_suggest_1_select2", 0, Select2WidgetElement.class, true);
        select2WidgetElement = new Select2WidgetElement(driver,
                S2_PREFIX + select2WidgetElement.getId());
        assertEquals("Europe/" + S2_SELECTION_1,
                select2WidgetElement.getSelectedValue().getText());
        WidgetElement we = s2HtmlTextComplexListViewWidget.getSubWidget(
                "nxw_htmlTextItem_1", 0, true);
        assertEquals(DUMMY_HTML_TEXT_CONTENT_1, we.getValue(false));
        select2WidgetElement = s2HtmlTextComplexListViewWidget.getSubWidget(
                "nxw_suggest_1_select2", 1, Select2WidgetElement.class, true);
        select2WidgetElement = new Select2WidgetElement(driver,
                S2_PREFIX + select2WidgetElement.getId());
        assertEquals("Europe/" + S2_SELECTION_2,
                select2WidgetElement.getSelectedValue().getText());
        we = s2HtmlTextComplexListViewWidget.getSubWidget("nxw_htmlTextItem_1",
                1, true);
        assertEquals(DUMMY_HTML_TEXT_CONTENT_2, we.getValue(false));

    }

}
