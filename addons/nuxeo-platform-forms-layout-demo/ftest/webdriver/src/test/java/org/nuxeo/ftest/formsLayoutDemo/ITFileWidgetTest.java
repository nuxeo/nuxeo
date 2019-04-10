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
