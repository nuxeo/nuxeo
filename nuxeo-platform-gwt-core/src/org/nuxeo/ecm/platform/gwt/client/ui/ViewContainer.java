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
 * A view container manage views. it is usually bound to a container widget.
 * Note that views must be added to the container the container is attached to an widget.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public interface ViewContainer<W extends Widget> extends View<W> {

    public int indexOf(View<?> view);
    
    public int indexOf(String name);
    
    public void add(View<?> view);

    public void insertBefore(View<?> view, int beforeIndex);

    public boolean insertBefore(View<?> view, String name);
    
    public void remove(int index);
    
    public void remove(String name);
        
    public View<?> getView(String name);
    
    public View<?> getView(int index);

    public int getViewsCount();

    public View<?>[] getViews();

    public void select(int i);
    
    public void select(String name);

    public int getSelectedIndex();
    
    public View<?> getSelectedView();    

}
