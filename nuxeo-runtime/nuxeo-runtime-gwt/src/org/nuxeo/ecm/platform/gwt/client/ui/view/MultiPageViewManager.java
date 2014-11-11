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

import java.util.Iterator;

import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.View;



/**
 * 
 * A view manager that can be used to build multi-page views (that are usually using tabs).
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MultiPageViewManager extends DefaultViewManager {

    public MultiPageViewManager() {
    }

    public MultiPageViewManager(Container mgr) {
        super(mgr);
    }

    /**
     * Refresh tabs to display the given input
     * @param input
     */
    public void open(Object input) {
        //container.clear(); // tabs impl dont know to hide tabs - we must reset them .. TODO
        super.open(input);
    }
    
    @Override
    public void addView(String key, View view) {
        ViewPageSite site = (ViewPageSite)getViewSite(key);
        if (site == null) {
            site = new ViewPageSite(key, view);
            sites.add(site);
        } else {
            site.addView(view);
        }
    }
    
    @Override
    public void removeView(View view) {
        Iterator<ViewSite> it = sites.iterator();
        while (it.hasNext()) {
            ViewPageSite s = (ViewPageSite)it.next(); 
            if (view == s.getView()) {
                if (s.isEmpty()) {
                    it.remove();    
                } else {
                    s.removeView(view);
                }
            }
        }
    }
    
}
