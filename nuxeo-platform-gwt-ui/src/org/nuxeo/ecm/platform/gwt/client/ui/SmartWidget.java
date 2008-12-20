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

package org.nuxeo.ecm.platform.gwt.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;

/**
 * Fix the WidgetCanvas implementation ... On resize GWT widgets disappears
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SmartWidget extends Canvas {

    protected Widget  widget;
    
    
    public SmartWidget(Widget widget) {
        this.widget = widget;
        init();
    }

    protected void init() {
        setRedrawOnResize(false);        
        setOverflow(Overflow.VISIBLE);
        setWidth(1);
        setHeight(1);
//        updateSize();
    }

    public String getInnerHTML() {
        if (!isDrawn()) {
            return "<DIV STYLE='width:100%;height:100%' ID=" + this.getID() + "_widget></DIV>";
        } else {
            return "<DIV STYLE='width:100%;height:100%' ID=" + this.getID() + "_widget>"+widget.toString()+"</DIV>";
        }
    }
    
    protected void onDraw() {
        //a GWT widget must be attached to a GWT Panel for its events to fire.
        boolean attached = widget.isAttached();
        if (!attached) {
            RootPanel rp = RootPanel.get(this.getID() + "_widget");
            rp.add(widget);
            //updateSize();
        }
    }

    @Override
    protected void onDestroy() {
        boolean attached = widget.isAttached();
        if (attached) {
            RootPanel.detachNow(widget);
        }
        widget = null;
    }

    protected void updateSize() {
        String width = DOM.getStyleAttribute(widget.getElement(), "width");
        if (width != null && !width.equals("")) {
            setWidth(width);
        }
        String height = DOM.getStyleAttribute(widget.getElement(), "height");
        if (height != null && !height.equals("")) {
            setHeight(height);
        }
    }
    
}

