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
 *     Nelson Silva <nsilva@nuxeo.com>
 */

package org.nuxeo.ftest.formsLayoutDemo;

import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.forms.ListWidgetElement;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets.ListStandardWidgetPage;
import org.openqa.selenium.support.ui.Select;

/**
 * List Widget Tests.
 *
 * @since 7.2
 */
public class ITJSListWidgetTest extends AbstractTest {

    private final static String DUMMY_HTML_TEXT_CONTENT_1 = "dummy content";

    private final static String S2_SELECTION_1 = "France";

    private final static String DUMMY_HTML_TEXT_CONTENT_2 = "dummy content 2";

    private final static String S2_SELECTION_2 = "Germany";

    private final static String S2_PREFIX = "s2id_";

    private final static String VALUE_REQUIRED = "Value is required";

    ListStandardWidgetPage page;

    @Before
    public void setUp() {
        driver.get(NUXEO_URL + "/layoutDemo");
        page = get(NUXEO_URL + "/layoutDemo/listWidget", ListStandardWidgetPage.class);
        page.goToOverviewTab();
    }

    @Test
    public void testListArrayWidget() {
        ListWidgetElement listWidget = page.getListArrayEditWidget();
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals(0, listWidget.getRows().size());

        listWidget.addNewElement();
        assertEquals(1, listWidget.getRows().size());
        listWidget = page.submitListArrayWidget();
        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_listItem", 0, 1));

        listWidget.getSubWidget("nxw_listItem", 0).setInputValue("test");
        listWidget.addNewElement();
        assertEquals("test", listWidget.getSubWidget("nxw_listItem", 0).getInputValue());
        listWidget.getSubWidget("nxw_listItem", 1).setInputValue("test 2");

        listWidget = page.submitListArrayWidget();
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals("test", listWidget.getSubWidget("nxw_listItem", 0).getInputValue());
        assertEquals("test 2", listWidget.getSubWidget("nxw_listItem", 1).getInputValue());

        listWidget.removeElement(1);
        listWidget = page.submitListArrayWidget();
        assertEquals("test", listWidget.getSubWidget("nxw_listItem", 0).getInputValue());
        assertEquals(1, listWidget.getRows().size());
    }

    @Test
    public void testListWidget() {
        // Edit mode
        ListWidgetElement listWidget = page.getListEditWidget();
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals(0, listWidget.getRows().size());

        listWidget.addNewElement();
        assertEquals(1, listWidget.getRows().size());
        listWidget = page.submitListWidget();
        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_listItem", 0, 3));

        listWidget.getSubWidget("nxw_listItem_2", 0).setInputValue("test");
        listWidget.addNewElement();
        assertEquals("test", listWidget.getSubWidget("nxw_listItem_2", 0).getInputValue());
        listWidget.getSubWidget("nxw_listItem_2", 1).setInputValue("test 2");

        page.submitListWidget();

        // View mode
        listWidget = page.getListViewWidget();
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals("test", listWidget.getSubWidget("nxw_listItem_3", 0).getOutputValue());
        assertEquals("test 2", listWidget.getSubWidget("nxw_listItem_3", 1).getOutputValue());

        // remove
        page.getListEditWidget().removeElement(1);
        page.submitListWidget();

        listWidget = page.getListViewWidget();
        assertEquals("test", listWidget.getSubWidget("nxw_listItem_3", 0).getOutputValue());
        assertEquals(1, listWidget.getRows().size());
    }

    @Test
    public void testComplexListWidget() {
        ListWidgetElement listWidget = page.getComplexListEditWidget();
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());

        listWidget = page.submitComplexListWidget();
        assertEquals(VALUE_REQUIRED, listWidget.getMessageValue());

        listWidget.addNewElement();
        assertEquals(1, listWidget.getRows().size());
        assertNotNull(listWidget.getSubWidget("nxw_stringComplexItem", 0));
        assertNotNull(listWidget.getSubWidget("nxw_text", 0));
        listWidget.getSubWidget("nxw_stringComplexItem", 0).setInputValue("test");
        listWidget.getSubWidget("nxw_dateComplexItemInputDate", 0).setInputValue("09/7/2010 03:14 PM");
        listWidget.getSubWidget("nxw_intComplexItem", 0).setInputValue("lala");
        listWidget.getSubWidget("nxw_text", 0).setInputValue("field 1");
        listWidget.getSubWidget("nxw_text_1", 0).setInputValue("field 2");

