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

package org.nuxeo.webengine.gwt.client.ui.impl;

import org.nuxeo.webengine.gwt.client.Application;
import org.nuxeo.webengine.gwt.client.Extensible;
import org.nuxeo.webengine.gwt.client.ui.ApplicationWindow;
import org.nuxeo.webengine.gwt.client.ui.EditorContainer;
import org.nuxeo.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.webengine.gwt.client.ui.ViewContainer;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationWindowImpl extends ApplicationWindow implements Extensible, ExtensionPoints  {

    protected SimplePanel editorContainer;
    protected SimplePanel viewContainer;
    
    
    public ApplicationWindowImpl() {
        DockPanel panel = new DockPanel();
        panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        panel.setSpacing(4);
        panel.setSize("100%", "100%");
        
        Widget header = createHeader();
        panel.add(header, DockPanel.NORTH);
        Widget footer = createFooter();
        panel.add(footer, DockPanel.SOUTH);
        
        panel.setCellHeight(header, "4%");
        panel.setCellHeight(footer, "4%");

        viewContainer = new SimplePanel();
        viewContainer.setSize("100%", "100%");
        panel.add(viewContainer, DockPanel.WEST);
        panel.setCellHeight(viewContainer, "92%");
        panel.setCellWidth(viewContainer, "25%");

        editorContainer = new SimplePanel();
        editorContainer.setSize("100%", "100%");
        panel.add(editorContainer, DockPanel.EAST);
        panel.setCellWidth(editorContainer, "75%");

        panel.setBorderWidth(3);
        panel.setPixelSize(Window.getClientWidth(), Window.getClientHeight());
        initWidget(panel);
    }
    
    protected void setViewContainer(ViewContainer container) {        
        this.viewContainer.setWidget(container);
    }

    protected void setEditorContainer(EditorContainer container) {
        this.editorContainer.setWidget(container);
    }

    public void install() {
        if (editorContainer.getWidget() == null) {
            setEditorContainer(new EditorContainerImpl());
        }
        if (viewContainer.getWidget() == null) {
            setViewContainer(new ViewContainerImpl());
        }
        RootPanel.get().add(this);
    }

    public Widget createHeader() {
        Grid grid = new Grid(1, 3);
        grid.setCellPadding(2);
        grid.setBorderWidth(1);
        grid.setSize("100%", "100%");
        grid.getCellFormatter().setAlignment(0, 0,
                HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_MIDDLE);
        grid.getCellFormatter().setHorizontalAlignment(0, 1,
                HasHorizontalAlignment.ALIGN_RIGHT);
        grid.getCellFormatter().setHorizontalAlignment(0, 2,
                HasHorizontalAlignment.ALIGN_RIGHT);
        grid.setWidget(0, 0, new Image("http://google-web-toolkit-doc-1-5.googlecode.com/svn/wiki/gwt-logo.png"));
        grid.setWidget(0, 1, new HTML("&nbsp;"));
        grid.setWidget(0, 2, new HTML("LOGIN"));
        return grid;
    }

    public Widget createFooter() {
        HTML html = new HTML("Nuxeo ...");
        html.setSize("100%", "100%");
        return html;
    }
    

    public EditorContainer getEditorContainer() {
        return (EditorContainer)editorContainer.getWidget();
    }
    
    public void openInEditor(Object input) {
        getEditorContainer().setInput(input);
    }
    
    public void registerExtension(String target, Object extension) {
        if (VIEW_CONTAINER_XP.equals(target) ) {
            setViewContainer((ViewContainer)extension);
        } else if (EDITOR_CONTAINER_XP.equals(target)) {
            setEditorContainer((EditorContainer)extension); 
        } else if (HEADER_CONTAINER_XP.equals(target)) {
        
        } else if (FOOTER_CONTAINER_XP.equals(target)) {
            
        }
    }
    
    public void register() {
        Application.registerExtension(Application.APPLICATION_WINDOW_XP, this);
        Application.registerExtensionPoint(VIEW_CONTAINER_XP, this);
        Application.registerExtensionPoint(EDITOR_CONTAINER_XP, this);
        Application.registerExtensionPoint(HEADER_CONTAINER_XP, this);
        Application.registerExtensionPoint(FOOTER_CONTAINER_XP, this);
    }

    
}
