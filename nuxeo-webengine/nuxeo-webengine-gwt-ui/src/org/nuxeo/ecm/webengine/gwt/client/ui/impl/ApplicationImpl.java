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

import org.nuxeo.ecm.webengine.gwt.client.ErrorHandler;
import org.nuxeo.ecm.webengine.gwt.client.Extensible;
import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.ui.EditorContainer;
import org.nuxeo.ecm.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.webengine.gwt.client.ui.StackedViewContainer;
import org.nuxeo.ecm.webengine.gwt.client.ui.UIApplication;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationImpl extends UIApplication implements Extensible, ExtensionPoints, ErrorHandler  {

    protected SimplePanel headerPanel;
    protected SimplePanel footerPanel;
    protected SimplePanel editorContainer;
    protected SimplePanel viewContainer;


    public ApplicationImpl() {
        DockPanel panel = new DockPanel();
        panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        panel.setSpacing(2);
        panel.setSize("100%", "100%");

        headerPanel = new SimplePanel();
        panel.add(headerPanel, DockPanel.NORTH);
        footerPanel = new SimplePanel();
        panel.add(footerPanel, DockPanel.SOUTH);

        panel.setCellHeight(headerPanel, "4%");
        panel.setCellHeight(footerPanel, "4%");

        viewContainer = new SimplePanel();
        viewContainer.setSize("100%", "100%");
        panel.add(viewContainer, DockPanel.WEST);
        panel.setCellHeight(viewContainer, "92%");
        panel.setCellWidth(viewContainer, "25%");

        editorContainer = new SimplePanel();
        editorContainer.setSize("100%", "100%");
        panel.add(editorContainer, DockPanel.EAST);
        panel.setCellWidth(editorContainer, "75%");

        //panel.setBorderWidth(3);
        panel.setPixelSize(Window.getClientWidth(), Window.getClientHeight());
        initWidget(panel);
    }


    @Override
    protected void onAttach() {
        super.onAttach();
        // if no contributions were made initialize with default values
        if (editorContainer.getWidget() == null) {
            new EditorContainerImpl().register();
        }
        if (viewContainer.getWidget() == null) {
            new ViewContainerImpl().register();
        }
        if (headerPanel.getWidget() == null) {
            headerPanel.setWidget(new DefaultHeader());
        }
        if (footerPanel.getWidget() == null) {
            footerPanel.setWidget(new DefaultFooter());
        }
        Framework.setErrorHandler(this);
    }


    protected void setViewContainer(StackedViewContainer container) {
        this.viewContainer.setWidget(container);
    }

    protected void setEditorContainer(StackedViewContainer container) {
        this.editorContainer.setWidget(container);
    }

    protected void setHeader(Widget header) {
        this.headerPanel.setWidget(header);
    }

    protected void setFooter(Widget footer) {
        this.footerPanel.setWidget(footer);
    }

    public EditorContainer getEditorContainer() {
        return (EditorContainer)editorContainer.getWidget();
    }

    public StackedViewContainer getViewContainer() {
        return (StackedViewContainer)viewContainer.getWidget();
    }

    public void openInEditor(Object input) {
        getEditorContainer().open(input);
    }

    @Override
    public void showView(String name) {
        getViewContainer().selectView(name);
    }

    public void registerExtension(String target, Object extension) {
        if (VIEW_CONTAINER_XP.equals(target) ) {
            setViewContainer((StackedViewContainer)extension);
        } else if (EDITOR_CONTAINER_XP.equals(target)) {
            setEditorContainer((EditorContainer)extension);
        } else if (HEADER_CONTAINER_XP.equals(target)) {
            setHeader((Widget)extension);
        } else if (FOOTER_CONTAINER_XP.equals(target)) {
            setFooter((Widget)extension);
        }
    }

    public ApplicationImpl register() {
        Framework.registerExtension(Framework.APPLICATION_XP, this);
        Framework.registerExtensionPoint(VIEW_CONTAINER_XP, this);
        Framework.registerExtensionPoint(EDITOR_CONTAINER_XP, this);
        Framework.registerExtensionPoint(HEADER_CONTAINER_XP, this);
        Framework.registerExtensionPoint(FOOTER_CONTAINER_XP, this);
        return this;
    }


    public void handleError(Throwable t) {
        UI.showError(t);
    }

}
