/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.webengine.gwt.client.ui.impl;

import org.nuxeo.webengine.gwt.client.Application;
import org.nuxeo.webengine.gwt.client.Extensible;
import org.nuxeo.webengine.gwt.client.ui.Editor;
import org.nuxeo.webengine.gwt.client.ui.EditorContainer;
import org.nuxeo.webengine.gwt.client.ui.ExtensionPoints;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EditorContainerImpl extends EditorContainer implements Extensible {
    
    
    public EditorContainerImpl() {
        DeckPanel panel = new DeckPanel();
        panel.setSize("100%", "100%");
        SimplePanel defaultPanel = new SimplePanel();
        defaultPanel.setSize("100%", "100%");
        panel.add(defaultPanel);
        panel.showWidget(0);
        initWidget(panel);
    }    
    
    @Override
    protected void onAttach() {        
        super.onAttach();
        // if no contributions were made initialize with default values
        Editor editor = (Editor)((SimplePanel)(getDeckPanel().getWidget(0))).getWidget();
        if (editor == null) { // a default editor if no one was contributed
            editor = new Editor("default", new SimplePanel()) {
                @Override
                public void refresh() {
                    Object input = Application.getContext().getInputObject();
                    if (input instanceof Widget) {
                        ((SimplePanel)getWidget()).setWidget((Widget)input);
                    } else {
                        ((SimplePanel)getWidget()).setWidget(new HTML(input.toString()));
                    }
                }
            };  
            ((SimplePanel)(getDeckPanel().getWidget(0))).setWidget(editor);
        }
    }
    
    
    protected void setDefaultEditor(Editor editor) {
        if (editor == null) return;
        ((SimplePanel)(getDeckPanel().getWidget(0))).setWidget(editor);
    }
    
    @Override
    public Editor getDefaultEditor() {
        return (Editor)((SimplePanel)(getDeckPanel().getWidget(0))).getWidget(); 
    }
    
    public DeckPanel getDeckPanel() {
        return (DeckPanel)getWidget();        
    }
    
    public void showEditor() {
        DeckPanel panel = getDeckPanel();
        Object input = Application.getContext().getInputObject();
        int cnt = panel.getWidgetCount();
        for (int i=1; i<cnt; i++) {
            Editor editor = (Editor)panel.getWidget(i);
            if (editor.acceptInput(input)) {
                editor.refresh();
                panel.showWidget(i);                
                return;
            }
        }
        // show default editor
        Editor editor = getDefaultEditor();
        if (editor != null) {
            editor.refresh();
        }
        panel.showWidget(0);
    }

    @Override
    public void showEditor(String name) {
        if (name == null) {
            showEditor();
            return;
        }
        DeckPanel panel = getDeckPanel();
        if ("default".equals(name)) {
            Editor editor = getDefaultEditor();
            if (editor != null) {
                editor.refresh();
            }
            panel.showWidget(0);
            return;
        }         
        int cnt = panel.getWidgetCount();
        for (int i=1; i<cnt; i++) {
            Editor editor = (Editor)panel.getWidget(i);
            if (name.equals(editor.getName())) {
                editor.refresh();
                panel.showWidget(i);                
                return;
            }
        }
    }

    public void registerExtension(String target, Object extension, int type) {
        if (ExtensionPoints.EDITORS_XP.equals(target)) {
            DeckPanel panel = getDeckPanel();
            Editor editor = (Editor)extension;
            if ("default".equals(editor.getName())) {
                setDefaultEditor(editor);
            } else {
                panel.add(editor);    
            }            
        } else if (ExtensionPoints.EDITORS_XP.equals(target)) {
            GWT.log("Unknown extension point: "+target, null);            
        }
    }
    
    public EditorContainerImpl register() {
        Application.registerExtension(ExtensionPoints.EDITOR_CONTAINER_XP, this);
        Application.registerExtensionPoint(ExtensionPoints.EDITORS_XP, this);
        return this;
    }

    
}
