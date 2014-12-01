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

package org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.forms.ListWidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;

/**
 * @since 7.1
 */
public class ListStandardWidgetPage extends Page {

    private static final String S2_HTML_TEXT_COMPLEX_LIST_WIDGET_EDIT_ID = "complexListWidgetLayout2_edit_form:nxl_complexListWidgetLayout2:nxw_complexListWidget_2";

    private static final String S2_HTML_TEXT_COMPLEX_LIST_WIDGET_VIEW_ID = "complexListWidgetLayout2_view_form:nxl_complexListWidgetLayout2_1:nxw_complexListWidget_3";

    private static final String S2_HTML_TEXT_COMPLEX_LIST_WIDGET_SUBMIT_ID = "complexListWidgetLayout2_edit_form:complexListWidgetLayout2_edit_form_submitButton";

    public ListWidgetElement getS2HtmlTextComplexListEditWidget() {
        goToOverviewTab();
        return new ListWidgetElement(AbstractTest.driver,
                S2_HTML_TEXT_COMPLEX_LIST_WIDGET_EDIT_ID);
    }

    public ListWidgetElement getS2HtmlTextComplexListViewWidget() {
        goToOverviewTab();
        return new ListWidgetElement(AbstractTest.driver,
                S2_HTML_TEXT_COMPLEX_LIST_WIDGET_VIEW_ID);
    }

    public ListStandardWidgetPage submitS2HtmlTextComplexListWidget() {
        AjaxRequestManager a = new AjaxRequestManager(AbstractTest.driver);
        a.watchAjaxRequests();
        AbstractTest.driver.findElement(
                By.id(S2_HTML_TEXT_COMPLEX_LIST_WIDGET_SUBMIT_ID)).click();
        a.waitForAjaxRequests();
        return AbstractTest.asPage(ListStandardWidgetPage.class);
    }

}
