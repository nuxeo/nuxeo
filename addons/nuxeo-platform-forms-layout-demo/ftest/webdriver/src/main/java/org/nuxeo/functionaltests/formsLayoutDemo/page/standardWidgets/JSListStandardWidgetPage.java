/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 *
 */

package org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.forms.JSListWidgetElement;
import org.nuxeo.functionaltests.forms.JSListWidgetElement.Display;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;

/**
 * @since 7.1
 */
public class JSListStandardWidgetPage extends Page {

    private static final String LIST_ARRAY_WIDGET_EDIT_ID = "listArrayWidgetLayout_edit_form:nxl_listArrayWidgetLayout:nxw_listArrayWidget";

    private static final String LIST_ARRAY_WIDGET_VIEW_ID = "listArrayWidgetLayout_view_form:nxl_listArrayWidgetLayout_1:nxw_listArrayWidget_1";

    private static final String LIST_ARRAY_WIDGET_SUBMIT_ID = "listArrayWidgetLayout_edit_form:listArrayWidgetLayout_edit_form_submitButton";

    private static final String LIST_WIDGET_EDIT_ID = "listWidgetLayout_edit_form:nxl_listWidgetLayout:nxw_listWidget";

    private static final String LIST_WIDGET_VIEW_ID = "listWidgetLayout_view_form:nxl_listWidgetLayout_1:nxw_listWidget_1";

    private static final String LIST_WIDGET_SUBMIT_ID = "listWidgetLayout_edit_form:listWidgetLayout_edit_form_submitButton";

    private static final String COMPLEX_LIST_WIDGET_EDIT_ID = "complexListWidgetLayout_edit_form:nxl_complexListWidgetLayout:nxw_complexListWidget";

    private static final String COMPLEX_LIST_WIDGET_VIEW_ID = "complexListWidgetLayout_view_form:nxl_complexListWidgetLayout_1:nxw_complexListWidget_1";

    private static final String COMPLEX_LIST_WIDGET_SUBMIT_ID = "complexListWidgetLayout_edit_form:complexListWidgetLayout_edit_form_submitButton";

    private static final String S2_HTML_TEXT_COMPLEX_LIST_WIDGET_EDIT_ID = "complexListWidgetLayout2_edit_form:nxl_complexListWidgetLayout2:nxw_complexListWidget_2";

    private static final String S2_HTML_TEXT_COMPLEX_LIST_WIDGET_VIEW_ID = "complexListWidgetLayout2_view_form:nxl_complexListWidgetLayout2_1:nxw_complexListWidget_3";

    private static final String S2_HTML_TEXT_COMPLEX_LIST_WIDGET_SUBMIT_ID = "complexListWidgetLayout2_edit_form:complexListWidgetLayout2_edit_form_submitButton";

    private static final String LIST_OF_LISTS_WIDGET_VIEW_ID = "listOfListsWidgetLayout_view_form:nxl_listOfListsWidgetLayout_1:nxw_listOfListsWidget_1";

    private static final String LIST_OF_LISTS_WIDGET_EDIT_ID = "listOfListsWidgetLayout_edit_form:nxl_listOfListsWidgetLayout:nxw_listOfListsWidget";

    private static final String LIST_OF_LISTS_WIDGET_SUBMIT_ID = "listOfListsWidgetLayout_edit_form:listOfListsWidgetLayout_edit_form_submitButton";

    // List Array
    public JSListWidgetElement getListArrayViewWidget() {
        return new JSListWidgetElement(AbstractTest.driver, LIST_ARRAY_WIDGET_VIEW_ID);
    }

    public JSListWidgetElement getListArrayEditWidget() {
        return new JSListWidgetElement(AbstractTest.driver, LIST_ARRAY_WIDGET_EDIT_ID);
    }

    public JSListWidgetElement submitListArrayWidget() {
        submit(LIST_ARRAY_WIDGET_SUBMIT_ID);
        return getListArrayEditWidget();
    }

    // Standard List
    public JSListWidgetElement getListViewWidget() {
        return new JSListWidgetElement(AbstractTest.driver, LIST_WIDGET_VIEW_ID);
    }

    public JSListWidgetElement getListEditWidget() {
        return new JSListWidgetElement(AbstractTest.driver, LIST_WIDGET_EDIT_ID);
    }

    public JSListWidgetElement submitListWidget() {
        submit(LIST_WIDGET_SUBMIT_ID);
        return getListEditWidget();
    }

    // Complex List
    public JSListWidgetElement getComplexListViewWidget() {
        return new JSListWidgetElement(AbstractTest.driver, COMPLEX_LIST_WIDGET_VIEW_ID);
    }

    public JSListWidgetElement getComplexListEditWidget() {
        return new JSListWidgetElement(AbstractTest.driver, COMPLEX_LIST_WIDGET_EDIT_ID,
                JSListWidgetElement.Display.TABLE);
    }

    public JSListWidgetElement submitComplexListWidget() {
        submit(COMPLEX_LIST_WIDGET_SUBMIT_ID);
        return getComplexListEditWidget();
    }

    // Complex List 2
    public JSListWidgetElement getS2HtmlTextComplexListViewWidget() {
        return new JSListWidgetElement(AbstractTest.driver, S2_HTML_TEXT_COMPLEX_LIST_WIDGET_VIEW_ID, Display.TABLE);
    }

    public JSListWidgetElement getS2HtmlTextComplexListEditWidget() {
        return new JSListWidgetElement(AbstractTest.driver, S2_HTML_TEXT_COMPLEX_LIST_WIDGET_EDIT_ID);
    }

    public JSListWidgetElement submitS2HtmlTextComplexListWidget() {
        submit(S2_HTML_TEXT_COMPLEX_LIST_WIDGET_SUBMIT_ID);
        return getS2HtmlTextComplexListEditWidget();
    }

    // List of lists
    public JSListWidgetElement getListOfListsViewWidget() {
        return new JSListWidgetElement(AbstractTest.driver, LIST_OF_LISTS_WIDGET_VIEW_ID);
    }

    public JSListWidgetElement getListOfListsEditWidget() {
        return new JSListWidgetElement(AbstractTest.driver, LIST_OF_LISTS_WIDGET_EDIT_ID);
    }

    public JSListWidgetElement submitListOfListsWidget() {
        submit(LIST_OF_LISTS_WIDGET_SUBMIT_ID);
        return getListOfListsEditWidget();
    }

    protected JSListStandardWidgetPage submit(String buttonId) {
        AjaxRequestManager a = new AjaxRequestManager(AbstractTest.driver);
        a.watchAjaxRequests();
        AbstractTest.driver.findElement(By.id(buttonId)).click();
        a.waitForAjaxRequests();
        return AbstractTest.asPage(JSListStandardWidgetPage.class);
    }

}
