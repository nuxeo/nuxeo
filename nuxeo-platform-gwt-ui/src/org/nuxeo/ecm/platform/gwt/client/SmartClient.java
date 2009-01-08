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

package org.nuxeo.ecm.platform.gwt.client;

import org.nuxeo.ecm.platform.gwt.client.ui.SmartWidget;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * This class is there to fix any problem found in smartgwt like 
 * unexposed JS API or real bugs. Ideally this class should not exist.
 * 
 * See also NuxeoDataSource
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SmartClient implements EntryPoint {


    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        ApplicationBundle bundle = GWT.create(SmartBundle.class);
        bundle.start();
    }
    
    public static Canvas toCanvas(Widget widget) {
        if (widget instanceof Canvas) {
            return (Canvas)widget;
        } else {
            return new SmartWidget(widget);
        }
    }
    
    
    public static void install() {
        fixCursorTrackerPath();        
    }
    
    
    /**
     * The cursor tracker path is not correctly initialized by ISC - because of the JS load order
     * which is first loading RPCManager class and then the loadSkin JS.
     *   
     */
    public native static void fixCursorTrackerPath()/*-{
    $wnd.isc.RPC.cursorTrackerDefaults.src=$wnd.isc.Page.getSkinDir()+"/images/shared/progressCursorTracker.png";
    }-*/;
    
    /**
     * Tree is not yet exposing findNodeById method. 
     */    
    public static TreeNode findNodeById(TreeGrid tree, String id) {
        return findNodeById(tree.getData(), id);
    }
    
    public native static TreeNode findNodeById(Tree tree, String id) /*-{
        var theTree= tree.@com.smartgwt.client.core.BaseClass::getOrCreateJsObj()();
        var node = theTree.findById(id);
        if (node != null) {
            return @com.smartgwt.client.widgets.tree.TreeNode::new(Lcom/google/gwt/core/client/JavaScriptObject;)(node);
        }
        return null;
    }-*/;

    


    public static void unloadChildren(Tree tree, TreeNode node) {
        unloadChildren(tree, node.getJsObj());
    }
    
    public native static void unloadChildren(Tree tree, JavaScriptObject node) /*-{
    var theTree= tree.@com.smartgwt.client.core.BaseClass::getOrCreateJsObj()();
    theTree.unloadChildren(node);
    }-*/;
    
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

    public static native void setTabIcon(TabSet tabs, String tab, String icon) /*-{
    var self = tabs.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
    self.setTabIcon(tab, icon);
}-*/;
    
    public static native void setTabTitle(TabSet tabs, Tab tab, String title) /*-{
        var self = tabs.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        var tabJS = tab.@com.smartgwt.client.widgets.tab.Tab::getJsObj()();
        self.setTabTitle(tabJS, title);
    }-*/;

    
    public static native void setSectionTitle(SectionStack stack, String id, String title) /*-{
    var self = stack.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
    self.setSectionTitle(id, title);
}-*/;

    //TODO
    public static native void setSectionIcon(SectionStack stack, String id, String icon) /*-{
    var self = stack.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
    //self.setSectionIcon(id, icon);
}-*/;

    public static native int getSectionsCount(SectionStack stack) /*-{
    var self = stack.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
    //var sectionJS = tab.@com.smartgwt.client.widgets.layout.SectionStackSection::getJsObj()();
    return self.getSections().length;
}-*/;

    public static native void addSectionItem(SectionStack stack, String id, Canvas widget) /*-{
    var self = stack.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
    var jsWidget = widget.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
    self.addItem(id, jsWidget);
}-*/;
    
}
