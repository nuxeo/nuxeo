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

import java.util.ArrayList;
import java.util.Iterator;

import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultViewManager implements ViewManager {

    protected Container container;
    protected ArrayList<ViewSite> sites = new ArrayList<ViewSite>();    


    public DefaultViewManager() {
    }

    public DefaultViewManager(Container mgr) {
        this.container = mgr;
    }

    public void setContainer(Container mgr) {
        this.container = mgr;
    }
    
    public Container getContainer() {
        return container;
    }
    
    public void open(Object input) {
        for (ViewSite vs : sites) {
            vs.open(container, input);
        }
    }
    
    public View[] getViews() {
        View[] views = new View[sites.size()];
        int i = 0;
        for (ViewSite vs : sites) {
            views[i++] = vs.getView();
        }
         return views;
    }
    
    public int getViewsCount() {
        return sites.size();
    }
    
    public void addView(String key, View view) {
        ViewSite vs = new ViewSite(key, view);
        sites.add(vs);
    }
    
    public void removeView(View view) {
        Iterator<ViewSite> it = sites.iterator();
        while (it.hasNext()) {
            ViewSite vs = it.next();
            if (view == vs.getView()) {
                it.remove();
                container.closeSite(vs);
            }
        }
    }
    
    public View getView(String name) {
        ViewSite vs = getViewSite(name);
        return vs == null ? null : vs.getView();
    }
    
    public ViewSite getViewSiteByHandle(Object handle) {
        for (ViewSite vs : sites) {
            if (handle.equals(vs.getHandle())) {
                return vs;
            }
        }
        return null;
    }
    
    public ViewSite getViewSite(String name) {
        for (ViewSite vs : sites) {
            if (name.equals(vs.getName())) {
                return vs;
            }
        }
        return null;
    }
    
    public View getActiveView() {
        Object handle = container.getActiveSiteHandle();
        if (handle != null) {
            ViewSite vs = getViewSiteByHandle(handle);
            return vs == null ? null : vs.getView();
        }
        return null;
    }
    
    public void hideView(String key) {
        ViewSite vs = getViewSite(key);
        if (vs != null) {
            container.disableSite(vs);
        }
    }
    
    public void showView(String key) {
        ViewSite vs = getViewSite(key);
        if (vs != null) {
            container.enableSite(vs);
        }
    }
    
    public void activateView(String key) {
        ViewSite vs = getViewSite(key);
        if (vs != null) {
            container.activateSite(vs);
        }
    }
    
    public void deactivateView(String key) {
        ViewSite vs = getViewSite(key);
        if (vs != null) {
            container.deactivateSite(vs);
        }
    }    
    

}
