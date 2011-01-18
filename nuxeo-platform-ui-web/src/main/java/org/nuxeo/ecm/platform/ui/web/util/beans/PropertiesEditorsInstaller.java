package org.nuxeo.ecm.platform.ui.web.util.beans;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;


public class PropertiesEditorsInstaller {

    public PropertiesEditorsInstaller() {

    }

    public void installEditors() {
        installEditor(String[].class, StringArrayEditor.class);
    }

    public void uninstallEditors() {
        uninstallEditor(String[].class, StringArrayEditor.class);
    }

    protected void installEditor(Class<?> targetType, Class<?> editorClass) {
        if (PropertyEditorManager.findEditor(targetType)  != null) {;
            return;
        }
        PropertyEditorManager.registerEditor(targetType, editorClass);
    }

    protected void uninstallEditor(Class<?> targetType, Class<?> editorClass) {
        PropertyEditor editor  = PropertyEditorManager.findEditor(targetType);
        if (!editorClass.equals(editor.getClass())) {
            return;
        }
        PropertyEditorManager.registerEditor(targetType, null);
    }
}
