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
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Item extends Composite {
    
    protected String name;
    protected boolean isDefault;
    protected int preferredIndex = -1;
    
    protected Item(String name) {
        this.name = name;
        initWidget(createContent());
    }
    
    public Item(String name, Widget widget) {
        this.name = name;
        initWidget(widget);
    }
    
    /**
     * Must override to create the content widget
     * This method is not used if the item is not subclassed  
     * @return
     */
    protected Widget createContent() {
        throw new IllegalStateException("This method must be overrided when Item class is subclassed");
    }
    
    public Widget getWidget() {
        return super.getWidget();
    }
    
    public String getName() {
        return name;
    }
    
    
    /**
     * @return the icon.
     */
    public Image getIcon() {
        return new Image("aaa.gif"); //ApplicationWindow.getEmptyImage();
    }
    
    public int getPreferredIndex() {
        return preferredIndex;
    }
    
    /**
     * @param preferredIndex the preferredIndex to set.
     */
    public void setPreferredIndex(int preferredIndex) {
        this.preferredIndex = preferredIndex;
    }
        
    public String getHeader() {
        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setSpacing(0);
        hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        hPanel.add(getIcon());
        HTML headerText = new HTML(getTitle());
        headerText.setStyleName("cw-StackPanelHeader");
        hPanel.add(headerText);

        // Return the HTML string for the panel
        return hPanel.getElement().getString();        
    }
    
    /**
     * Called by the container (if container supports refresh)
     * when application context change
     */
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
