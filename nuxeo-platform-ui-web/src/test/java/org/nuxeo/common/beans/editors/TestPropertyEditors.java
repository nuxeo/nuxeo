package org.nuxeo.common.beans.editors;

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
