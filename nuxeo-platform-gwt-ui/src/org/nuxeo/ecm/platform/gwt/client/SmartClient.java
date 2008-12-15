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
    
    public static Canvas getCanvas(Widget widget) {
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
    
    
}
