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
public abstract class AbstractView implements View {

    protected Object input; 
    protected Site site;
    protected String name;
    protected Widget widget;
    
    
    public AbstractView(String name) {
        this.name = name;
    }   
    
    public void install(Site site, Object input) {
        this.site = site;
        this.input = input;
        getWidget(); // create the widget
        initInput();        
    }
    
    protected void initInput() {
        inputChanged();
    }
    
    public void uninstall() {
        widget = null;
        input = null;
        site = null;
    }
    
    public boolean isInstalled() {
        return site != null;
    }    
    
    public boolean hasWidget() {
        return widget != null;
    }
    
    public String getName() {
        return name;
    }
    
    public String getTitle() {
        return null;
    }
    
    public String getIcon() {
        return null;
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
    
    public Widget getWidget() {
        if (widget == null) {
            widget = createWidget();
        }
        return widget;
    }

    /**
     * Create a widget to be bound to this view
     * @return the new widget. must be never null.
     */
    protected abstract Widget createWidget();
    
    
    public boolean acceptInput(Object input) {
        return true;
    }
    
    public void setInput(Object input) {
        if (this.input != input) {
            this.input = input; 
            inputChanged();
        }
    }
    
    /**
     * May override this to notify the site about title or icon updates.
     */
    protected void inputChanged() {
        
    }
    
    public Object getInput() {
        return input;
    }
    

    @Override
    public String toString() {
        return "View: "+name+" ["+getClass().getName()+"]";
    }
}
