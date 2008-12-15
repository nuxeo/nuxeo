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

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.user.client.ui.Widget;

/**
 * A view container manage views. it is usually bound to a container widget.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public abstract class AbstractViewContainer<W extends Widget> extends AbstractView<W> implements ViewContainer<W> {

    protected int selectedIndex = -1;
    protected List<View<?>> views;
    
    public AbstractViewContainer(String name) {
        super (name);
        views = new ArrayList<View<?>>();
    }
    
    public View<?>[] getViews() {
        return views.toArray(new View[views.size()]);
    }
    
    public int indexOf(View<?> view) {
        return indexOf(view.getName());
    }
    
    @Override
    public void destroy() {
        for (View<?> view : views) {
            view.destroy();
        }
        super.destroy();
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public View<?> getSelectedView() {
        return views.get(getSelectedIndex());
    }
    
    public void select(int i) {
        this.selectedIndex = i;
        selectView(i);
    }
    
    protected abstract void selectView(int i);
    
    public int indexOf(String name) {
        for (int i=0,len=views.size(); i<len; i++) {
            if (name.equals(views.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }
    
    public int getViewsCount() {
        return views.size();
    }
    
    public View<?> getView(int index) {
        return views.get(index);
    }
    
    public View<?> getView(String name) {
        for (View<?> view : views) {
            if (view.getName().equals(name)) {
                return view;
            }
        }
        return null;
    }    

    public boolean insertBefore(View<?> view, String name) {
        int index = indexOf(name);
        if (index > -1) {
            insertBefore(view, index);
            return true;
        }
        return false;
    }

    public void select(String name) {
        select(indexOf(name));
    }
    

    public void refresh() {        
        View<?>[] views = getViews();
        for (int i = 0; i < views.length; i++) {
            if (views[i].isVisible()) {
                views[i].refresh();
            }
        }
    }

    
    public void add(View<?> view) {
        views.add(view);  
    }
    
    public void insertBefore(View<?> view, int beforeIndex) {
        views.add(beforeIndex, view);
    }
    
    public void remove(int index) {
        views.remove(index);
    }
    
    public void remove(String name) {
        remove(indexOf(name));
    }
    
    
}
