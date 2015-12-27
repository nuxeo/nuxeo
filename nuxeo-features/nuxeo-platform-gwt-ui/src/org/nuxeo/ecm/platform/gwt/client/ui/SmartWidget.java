/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