        listWidget.addNewElement();

        assertNotNull(listWidget.getSubWidget("nxw_stringComplexItem", 1));
        listWidget.getSubWidget("nxw_stringComplexItem", 1).setInputValue("test 2");
        listWidget.getSubWidget("nxw_text", 1).setInputValue("field 3");
        listWidget.getSubWidget("nxw_text_1", 1).setInputValue("field 4");

        listWidget = page.submitComplexListWidget();

        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals("'lala' is not a number. Example: 99",
                listWidget.getSubWidgetMessageValue("nxw_intComplexItem", 0, 1));

        listWidget.getSubWidget("nxw_intComplexItem", 0).setInputValue("3");
        listWidget = page.submitComplexListWidget();
        assertThat(listWidget.getSubWidgetMessageValue("nxw_intComplexItem", 0, 1), isEmptyString());

        // View mode
        listWidget = page.getComplexListViewWidget();
        assertEquals("test", listWidget.getSubWidget("nxw_stringComplexItem_1", 0).getOutputValue());
        assertEquals("9/7/2010", listWidget.getSubWidget("nxw_dateComplexItem_1", 0).getOutputValue());
        assertEquals("3", listWidget.getSubWidget("nxw_intComplexItem_1", 0).getOutputValue());
        assertEquals("No", listWidget.getSubWidget("nxw_booleanComplexItem_1", 0).getOutputValue());
        assertEquals("field 1", listWidget.getSubWidget("nxw_text_2", 0).getOutputValue());
        assertEquals("field 2", listWidget.getSubWidget("nxw_text_3", 0).getOutputValue());

        assertEquals("test 2", listWidget.getSubWidget("nxw_stringComplexItem_1", 1).getOutputValue());
        assertThat(listWidget.getSubWidget("nxw_dateComplexItem_1", 1).getOutputValue(), isEmptyString());
        assertThat(listWidget.getSubWidget("nxw_intComplexItem_1", 1).getOutputValue(), isEmptyString());
        assertEquals("No", listWidget.getSubWidget("nxw_booleanComplexItem_1", 1).getOutputValue());
        assertEquals("field 3", listWidget.getSubWidget("nxw_text_2", 1).getOutputValue());
        assertEquals("field 4", listWidget.getSubWidget("nxw_text_3", 1).getOutputValue());

