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
import org.nuxeo.webengine.gwt.client.SessionListener;
import org.nuxeo.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.webengine.gwt.client.ui.ViewContainer;
import org.nuxeo.webengine.gwt.client.ui.login.LoginContainer;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewContainerImpl extends ViewContainer implements Extensible,
        SessionListener {

    protected StackPanel stackPanel; 
    
    
    public ViewContainerImpl() {
        Button button1 = new Button("Show Tabs");
        Button button2 = new Button("Manage");
        
        stackPanel = new StackPanel();
        stackPanel.add(button1, "Navigator");
        stackPanel.add(button2, "Tools");
        LoginContainer login = new LoginContainer();
        Application.addSessionListener(login);
        stackPanel.add(login, "Login");
        stackPanel.ensureDebugId("mainStackPanel");
        stackPanel.setSize("100%", "100%");
        
        button1.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                TabPanel w = new TabPanel();
                Application.getPerspective().openInEditor(w);
            }
        });

        button2.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                HTML w = new HTML("<h1>My Content</h1>Some html text!");
                Application.getPerspective().openInEditor(w);
            }
        });

        initWidget(stackPanel);
  
    }
    
    public void registerExtension(String target, Object extension) {
        // TODO Auto-generated method stub

    }

    public void onSessionEvent(int event) {
        // TODO Auto-generated method stub

    }

    public void register() {
        Application.registerExtension(ExtensionPoints.VIEW_CONTAINER_XP, this);
        Application.registerExtensionPoint("VIEWS", this);
    }


    @Override
    public void showView(String name) {
        // TODO Auto-generated method stub
        
    }
}
