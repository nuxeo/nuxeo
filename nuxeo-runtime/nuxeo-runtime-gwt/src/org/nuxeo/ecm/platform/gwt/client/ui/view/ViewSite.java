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

package org.nuxeo.ecm.platform.gwt.client.ui.view;

import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.Site;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewSite implements Site {

    protected String name;
    protected View view;
    protected Object handle;
    protected Container container;
    
    public ViewSite(String name, View view) {
        this.name = name;
        this.view = view;
    }
    
    public View getView() {
        return view;
    }
    
    public String getName() {
        return name;
    }
    
    
    public Object getHandle() {
        return handle;
    }

    public String getTitle() {
        String title = view.getTitle();
        return title == null ? name : title;
    }
    
    public String getIcon() {
        return view.getIcon();
    }
    
    public void open(Container container, Object input) {
        this.container = container;
        View v = findViewForInput(input);
        if (v == null) {
            if (handle != null) {
                container.disableSite(this);
            }
            return;            
        }
        if (handle == null) {
            handle = container.createHandle(this);
        }        
        if (!v.isInstalled() || v != view) {
            view = v;
            v.install(this, input);
            container.installWidget(this);
            container.updateSiteTitle(this); 
            container.updateSiteIcon(this);
        } else {
            view = v; // set the current view before calling setInput()
            v.setInput(input);
        }
    }
    
    protected View findViewForInput(Object input) {
        return view.acceptInput(input) ? view : null;
    }

    
    public void updateIcon() {
        if (container != null) {
            container.updateSiteIcon(this);
        }         
    }
    
    public void updateTitle() {
        if (container != null) {
            container.updateSiteTitle(this);
        }
    }
    
    public void updateWidget() {
        if (container != null) {
            container.installWidget(this);        
        }        
    }
    
    public void enable() {
        container.enableSite(this);
    }
    
    public void disable() {
        container.disableSite(this);
    }
    
    public void activate() {
        container.activateSite(this);
    }
    
    public void deactivate() {
        container.deactivateSite(this);
    }
    
    public void close() {
        container.closeSite(this);
    }
    
    public boolean isActive() {
        return container.isSiteActive(this);
    }
    
    public boolean isEnabled() {
        return container.isSiteEnabled(this);
    }
    
}
