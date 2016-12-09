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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.functionaltests.forms.FileWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.openqa.selenium.By;

/**
 * @since 7.4
 */
public class ITFileWidgetTest extends AbstractWidgetPageTest {

    public ITFileWidgetTest() {
        super("fileWidget");
    }

    @Test
    public void testWidget() throws IOException {
        navigateTo(pageId);
        checkNoError();
        assertEquals("File widget", driver.findElement(By.id("fileWidgetLayout_view_form")).getText());
        checkValueRequired(false);
        assertNotEquals("Empty file", getEditWidgetMessage());
        doSubmitDemo();
        assertEquals("Empty file", getEditWidgetMessage());
        LayoutElement edit = new LayoutElement(driver, pageId + "Layout_edit_form:nxl_" + pageId + "Layout");
        FileWidgetElement w = edit.getWidget("nxw_" + pageId, FileWidgetElement.class);
        w.uploadTestFile("file_1", ".txt", "hello");
        doSubmitDemo();
        checkValueRequired(false);
        assertNotEquals("Empty file", getEditWidgetMessage());
        String viewName = getViewWidget().getSubElement("download").getText();
        String editName = getEditWidget().getSubElement("default_download:download").getText();
        assertTrue(viewName.startsWith("file_1"));
        assertTrue(viewName.endsWith(".txt"));
        assertTrue(editName.startsWith("file_1"));
        assertTrue(editName.endsWith(".txt"));
        navigateTo(pageId);
        assertEquals("File widget", driver.findElement(By.id("fileWidgetLayout_view_form")).getText());
    }
}