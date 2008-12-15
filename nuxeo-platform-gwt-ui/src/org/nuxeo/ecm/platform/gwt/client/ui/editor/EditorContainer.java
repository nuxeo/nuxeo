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
import org.nuxeo.ecm.platform.gwt.client.ui.Editor;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.CloseClickHandler;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EditorContainer extends SmartView<TabSet> implements Extensible, CloseClickHandler {

    protected List<Editor> editors;
    protected List<EditorView> editorViews;
    protected Editor defaultEditor;
    
    /**
     * 
     */
    public EditorContainer() {
        super("editors");
        editors = new ArrayList<Editor>();
        editorViews = new ArrayList<EditorView>();
    }
    
    @Override
    public void setInput(Object input) {
        EditorView view = openTab(input);
        if (view == null) {
            GWT.log("Cannot create editor tab for input: "+input, null);
            return;
        }        
        if (widget.getTab(view.tab.getID()) == null) {
            widget.addTab(view.tab); 
            editorViews.add(view);
        }
        widget.setTabTitle(view.tab, view.view.getTitle());
        //TODO: ow to change icon?
        //widget.setTabTitle(view.tab, view.view.getIcon());
        widget.selectTab(view.tab);
    }
    
    @Override
    public Object getInput() {
        return null; //TODO
    }
    
    @Override
    protected TabSet createWidget() {
        TabSet tabs = new TabSet();
        tabs.setTabBarPosition(Side.TOP);
                
        if (defaultEditor == null) {
            defaultEditor = new DefaultEditor();
        }

        tabs.addCloseClickHandler(this);
        
        return tabs;
    }

    public EditorView openTab(Object input) {
        EditorView view = null;
        for (Editor editor : editors) {
            view = openTab(editor, input);
            if (view != null) {
                return view;
            }
        }
        return openTab(defaultEditor, input);
    }

    public EditorView openTab(Editor editor, Object input) {
        if (editor.acceptInput(input)) {
            if (editor.canReuseViews()) {                
                for (EditorView view : editorViews) {
                    if (view.editor == editor) {
                        // reuse view
                        view.setInput(input);
                        return view;
                    }
                }                
            }
            EditorView view = new EditorView(editor);
            view.setInput(input);
            return view;
        }
        return null;
    }
    


    public void onCloseClick(TabCloseClickEvent event) {
        //event.getTab() is not working -> throw a classcastexception
        // so we try a workaround
        String id = getTabId(event);
        Iterator<EditorView> it = editorViews.iterator();
        while (it.hasNext()) {
            EditorView view = it.next();
            if (id.equals(view.tab.getID())) {
                //System.out.println("remove view: "+view.view.getName());
                it.remove();
                break;
            }
        }
        widget.removeTab(id);
    }

    static class EditorView {
        public Editor editor;
        public View<?> view;
        public Tab tab;
        public EditorView(Editor editor) {
            this.editor = editor;
        }
        public void setInput(Object input) {
            if (view != null) {
                view.setInput(input);
            } else {
                view = editor.open(input);
                tab = new Tab();
                tab.setCanClose(true);
                tab.setAttribute("viewId", view.getName());
                tab.setPane((Canvas)view.getWidget());
                tab.setID("ZZZZZ_"+System.currentTimeMillis());
            }
        }
    }

    /**
     * TODO workaround for a smartgwt bug
     * Hack to get the tab id - event.getTab() is not working - throws a ClassCastExcpetion
     * @param event
     * @return
     */
    public static native String getTabId(TabCloseClickEvent event) /*-{
        var jsObj = event.@com.smartgwt.client.event.AbstractSmartEvent::jsObj;
        return jsObj.tab.ID;
    }-*/;

    /**
     * TODO workaround for a smartgwt bug
     * Hack to set the icon - this method is missing from the API 
     * @param tabs
     * @param tab
     * @param icon
     */
    public static native void setTabIcon(TabSet tabs, Tab tab, String icon) /*-{
        var self = tabs.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        var tabJS = tab.@com.smartgwt.client.widgets.tab.Tab::getJsObj()();
        self.setTabIcon(tabJS, icon);
    }-*/;
    
    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.EDITORS_XP.equals(target)) {
            editors.add((Editor)extension);
        } else if (ExtensionPoints.DEFAULT_EDITOR_XP.equals(target)) {
            defaultEditor = (Editor)extension;
        }
    }
    
}
