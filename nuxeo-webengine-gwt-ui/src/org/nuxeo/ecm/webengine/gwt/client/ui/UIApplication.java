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

import org.nuxeo.ecm.webengine.gwt.client.Application;
import org.nuxeo.ecm.webengine.gwt.client.Framework;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class UIApplication extends Composite implements Application {

      
    public void start() {
        RootPanel.get().add(this);
    }  

    public void openInEditor(Object input) {
        Framework.getContext().setInputObject(input);
        openEditor();
    }
    
    public void openInEditor(String name, Object input) {
        Framework.getContext().setInputObject(input);
        openEditor(name);
    }
    
    public abstract void showView(String name);
    
    public abstract void openEditor();
    
    public abstract void openEditor(String name);
    
    //TODO add getView , showView etc
}
