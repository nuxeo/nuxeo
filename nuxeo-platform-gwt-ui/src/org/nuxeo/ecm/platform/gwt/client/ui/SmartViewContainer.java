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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class SmartViewContainer<W extends Canvas> extends AbstractViewContainer<W> 
    implements ViewContainer<W>, DrawHandler {

    public SmartViewContainer(String name) {
        super (name);
    }

    protected boolean destroyWidget(W widget) {
        widget.destroy();
        return true;
    }
    
    @Override
    public void detachWidget() {
        if (widget != null) {
            Canvas parent = widget.getParentElement();
            if (parent != null) {
                parent.removeChild(widget);
            }
        }
    }
    
    @Override
    public boolean isVisible() {
        return widget != null && widget.isDrawn() && widget.isVisible();
    }
    
    @Override
    protected void onAttach() {
        //widget.setProperty("nuxeo_viewId", name);
        widget.addDrawHandler(this);
    }    
  
    /**
     * Widget was drawn 
     */
    public void onDraw(DrawEvent event) {
//        System.out.println("on draw "+name);
    }

}
