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
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.forms.SelectOneDirectoryWidgetElement;
import org.openqa.selenium.By;

/**
 * @since 7.4
 */
public class ITSelectOneDirectoryWidgetTest extends AbstractWidgetPageTest {

    public ITSelectOneDirectoryWidgetTest() {
        super("selectOneDirectoryWidget");
    }

    @Test
    public void testWidget() {
        navigateTo(pageId);
        checkNoError();
        assertEquals(
                "",
                driver.findElement(
                        By.xpath("//form[@id='selectOneDirectoryWidgetLayout_view_form']/table/tbody/tr/td[2]")).getText());
        checkValueRequired(false);
        submitDemo();
        checkValueRequired(true);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.watchAjaxRequests();
        getEditWidget(SelectOneDirectoryWidgetElement.class).setInputValue("Eric Cartman");
        arm.waitForAjaxRequests();
        submitDemo();
        checkValueRequired(false);
        assertEquals(
                "Eric Cartman",
                driver.findElement(
                        By.xpath("//form[@id='selectOneDirectoryWidgetLayout_view_form']/table/tbody/tr/td[2]")).getText());
        navigateTo(pageId);
        assertEquals(
                "",
                driver.findElement(
                        By.xpath("//form[@id='selectOneDirectoryWidgetLayout_view_form']/table/tbody/tr/td[2]")).getText());
        assertEquals("", getEditWidget(SelectOneDirectoryWidgetElement.class).getValue(true));
    }

}