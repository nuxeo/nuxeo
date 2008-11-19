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

package org.nuxeo.webengine.gwt.client.ui;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Item extends Composite {

    protected String name;
    protected Image icon;
    protected boolean isDefault;
    
    public Item(String name) {
        this (name, new SimplePanel());
    }
    
    public Item(String name, Widget widget) {
        this.name = name;
        initWidget(widget);
    }
    
    public Widget getWidget() {
        return super.getWidget();
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * @param icon the icon to set.
     */
    public void setIcon(Image icon) {
        this.icon = icon;
    }
    
    /**
     * @return the icon.
     */
    public Image getIcon() {
        return icon;
    }
        
    public String getHeader() {
        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setSpacing(0);
        hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        if (icon != null) {
            hPanel.add(icon);
        }
        HTML headerText = new HTML(getTitle());
        headerText.setStyleName("cw-StackPanelHeader");
        hPanel.add(headerText);

        // Return the HTML string for the panel
        return hPanel.getElement().getString();        
    }
    
    public void refresh() {
        
    }
    
    protected boolean isDefault() {
        return isDefault;
    }
    
    /**
     * @param isDefault the isDefault to set.
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
}
