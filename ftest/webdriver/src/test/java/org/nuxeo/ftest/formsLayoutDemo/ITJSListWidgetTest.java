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
 *     Nelson Silva <nsilva@nuxeo.com>
 */

package org.nuxeo.ftest.formsLayoutDemo;

import static org.hamcrest.text.IsEmptyString.isEmptyString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.forms.JSListWidgetElement;
import org.nuxeo.functionaltests.forms.JSListWidgetElement.Display;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.ValidationPage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets.ListStandardWidgetPage;
import org.openqa.selenium.support.ui.Select;

/**
 * List Widget Tests.
 *
 * @since 7.2
 */
@RunWith(Parameterized.class)
public class ITJSListWidgetTest extends AbstractTest {

    private final static String DUMMY_HTML_TEXT_CONTENT_1 = "dummy content";

    private final static String S2_SELECTION_1 = "France";

    private final static String DUMMY_HTML_TEXT_CONTENT_2 = "dummy content 2";

    private final static String S2_SELECTION_2 = "Germany";

    private final static String S2_PREFIX = "s2id_";

    private final static String VALUE_REQUIRED = ValidationPage.VALUE_REQUIRED;

    ListStandardWidgetPage page;

    private Display display;

    @Parameter
    public String displayParam;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "BLOCK_LEFT" }, { "BLOCK_TOP" }, { "TABLE" }, { "INLINE" }
        });
    }


    @Before
    public void setUp() {
        driver.get(HomePage.URL);
        page = get(HomePage.URL + "listWidget?display=" + displayParam.toLowerCase(), ListStandardWidgetPage.class);
        page.goToOverviewTab();
        display = Display.valueOf(displayParam);
    }

    @Test
    public void testListArrayWidget() {
        JSListWidgetElement listWidget = page.getListArrayEditWidget(display);
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals(0, listWidget.getRows().size());

        listWidget.addNewElement();
        assertEquals(1, listWidget.getRows().size());
        listWidget = page.submitListArrayWidget(display);
        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_listItem", 0));

        listWidget.getSubWidget("nxw_listItem", 0).setInputValue("test");
        listWidget.addNewElement();
        assertEquals("test", listWidget.getSubWidget("nxw_listItem", 0).getInputValue());
        listWidget.getSubWidget("nxw_listItem", 1).setInputValue("test 2");

        listWidget = page.submitListArrayWidget(display);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals("test", listWidget.getSubWidget("nxw_listItem", 0).getInputValue());
        assertEquals("test 2", listWidget.getSubWidget("nxw_listItem", 1).getInputValue());

        listWidget.removeElement(1);
        listWidget = page.submitListArrayWidget(display);
        assertEquals("test", listWidget.getSubWidget("nxw_listItem", 0).getInputValue());
        assertEquals(1, listWidget.getRows().size());
    }

    @Test
    public void testListWidget() {
        // Edit mode
        JSListWidgetElement listWidget = page.getListEditWidget(display);
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals(0, listWidget.getRows().size());

        listWidget.addNewElement();
        assertEquals(1, listWidget.getRows().size());
        listWidget = page.submitListWidget(display);
        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_listItem", 0, 1));

        listWidget.getSubWidget("nxw_listItem_2", 0).setInputValue("test");
        listWidget.addNewElement();
        assertEquals("test", listWidget.getSubWidget("nxw_listItem_2", 0).getInputValue());
        listWidget.getSubWidget("nxw_listItem_2", 1).setInputValue("test 2");

        page.submitListWidget(display);

        // View mode
        listWidget = page.getListViewWidget(display);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals("test", listWidget.getSubWidget("nxw_listItem_3", 0).getOutputValue());
        assertEquals("test 2", listWidget.getSubWidget("nxw_listItem_3", 1).getOutputValue());

        // remove
        page.getListEditWidget(display).removeElement(1);
        page.submitListWidget(display);

        listWidget = page.getListViewWidget(display);
        assertEquals("test", listWidget.getSubWidget("nxw_listItem_3", 0).getOutputValue());
        assertEquals(1, listWidget.getRows().size());
    }

    @Test
    public void testComplexListWidget() {
        JSListWidgetElement listWidget = page.getComplexListEditWidget(display);
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());

        listWidget = page.submitComplexListWidget(display);
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
        listWidget.getSubWidget("nxw_booleanComplexItem", 1).setInputValue("false");
        listWidget.getSubWidget("nxw_text", 1).setInputValue("field 3");
        listWidget.getSubWidget("nxw_text_1", 1).setInputValue("field 4");

        listWidget = page.submitComplexListWidget(display);

        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals("'lala' is not a number. Example: 9346.",
                listWidget.getSubWidgetMessageValue("nxw_intComplexItem", 0));

        listWidget.getSubWidget("nxw_intComplexItem", 0).setInputValue("3");
        listWidget = page.submitComplexListWidget(display);
        assertThat(listWidget.getSubWidgetMessageValue("nxw_intComplexItem", 0), isEmptyString());

        // View mode
        listWidget = page.getComplexListViewWidget(display);
        assertEquals("test", listWidget.getSubWidget("nxw_stringComplexItem_1", 0).getOutputValue());
        assertEquals("9/7/2010", listWidget.getSubWidget("nxw_dateComplexItem_1", 0).getOutputValue());
        assertEquals("3", listWidget.getSubWidget("nxw_intComplexItem_1", 0).getOutputValue());
        assertEquals("Yes", listWidget.getSubWidget("nxw_booleanComplexItem_1", 0).getOutputValue());
        assertEquals("field 1", listWidget.getSubWidget("nxw_text_2", 0).getOutputValue());
        assertEquals("field 2", listWidget.getSubWidget("nxw_text_3", 0).getOutputValue());

        assertEquals("test 2", listWidget.getSubWidget("nxw_stringComplexItem_1", 1).getOutputValue());
        assertThat(listWidget.getSubWidget("nxw_dateComplexItem_1", 1).getOutputValue(), isEmptyString());
        assertThat(listWidget.getSubWidget("nxw_intComplexItem_1", 1).getOutputValue(), isEmptyString());
        assertEquals("No", listWidget.getSubWidget("nxw_booleanComplexItem_1", 1).getOutputValue());
        assertEquals("field 3", listWidget.getSubWidget("nxw_text_2", 1).getOutputValue());
        assertEquals("field 4", listWidget.getSubWidget("nxw_text_3", 1).getOutputValue());

        page.getComplexListEditWidget(display).removeElement(1);
        listWidget = page.submitComplexListWidget(display);

        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());

        // View mode
        listWidget = page.getComplexListViewWidget(display);
        assertEquals("test", listWidget.getSubWidget("nxw_stringComplexItem_1", 0).getOutputValue());
        assertEquals("9/7/2010", listWidget.getSubWidget("nxw_dateComplexItem_1", 0).getOutputValue());
        assertEquals("3", listWidget.getSubWidget("nxw_intComplexItem_1", 0).getOutputValue());
        assertEquals("Yes", listWidget.getSubWidget("nxw_booleanComplexItem_1", 0).getOutputValue());
        assertEquals("field 1", listWidget.getSubWidget("nxw_text_2", 0).getOutputValue());
        assertEquals("field 2", listWidget.getSubWidget("nxw_text_3", 0).getOutputValue());
    }

    @Test
    public void testComplexList2Widget() {
        // Test select2 + html text (tiny_mce) in complex list widget
        JSListWidgetElement listWidget = page.getS2HtmlTextComplexListEditWidget(display);
        assertNotNull(listWidget);
        listWidget.addNewElement();

        String select2WidgetElementId = listWidget.getSubWidgetId("nxw_suggest_select2", 0);
        Select2WidgetElement select2WidgetElement = new Select2WidgetElement(driver, S2_PREFIX + select2WidgetElementId);
        select2WidgetElement.selectValue(S2_SELECTION_1, true);
        RichEditorElement richEditorElement = listWidget.getSubWidget("nxw_htmlTextItem", 0, RichEditorElement.class,
                true);
        richEditorElement.setInputValue(DUMMY_HTML_TEXT_CONTENT_1);

        listWidget.addNewElement();
        assertEquals(2, listWidget.getRows().size());

        select2WidgetElementId = listWidget.getSubWidgetId("nxw_suggest_select2", 1);
        select2WidgetElement = new Select2WidgetElement(driver, S2_PREFIX + select2WidgetElementId);
        select2WidgetElement.selectValue(S2_SELECTION_2, true);

        richEditorElement = listWidget.getSubWidget("nxw_htmlTextItem", 1, RichEditorElement.class, true);
        richEditorElement.setInputValue(DUMMY_HTML_TEXT_CONTENT_2);

        page.submitS2HtmlTextComplexListWidget(display);

        // View mode
        listWidget = page.getS2HtmlTextComplexListViewWidget(display);
        select2WidgetElementId = listWidget.getSubWidgetId("nxw_suggest_1_select2", 0);
        select2WidgetElement = new Select2WidgetElement(driver, select2WidgetElementId);
        assertEquals("Europe/" + S2_SELECTION_1, select2WidgetElement.getSelectedValue().getText());
        WidgetElement we = listWidget.getSubWidget("nxw_htmlTextItem_1", 0, true);
        assertEquals(DUMMY_HTML_TEXT_CONTENT_1, we.getValue(false));

        select2WidgetElementId = listWidget.getSubWidgetId("nxw_suggest_1_select2", 1);
        select2WidgetElement = new Select2WidgetElement(driver, select2WidgetElementId);
        assertEquals("Europe/" + S2_SELECTION_2, select2WidgetElement.getSelectedValue().getText());
        we = listWidget.getSubWidget("nxw_htmlTextItem_1", 1, true);
        assertEquals(DUMMY_HTML_TEXT_CONTENT_2, we.getValue(false));
    }

    @Test
    public void testListOfListsWidget() {
        JSListWidgetElement listWidget = page.getListOfListsEditWidget(display);
        assertNotNull(listWidget);
        assertNotEquals(VALUE_REQUIRED, listWidget.getMessageValue());
        assertEquals(0, listWidget.getRows().size());

        listWidget.addNewElement();

        WidgetElement stringItem = listWidget.getSubWidget("nxw_stringItem", 0);
        assertNotNull(stringItem);
        stringItem.setInputValue("test");

        listWidget = page.submitListOfListsWidget(display);

        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0));

        WidgetElement stringArrayItem = listWidget.getSubWidget("nxw_stringArrayItem", 0, WidgetElement.class, false);
        new Select(stringArrayItem.getInputElement()).selectByValue("cartman");

        listWidget = page.submitListOfListsWidget(display);

        assertThat(listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0), isEmptyString());

        JSListWidgetElement stringListItem = listWidget.getSubWidget("nxw_stringListItem", 0,
                JSListWidgetElement.class, false);
        stringListItem.addNewElement();
        stringListItem.getSubWidget("nxw_stringListSubItem", 0).setInputValue("test sublist");
        stringListItem.addNewElement();
        stringListItem.getSubWidget("nxw_stringListSubItem", 1).setInputValue("test sublist 2");

        // non regression tests for NXP-16406
        JSListWidgetElement stringListItem2 = listWidget.getSubWidget("nxw_stringListItem2", 0,
                JSListWidgetElement.class, false);
        stringListItem2.addNewElement();
        stringListItem2.getSubWidget("nxw_stringListSubItem2", 0).setInputValue("test sublist bis");
        stringListItem2.addNewElement();
        stringListItem2.getSubWidget("nxw_stringListSubItem2", 1).setInputValue("test sublist 2 bis");

        page.submitListOfListsWidget(display);

        // View mode
        listWidget = page.getListOfListsViewWidget(display);

        assertEquals("test", listWidget.getSubWidget("nxw_stringItem_1", 0).getOutputValue());

        stringArrayItem = listWidget.getWidget("stringArrayItem", WidgetElement.class);
        assertEquals("Eric Cartman", stringArrayItem.getOutputValue());

        stringListItem = listWidget.getSubWidget("nxw_stringListItem_1", 0, JSListWidgetElement.class, false);
        assertEquals("test sublist", stringListItem.getSubWidget("nxw_stringListSubItem_1", 0).getOutputValue());
        assertEquals("test sublist 2", stringListItem.getSubWidget("nxw_stringListSubItem_1", 1).getOutputValue());

        stringListItem2 = listWidget.getSubWidget("nxw_stringListItem2_1", 0, JSListWidgetElement.class, false);
        assertEquals("test sublist bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 0).getOutputValue());
        assertEquals("test sublist 2 bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 1).getOutputValue());

        // delete 2nd row in stringListItem
        listWidget = page.getListOfListsEditWidget(display);
        stringListItem = listWidget.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class, false);
        stringListItem.removeElement(1);
        assertEquals(1, stringListItem.getRows().size());

        page.submitListOfListsWidget(display);

        listWidget = page.getListOfListsViewWidget(display);

        assertEquals("test", listWidget.getSubWidget("nxw_stringItem_1", 0).getOutputValue());

        stringArrayItem = listWidget.getWidget("stringArrayItem", WidgetElement.class);
        assertEquals("Eric Cartman", stringArrayItem.getOutputValue());
        assertEquals(1, stringListItem.getRows().size());

        stringListItem = listWidget.getSubWidget("nxw_stringListItem_1", 0, JSListWidgetElement.class, false);
        assertEquals("test sublist", stringListItem.getSubWidget("nxw_stringListSubItem_1", 0).getOutputValue());

        stringListItem2 = listWidget.getSubWidget("nxw_stringListItem2_1", 0, JSListWidgetElement.class, false);
        assertEquals("test sublist bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 0).getOutputValue());
        assertEquals("test sublist 2 bis", stringListItem2.getSubWidget("nxw_stringListSubItem2_1", 1).getOutputValue());

        // non regression tests for NXP-5576
        listWidget = page.getListOfListsEditWidget(display);

        listWidget.addNewElement();
        assertEquals(2, listWidget.getRows().size());
        assertNotNull(listWidget.getSubWidget("nxw_stringItem", 1));
    }

    // non regression tests for NXP-6933
    @Test
    public void testNonRegression_NXP_6933() {
        JSListWidgetElement listWidget = page.getListArrayEditWidget(display);

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

        page.submitListArrayWidget(display);

        assertEquals("BBB", page.getListArrayViewWidget(display).getSubWidget("nxw_listItem_1", 0).getOutputValue());
        assertEquals("BBB", page.getListArrayEditWidget(display).getSubWidget("nxw_listItem", 0).getInputValue());
    }

    /**
     * Check that removed elements are not validated.
     *
     * @since 7.2
     */
    @Test
    public void testRemovedElementValidation() {
        JSListWidgetElement listWidget = page.getListOfListsEditWidget(display);
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
        listWidget = page.submitListOfListsWidget(display);

        assertNotEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0));
    }

    /**
     * Check moved elements validation.
     *
     * @since 7.2
     */
    @Test
    public void testMovedElementValidation() {
        JSListWidgetElement listWidget = page.getListOfListsEditWidget(display);
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
        listWidget = page.submitListOfListsWidget(display);

        assertNotEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 0));
        assertEquals(VALUE_REQUIRED, listWidget.getSubWidgetMessageValue("nxw_stringArrayItem", 1));
    }

    /**
     * Non-regression test for NXP-18486: removed element state should be reset.
     *
     * @since 8.1
     */
    @Test
    public void testRemovedElementReset() {
        JSListWidgetElement listWidget = page.getListEditWidget(display);
        assertNotNull(listWidget);
        listWidget.addNewElement();
        assertEquals("", listWidget.getSubWidget("nxw_listItem_2", 0).getInputValue());
        listWidget.getSubWidget("nxw_listItem_2", 0).setInputValue("test");

        // remove it an add a new one
        listWidget.removeElement(0);
        listWidget.addNewElement();

        // check that new element state is reset
        assertEquals("", listWidget.getSubWidget("nxw_listItem_2", 0).getInputValue());
    }

    /**
     * Non-regression test for NXP-18486: removed element state should be reset.
     *
     * @since 8.1
     */
    @Test
    public void testRemovedSubElementReset() {
        JSListWidgetElement listWidget = page.getListOfListsEditWidget(display);
        assertNotNull(listWidget);
        listWidget.addNewElement();

        JSListWidgetElement stringListItem = listWidget.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class,
                false);
        stringListItem.addNewElement();
        assertEquals("", stringListItem.getSubWidget("nxw_stringListSubItem", 0).getInputValue());
        stringListItem.getSubWidget("nxw_stringListSubItem", 0).setInputValue("test sublist");

        // remove it an add a new one
        stringListItem.removeElement(0);
        stringListItem.addNewElement();

        // check that new element state is reset
        assertEquals("", stringListItem.getSubWidget("nxw_stringListSubItem", 0).getInputValue());
    }

    /**
     * Non-regression test for NXP-18486: removed element state should be reset.
     *
     * @since 8.1
     */
    @Test
    public void testRemovedComplexElementReset() {
        JSListWidgetElement listWidget = page.getComplexListEditWidget(display);
        assertNotNull(listWidget);
        listWidget.addNewElement();
        assertEquals("", listWidget.getSubWidget("nxw_stringComplexItem", 0).getInputValue());
        listWidget.getSubWidget("nxw_stringComplexItem", 0).setInputValue("test");

        // remove it an add a new one
        listWidget.removeElement(0);
        listWidget.addNewElement();

        // check that new element state is reset
        assertEquals("", listWidget.getSubWidget("nxw_stringComplexItem", 0).getInputValue());
    }

}
