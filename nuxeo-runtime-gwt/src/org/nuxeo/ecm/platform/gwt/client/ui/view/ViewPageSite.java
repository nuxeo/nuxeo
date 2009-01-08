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

import org.nuxeo.ecm.platform.gwt.client.ui.View;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewPageSite extends ViewSite {

    protected ArrayList<View> views;
    
    public ViewPageSite(String name, View view) {
        super (name, view);
        views = new ArrayList<View>();
        views.add(view);
    }
    
    
    public void addView(View view) {
        views.add(view);
    }
    
    public void removeView(View view) {
        views.remove(view);
    }
    
    public View[] getViews() {
        return views.toArray(new View[views.size()]);
    }
    
    public boolean isEmpty() {
        return views.isEmpty();
    }
    
    @Override
    protected View findViewForInput(Object input) {
        for (View v : views) {
            if (v.acceptInput(input)) {
                return v;
            }           
        }
        return null;
    }
    
}
