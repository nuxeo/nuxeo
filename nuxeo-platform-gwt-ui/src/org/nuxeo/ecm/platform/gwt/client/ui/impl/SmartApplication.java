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

package org.nuxeo.ecm.platform.gwt.client.ui.impl;

import org.nuxeo.ecm.platform.gwt.client.ErrorHandler;
import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.SmartClient;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.UI;
import org.nuxeo.ecm.platform.gwt.client.ui.UIApplication;
import org.nuxeo.ecm.platform.gwt.client.ui.View;
import org.nuxeo.ecm.platform.gwt.client.ui.ViewContainer;

import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SmartApplication implements UIApplication, Extensible, ExtensionPoints, ErrorHandler {

    protected VLayout layout;
    
    protected View<Canvas> header;
    protected View<Canvas> footer;
    protected View<Canvas> content;
    protected View<Canvas> left;
    protected View<Canvas> right;


    
    public SmartApplication() { 
    }
    
    public void start() {
        // install fixes and workarounds
        SmartClient.install();

        // set error handler
        Framework.setErrorHandler(this);

        // create application layout        
        layout = new VLayout();
//layout.setBorder("1px solid blue");
        layout.setSize("100%", "100%");
        layout.setLayoutMargin(4);
        if (header != null) {
            Canvas canvas = header.getWidget();
            canvas.setHeight("80");            
            layout.addMember(canvas);            
        }
        HLayout main = new HLayout();
        if (left != null) {
            Canvas canvas = SmartClient.getCanvas(left.getWidget());
            canvas.setSize("25%", "100%");
            canvas.setShowResizeBar(true);
            main.addMember(canvas);
        }
        if (content != null) {
            String width = right == null ? "75%" : "50%";
            Canvas canvas = SmartClient.getCanvas(content.getWidget());
            canvas.setSize(width, "100%");
            main.addMember(canvas);            
        }
        if (right != null) {
            Canvas canvas = SmartClient.getCanvas(right.getWidget());
            canvas.setSize("25%", "100%");
            canvas.setShowResizeBar(true);
            main.addMember(canvas);
        }  
        int size = 100;
        if (header != null) size -= 4;
        if (footer != null) size -= 4;  
        main.setHeight("100%");
//main.setBorder("1px solid red");
        layout.addMember(main);
        if (footer != null) {
            Canvas canvas = SmartClient.getCanvas(footer.getWidget());
            canvas.setHeight("4%");
            layout.addMember(canvas);
        }             

        // install into root panel
        RootPanel.get().add(layout);       
    }

    public View<?> getLeftArea() {
        return left;
    }

    public View<?> getRightArea() {
        return right;
    }
    
    public View<?> getContentArea() {
        return content;
    }
    
    public View<?> getHeader() {
        return header;
    }
    
    public View<?> getFooter() {
        return footer;
    }
    
    public View<?> getArea(String name) {
        if (left != null && name.equals(left.getName())) {
            return left;
        }
        if (content != null && name.equals(content.getName())) {
            return content;
        }
        if (right != null && name.equals(right.getName())) {
            return right;
        }
        if (header != null && name.equals(header.getName())) {
            return header;
        }
        if (footer != null && name.equals(footer.getName())) {
            return footer;
        }
        return null;
    }
    
    public View<?> getView(String[] segments) {
        return getView(segments, segments.length);
    }
    
    public View<?> getView(String[] segments, int length) {
        int start = 0;
        if (segments[0].length() == 0) { // an absolute path
             start = 1;
        }
        View<?> view = getArea(segments[start]);
        for (int i=start+1; i<length; i++) {
            if (view instanceof ViewContainer) {
                view = ((ViewContainer<?>)view).getView(segments[i]);
            } else {
                return null;
            }
        }            
        return view;        
    }

    public View<?> getView(String path) {
        String[] segments = path.split("/");
        if (segments.length == 0) {
            return null;
        }         
        return getView(segments);        
    }


    public void showView(String path) {
        String[] segments = path.split("/");
        if (segments.length == 0) {
            return;
        }         
        ViewContainer<?> vc = (ViewContainer<?>)getView(segments, segments.length-1);
        if (vc != null) {
            vc.select(segments[segments.length-1]);
        }
    }
    
    public void openInEditor(Object input) {
        content.setInput(input);
    }
        

    @SuppressWarnings("unchecked")
    public void registerExtension(String target, Object extension) {
        if (LEFT_AREA_XP.equals(target) ) {
            left = (View)extension;
        } else if (CONTENT_AREA_XP.equals(target)) {
            content = (View)extension;
        } else if (RIGHT_AREA_XP.equals(target)) {
            right = (View)extension;
        } else if (HEADER_AREA_XP.equals(target)) {
            header = (View)extension;
        } else if (FOOTER_AREA_XP.equals(target)) {
            footer = (View)extension;
        }
    }
    

    public void handleError(Throwable t) {
        UI.showError(t);
    }
    
 
}
