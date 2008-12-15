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


import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractView<W extends Widget> implements View<W> {

    protected String name;
    protected String title;
    protected String icon;
    protected W widget;
    
    
    public AbstractView(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public String getTitle() {
        return title == null ? name : title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public void refresh() {
        // do nothing
    }
    
    public void showBusy() {
        UI.showBusy();
    }

    public void hideBusy() {
        UI.hideBusy();
    }
    
    public W getWidget() {
        if (widget == null) {
            widget = createWidget();
            onAttach();
        }
        return widget;
    }
    
    public boolean isAttached() {
        return widget != null;
    }
    
    public boolean isVisible() {
        return widget != null && widget.isVisible();
    }

    public void destroy() {
        if (widget != null) {
            onDetach();
            if (destroyWidget(widget)) {
                widget = null;
            }
        }        
    }
      
    /**
     * Create a widget to be bound to this view
     * @return the new widget. must be never null.
     */
    protected abstract W createWidget();
    
    /**
     * Destroy the given widget.
     * If widget cannot be destroyed (e.g. destroy not supported) return false, otherwise destroy it and return true.
     * If true is returned the widget will be unbound from the view.
     * @param widget the widget to destroy.
     * @return whether or not the widget was destroyed
     */
    protected boolean destroyWidget(W widget) {
        // don't know how to destroy a GWT widget - this method will do the same as detachWidget
        widget.removeFromParent(); 
        return true;
    }
    
    public void detachWidget() {
        if (widget != null) {
            widget.removeFromParent();
        }
    }
    
    /**
     * This view was attached to a widget. 
     * Override this to add custom initialization after attaching to an widget
     */
    protected void onAttach() {
        // do nothing
    }
    
    /**
     * This view is about to detach from a widget.
     *  Override this to add custom disposal code before the view is detached
     */
    protected void onDetach() {
        // do nothing
    }
    
    public boolean acceptInput(Object input) {
        return false;
    }
    
    public void setInput(Object input) {
        
    }
    
    public Object getInput() {
        return null;
    }

    @Override
    public String toString() {
        return "View: "+name+" ["+getClass().getName()+"]";
    }
}
