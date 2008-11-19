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
import org.nuxeo.webengine.gwt.client.ui.EditorContainer;
import org.nuxeo.webengine.gwt.client.ui.ExtensionPoints;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EditorContainerImpl extends EditorContainer implements Extensible {
    
    public EditorContainerImpl() {
        SimplePanel panel = new SimplePanel();
        panel.setWidget(new HTML("&nbsp;"));
        panel.setSize("100%", "100%");
        initWidget(panel);
    }
    
    public void setInput(Object input) {
        SimplePanel panel = (SimplePanel)getWidget();        
        if (input instanceof Widget) {
            panel.setWidget((Widget)input);
        } else {
            panel.setWidget(new HTML(input.toString()));
        }
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.EDITORS_XP.equals(target)) {
            //TODO add editor
        } else {
            GWT.log("Unknown extension point: "+target, null);
        }
    }
    
    public void register() {
        Application.registerExtension(ExtensionPoints.EDITOR_CONTAINER_XP, this);
        Application.registerExtensionPoint(ExtensionPoints.EDITORS_XP, this);
    }
    
}