        page.getComplexListEditWidget().removeElement(1);
        listWidget = page.submitComplexListWidget();

        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());

        // View mode
        listWidget = page.getComplexListViewWidget();
        assertEquals("test", listWidget.getSubWidget("nxw_stringComplexItem_1", 0).getOutputValue());
        assertEquals("9/7/2010", listWidget.getSubWidget("nxw_dateComplexItem_1", 0).getOutputValue());
        assertEquals("3", listWidget.getSubWidget("nxw_intComplexItem_1", 0).getOutputValue());
        assertEquals("No", listWidget.getSubWidget("nxw_booleanComplexItem_1", 0).getOutputValue());
        assertEquals("field 1", listWidget.getSubWidget("nxw_text_2", 0).getOutputValue());
        assertEquals("field 2", listWidget.getSubWidget("nxw_text_3", 0).getOutputValue());
    }

    @Test
    public void testComplexList2Widget() {
        // Test select2 + html text (tiny_mce) in complex list widget
        ListWidgetElement listWidget = page.getS2HtmlTextComplexListEditWidget();
        assertNotNull(listWidget);
        listWidget.addNewElement();

        Select2WidgetElement select2WidgetElement = listWidget.getSubWidget("nxw_suggest_select2", 0,
                Select2WidgetElement.class, true);
        select2WidgetElement = new Select2WidgetElement(driver, S2_PREFIX + select2WidgetElement.getId());
        select2WidgetElement.selectValue(S2_SELECTION_1, true);
        RichEditorElement richEditorElement = listWidget.getSubWidget("nxw_htmlTextItem", 0, RichEditorElement.class,
                true);
        richEditorElement.insertContent(DUMMY_HTML_TEXT_CONTENT_1);

        listWidget.addNewElement();
        assertEquals(2, listWidget.getRows().size());

        select2WidgetElement = listWidget.getSubWidget("nxw_suggest_select2", 1, Select2WidgetElement.class, true);
        select2WidgetElement = new Select2WidgetElement(driver, S2_PREFIX + select2WidgetElement.getId());
        select2WidgetElement.selectValue(S2_SELECTION_2, true);

        richEditorElement = listWidget.getSubWidget("nxw_htmlTextItem", 1, RichEditorElement.class, true);
        richEditorElement.insertContent(DUMMY_HTML_TEXT_CONTENT_2);

        page.submitS2HtmlTextComplexListWidget();

        // View mode
        listWidget = page.getS2HtmlTextComplexListViewWidget();
        select2WidgetElement = listWidget.getSubWidget("nxw_suggest_1_select2", 0, Select2WidgetElement.class, true);
        select2WidgetElement = new Select2WidgetElement(driver, S2_PREFIX + select2WidgetElement.getId());
        assertEquals("Europe/" + S2_SELECTION_1, select2WidgetElement.getSelectedValue().getText());
        WidgetElement we = listWidget.getSubWidget("nxw_htmlTextItem_1", 0, true);
        assertEquals(DUMMY_HTML_TEXT_CONTENT_1, we.getValue(false));

        select2WidgetElement = listWidget.getSubWidget("nxw_suggest_1_select2", 1, Select2WidgetElement.class, true);
        select2WidgetElement = new Select2WidgetElement(driver, S2_PREFIX + select2WidgetElement.getId());
        assertEquals("Europe/" + S2_SELECTION_2, select2WidgetElement.getSelectedValue().getText());
        we = listWidget.getSubWidget("nxw_htmlTextItem_1", 1, true);
        assertEquals(DUMMY_HTML_TEXT_CONTENT_2, we.getValue(false));
    }

    @Test
    public void testListOfListsWidget() {
        ListWidgetElement listWidget = page.getListOfListsEditWidget();
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals(0, listWidget.getRows().size());

        listWidget.addNewElement();

        WidgetElement stringItem = listWidget.getSubWidget("nxw_stringItem", 0);
        assertNotNull(stringItem);
        stringItem.setInputValue("test");

        listWidget = page.submitListOfListsWidget();

        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0, 1));

        WidgetElement stringArrayItem = listWidget.getSubWidget("nxw_stringArrayItem", 0, WidgetElement.class, false);
        new Select(stringArrayItem.getInputElement()).selectByValue("cartman");

        listWidget = page.submitListOfListsWidget();

        assertThat(listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0, 1), isEmptyString());

        ListWidgetElement stringListItem = listWidget.getSubWidget("nxw_stringListItem", 0, ListWidgetElement.class,
                false);
        stringListItem.addNewElement();
        stringListItem.getSubWidget("nxw_stringListSubItem", 0).setInputValue("test sublist");
        stringListItem.addNewElement();
        stringListItem.getSubWidget("nxw_stringListSubItem", 1).setInputValue("test sublist 2");

        // non regression tests for NXP-16406
        ListWidgetElement stringListItem2 = listWidget.getSubWidget("nxw_stringListItem2", 0, ListWidgetElement.class,
                false);
        stringListItem2.addNewElement();
        stringListItem2.getSubWidget("nxw_stringListSubItem2", 0).setInputValue("test sublist bis");
        stringListItem2.addNewElement();
        stringListItem2.getSubWidget("nxw_stringListSubItem2", 1).setInputValue("test sublist 2 bis");

        page.submitListOfListsWidget();

        // View mode
        listWidget = page.getListOfListsViewWidget();

        assertEquals("test", listWidget.getSubWidget("nxw_stringItem_1", 0).getOutputValue());

        stringArrayItem = listWidget.getWidget(listWidget.getListElementId() + ":stringArrayItem_1",
                WidgetElement.class);
        assertEquals("Eric Cartman", stringArrayItem.getOutputValue());

        stringListItem = listWidget.getSubWidget("nxw_stringListItem_1", 0, ListWidgetElement.class, false);
        assertEquals("test sublist", stringListItem.getSubWidget("nxw_stringListSubItem_1", 0).getOutputValue());
        assertEquals("test sublist 2", stringListItem.getSubWidget("nxw_stringListSubItem_1", 1).getOutputValue());

        stringListItem2 = listWidget.getSubWidget("nxw_stringListItem2_1", 0, ListWidgetElement.class, false);
        assertEquals("test sublist bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 0).getOutputValue());
        assertEquals("test sublist 2 bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 1).getOutputValue());

        // delete 2nd row in stringListItem
        listWidget = page.getListOfListsEditWidget();
        stringListItem = listWidget.getSubWidget("nxw_stringListItem", 0, ListWidgetElement.class, false);
        stringListItem.removeElement(1);
        assertEquals(1, stringListItem.getRows().size());

        page.submitListOfListsWidget();

        listWidget = page.getListOfListsViewWidget();

        assertEquals("test", listWidget.getSubWidget("nxw_stringItem_1", 0).getOutputValue());

        stringArrayItem = listWidget.getWidget(listWidget.getListElementId() + ":stringArrayItem_1",
                WidgetElement.class);
        assertEquals("Eric Cartman", stringArrayItem.getOutputValue());
        assertEquals(1, stringListItem.getRows().size());

        stringListItem = listWidget.getSubWidget("nxw_stringListItem_1", 0, ListWidgetElement.class, false);
        assertEquals("test sublist", stringListItem.getSubWidget("nxw_stringListSubItem_1", 0).getOutputValue());

        stringListItem2 = listWidget.getSubWidget("nxw_stringListItem2_1", 0, ListWidgetElement.class, false);
        assertEquals("test sublist bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 0).getOutputValue());
        assertEquals("test sublist 2 bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 1).getOutputValue());

        // non regression tests for NXP-5576
        listWidget = page.getListOfListsEditWidget();

        listWidget.addNewElement();
        assertEquals(2, listWidget.getRows().size());
        assertNotNull(listWidget.getSubWidget("nxw_stringItem", 1));
    }

    // non regression tests for NXP-6933
    @Test
    public void testNonRegression_NXP_6933() {
        ListWidgetElement listWidget = page.getListArrayEditWidget();

        listWidget.addNewElement();
        WidgetElement listItem0 = listWidget.getSubWidget("nxw_listItem", 0);
        assertNotNull(listItem0);
        listItem0.setInputValue("AAA");

        listWidget.addNewElement();
        WidgetElement listItem1 = listWidget.getSubWidget("nxw_listItem", 1);
        assertNotNull(listItem1);
        listItem1.setInputValue("BBB");

        assertEquals(2, listWidget.getRows().size());

        listWidget.removeElement(0);
        assertEquals(1, listWidget.getRows().size());

        page.submitListArrayWidget();

        assertEquals("BBB", page.getListArrayViewWidget().getSubWidget("nxw_listItem_1", 0).getOutputValue());
        assertEquals("BBB", page.getListArrayEditWidget().getSubWidget("nxw_listItem", 0).getInputValue());
    }

    /**
     * Check that removed elements are not validated.
     *
     * @since 7.2
     */
    @Test
    public void testRemovedElementValidation() {
        ListWidgetElement listWidget = page.getListOfListsEditWidget();
        assertNotNull(listWidget);
        // add 2 elements
        listWidget.addNewElement();
        listWidget.addNewElement();

        // only fill the second element required widget
        WidgetElement stringArrayItem = listWidget.getSubWidget("nxw_stringArrayItem", 1, WidgetElement.class, false);
        new Select(stringArrayItem.getInputElement()).selectByValue("cartman");

        // remove first element
        listWidget.removeElement(0);

        // submit => there should not be any validation error
        listWidget = page.submitListOfListsWidget();

        assertNotEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0, 1));
    }

    /**
     * Check moved elements validation.
     *
     * @since 7.2
     */
    @Test
    public void testMovedElementValidation() {
        ListWidgetElement listWidget = page.getListOfListsEditWidget();
        assertNotNull(listWidget);
        // add 3 elements
        listWidget.addNewElement();
        listWidget.addNewElement();
        listWidget.addNewElement();

        // only fill the second element required widget
        WidgetElement stringArrayItem = listWidget.getSubWidget("nxw_stringArrayItem", 1, WidgetElement.class, false);
        new Select(stringArrayItem.getInputElement()).selectByValue("cartman");

        // move it to first place
        listWidget.moveUpElement(1);

        // remove now second element
        listWidget.removeElement(1);

        // submit => there should not be a validation error on second item
        listWidget = page.submitListOfListsWidget();

        assertNotEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0, 1));
        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 1, 1));
    }

}
