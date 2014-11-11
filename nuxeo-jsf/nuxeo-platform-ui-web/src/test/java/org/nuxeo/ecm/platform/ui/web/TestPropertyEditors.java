/*
 * (C) Copyright 2007-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin
 *
 */

package org.nuxeo.ecm.platform.ui.web;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Arrays;

import org.nuxeo.ecm.platform.ui.web.util.beans.PropertiesEditorsInstaller;

import junit.framework.TestCase;

public class TestPropertyEditors extends TestCase {

    PropertiesEditorsInstaller installer = new PropertiesEditorsInstaller();

    @Override
    protected void setUp() throws Exception {
        installer.installEditors();
    }

    @Override
    protected void tearDown() throws Exception {
        installer.uninstallEditors();
    }

    public void testCoerceStringArrays() {
        PropertyEditor editor = PropertyEditorManager.findEditor(String[].class);
        assertNotNull(editor);

        // check singleton
        editor.setAsText("single");
        assertTrue(Arrays.equals(new String[] { "single" }, (String[])editor.getValue()));
        assertEquals("single", editor.getAsText());

        // check multi valued
        editor.setAsText("a,b");
        assertTrue(Arrays.equals(new String[] { "a", "b" }, (String[])editor.getValue()));
        assertEquals("a, b", editor.getAsText());
    }
}
