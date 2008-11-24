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

import org.nuxeo.ecm.webengine.gwt.client.Context;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.DeckPanel;

/**
 * Manage a collection of items. Only one item is visible at a time.
 * When refreshed this container will show the first item that is enabled for the current context.
 * To find out the enabled item the {@link Item#isEnabled(Context)} method is asked.
 * If no item is eanbled for the given context the default item is used.  
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ItemDeck extends StackedItemContainer {
    
    public ItemDeck(String name) {
        super(name);
    }    
    
    @Override
    protected ComplexPanel createPanel() {
        DeckPanel panel = new DeckPanel();
        panel.setSize("100%", "100%");
        return panel;
    }
    
    
    public DeckPanel getDeckPanel() {
        return (DeckPanel)getWidget();
    }
    
    @Override
    public int getSelectedIndex() {
        return getDeckPanel().getVisibleWidget();
    }

    @Override
    public void selectItem(int index) {
        getDeckPanel().showWidget(index);
    }

    @Override
    public void add(Item item) {
        getDeckPanel().add(item);
    }

    @Override
    public void insert(Item item, int beforeIndex) {
        getDeckPanel().insert(item, beforeIndex);
    }

    @Override
    public void refresh() {
        Item item = getSelectedItem();
        if (item != null) {
            item.refresh();
        }
    }

}
