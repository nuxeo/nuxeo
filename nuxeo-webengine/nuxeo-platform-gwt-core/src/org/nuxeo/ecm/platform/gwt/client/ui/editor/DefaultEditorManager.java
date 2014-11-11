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

package org.nuxeo.ecm.platform.gwt.client.ui.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SiteEventHandler;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

/**
 * Manage editors that can be tabbed and instantiated more than once in same time.  
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultEditorManager implements EditorManager, Extensible, SiteEventHandler {

    protected List<Editor> editors;
    protected List<EditorSite> sites;
    protected Editor defaultEditor;
    protected Container container;
    
    
    public DefaultEditorManager() {
        this (null);
    }
    
    public DefaultEditorManager(Container container) {
        editors = new ArrayList<Editor>();
        sites = new ArrayList<EditorSite>();
        this.container = container;
    }

    /**
     * @param container the container to set.
     */
    public void setContainer(Container container) {
        this.container = container;
    }
    
    /**
     * @return the container.
     */
    public Container getContainer() {
        return container;
    }
    
    public View getEditorView(String name) {
        EditorSite vs = getEditorSite(name);
        return vs == null ? null : vs.getView();
    }
    
    public EditorSite getEditorSiteByHandle(Object handle) {
        for (EditorSite vs : sites) {
            if (handle.equals(vs.getHandle())) {
                return vs;
            }
        }
        return null;
    }
    
    public EditorSite getEditorSite(String name) {
        for (EditorSite vs : sites) {
            if (name.equals(vs.getName())) {
                return vs;
            }
        }
        return null;
    }
    
    public void handleSiteEvent(Object handle, int event) {
        if (event == SITE_CLOSED) {
            siteClosed(handle);
        }        
    }
    
    /**
     * Must be called by the implementation when closing a tab through mouse click or through API. 
     * to free up the associated editor view
     * @param id
     */
    protected void siteClosed(Object handle) {
        Iterator<EditorSite> it = sites.iterator();
        while (it.hasNext()) {
            EditorSite esite = it.next();
            if (handle.equals(esite.getHandle())) {
                it.remove();
                //TODO: how to manage #fragments?
                History.newItem("", false);
                break;
            }
        }        
    }

    public void addEditor(Editor editor) {
        editors.add(editor);
    }
    
    public void removeEditor(Editor editor) {
        editors.remove(editor);
    }
    
    public View[] getOpenedEditors() {
        int len = sites.size();
        View[] views = new View[len];
        for (int i=0; i<len; i++) {
            views[i] = sites.get(i).getView();
        }
         return views;
    }
    
    public Editor[] getRegisteredEditors() {
        return editors.toArray(new Editor[editors.size()]);
    }
    

    public EditorSite getActiveEditor() {
        Object handle = container.getActiveSiteHandle();
        if (handle == null) {
            return null;
        }
        return getEditorSiteByHandle(handle);
    }

    public EditorSite openEditor(Object input) {
        return openEditor(input, false);
    }
    
    public EditorSite openEditor(Object input, boolean newView) {
        EditorSite esite = null;
        for (Editor editor : editors) {
            if (editor.acceptInput(input)) {
                esite = openEditor(editor, input, newView);
                break;
            }            
        }
        if (esite == null) {
            esite = openEditor(defaultEditor, input, newView);
            if (esite == null) {
                Window.alert("No editor registered for this input.\r\n\r\nYou must contribute a default editor that will be used for any unknown input");
                return null;
            }
        }
        container.activateSite(esite);
        return esite;
    }

    protected EditorSite openEditor(Editor editor, Object input, boolean newView) {
        if (!newView) {                
            for (EditorSite esite : sites) {
                if (esite.editor == editor) {
                    // reuse view
                    esite.open(container, input); // change view input
                    return esite;
                }
            }                
        }
        EditorSite esite = new EditorSite(editor);
        esite.open(container, input);
        sites.add(esite);
        return esite;
    }    

    
    public void closeEditor(String id) {
        // close tab should notify us through siteClosed() when close is done.
        EditorSite site = getEditorSite(id);
        container.closeSite(site);
        siteClosed(id);
    }

    public void closeAll() {
        container.clear();
        sites.clear();
    }
 
    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.EDITORS_XP.equals(target)) {
            editors.add((Editor)extension);
        } else if (ExtensionPoints.DEFAULT_EDITOR_XP.equals(target)) {
            defaultEditor = (Editor)extension;
        }
    }
    
}
