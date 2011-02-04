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
