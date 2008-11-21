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

package org.nuxeo.ecm.webengine.gwt.client.ui.impl;

import org.nuxeo.ecm.webengine.gwt.client.Application;
import org.nuxeo.ecm.webengine.gwt.client.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.Extensible;
import org.nuxeo.ecm.webengine.gwt.client.ui.ApplicationWindow;
import org.nuxeo.ecm.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.webengine.gwt.client.ui.Item;
import org.nuxeo.ecm.webengine.gwt.client.ui.ViewContainer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewContainerImpl extends ViewContainer implements Extensible,
        ContextListener {

    
    
    public ViewContainerImpl() {
        
        StackPanel stackPanel = new StackPanel();
        stackPanel.ensureDebugId("mainStackPanel");
        stackPanel.setSize("100%", "100%");
        
        initWidget(stackPanel);
    }

    public StackPanel getStackPanel() {
        return (StackPanel)getWidget();        
    }
    
    public void registerExtension(String target, Object extension, int type) {
        if (ExtensionPoints.VIEWS_XP.equals(target)) {
            if (extension instanceof Item) {
                Item item = (Item)extension;
                getStackPanel().add(item, item.getHeader(), true); //TODO image
            } else if (extension instanceof Widget) {
                Widget w = (Widget)extension;
                getStackPanel().add(w, getHeaderString(w.getTitle(), ApplicationWindow.getEmptyImage()), true); 
            } else {
                GWT.log("Extension is not a widget. Ignoring", null);
            }
        } else {
            GWT.log("Unknown extension point: "+target, null);
        }
    }

    public void onSessionEvent(int event) {
        // TODO Auto-generated method stub

    }

    public ViewContainerImpl register() {
        Application.registerExtension(ExtensionPoints.VIEW_CONTAINER_XP, this);
        Application.registerExtensionPoint(ExtensionPoints.VIEWS_XP, this);
        return this;
    }


    @Override
    public void showView(String name) {
        StackPanel panel = getStackPanel();
        int cnt = panel.getWidgetCount();
        for (int i=0; i<cnt; i++) {
            Item item = (Item)panel.getWidget(i);
            if (name.equals(item.getName())) {
                panel.showStack(i);
            }
        }
    }
    
    
    
    
    /**
     * Get a string representation of the header that includes an image and some
     * text.
     * 
     * @param text the header text
     * @param image the {@link AbstractImagePrototype} to add next to the header
     * @return the header as a string
     */
    protected String getHeaderString(String text, AbstractImagePrototype image) {
        return getHeaderString(text, image.createImage());
    }
    protected String getHeaderString(String text, Image image) {
        // Add the image and text to a horizontal panel
        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setSpacing(0);
        hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        hPanel.add(image);
        HTML headerText = new HTML(text);
        headerText.setStyleName("cw-StackPanelHeader");
        hPanel.add(headerText);

        // Return the HTML string for the panel
        return hPanel.getElement().getString();        
    }

}
