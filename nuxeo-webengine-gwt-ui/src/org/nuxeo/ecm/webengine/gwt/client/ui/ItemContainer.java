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

package org.nuxeo.ecm.webengine.gwt.client.ui;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * An item container is a composite that wraps a complex panel.
 * It is used to manage items
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ItemContainer extends Item {


    public ItemContainer(String name) {
        super(name);
    }
    
    @Override
    protected final Widget createContent() {
        return createPanel();
    }

    protected abstract ComplexPanel createPanel();
        
    public abstract void add(Item item);
    
    public abstract void insert(Item item, int beforeIndex);
        
    public boolean insertBefore(Item item, String name) {
        int index = indexOf(name);
        if (index > -1) {
            insert(item, index);
            return true;
        }
        return false; 
    }
    
    
    public ComplexPanel getPanel() {
        return (ComplexPanel)getWidget();
    }
    
    
    public int indexOf(Item item) {
        return getPanel().getWidgetIndex(item);
    }
    
    public int indexOf(String name) {
        ComplexPanel panel = getPanel();
        int cnt = panel.getWidgetCount();
        for (int i=0; i<cnt; i++) {
            Item item = (Item)panel.getWidget(i);
            if (name.equals(item.getName())) {
                return i;
            }
        }        
        return -1;        
    }
    
    public Item findItem(String name) {
        ComplexPanel panel = getPanel();
        int cnt = panel.getWidgetCount();
        for (int i=0; i<cnt; i++) {
            Item item = (Item)panel.getWidget(i);
            if (name.equals(item.getName())) {
                return item;
            }
        }        
        return null;
    }
    
    public Item getItem(int index) {
        return (Item)getPanel().getWidget(index);
    }
    
    public int getItemsCount() {
        return getPanel().getWidgetCount();
    }
    
    public Item[] getItems() {
        ComplexPanel panel = getPanel();
        Item[] items = new Item[panel.getWidgetCount()];
        for (int i=0; i<items.length; i++) {
            items[i] = (Item)panel.getWidget(i);
        }
        return items;
    }
    
    public void refresh() {
        ComplexPanel panel = getPanel();
        for (int i=0, cnt=panel.getWidgetCount(); i<cnt; i++) {
            Widget w = panel.getWidget(i);
            if (w.isVisible()) {
                ((Item)w).refresh();
            }
        }
    }

}

