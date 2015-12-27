/*
 * (C) Copyright 2007-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 *
 */

package org.nuxeo.ecm.platform.ui.web;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Arrays;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.ui.web.util.beans.PropertiesEditorsInstaller;

public class TestPropertyEditors {

    PropertiesEditorsInstaller installer = new PropertiesEditorsInstaller();

    @Before
    public void setUp() throws Exception {
        installer.installEditors();
    }

    @After
    public void tearDown() throws Exception {
        installer.uninstallEditors();
    }

    @Test
    public void testCoerceStringArrays() {
        PropertyEditor editor = PropertyEditorManager.findEditor(String[].class);
        assertNotNull(editor);

        // check singleton
        editor.setAsText("single");
        assertTrue(Arrays.equals(new String[] { "single" }, (String[]) editor.getValue()));
        assertEquals("single", editor.getAsText());

        // check multi valued
        editor.setAsText("a,b");
        assertTrue(Arrays.equals(new String[] { "a", "b" }, (String[]) editor.getValue()));
        assertEquals("a, b", editor.getAsText());
    }
}
