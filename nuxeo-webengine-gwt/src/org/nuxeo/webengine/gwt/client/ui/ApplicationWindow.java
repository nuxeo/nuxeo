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

import org.nuxeo.webengine.gwt.client.Application;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ApplicationWindow extends Composite {

    private static Image EMPTY_IMAGE = null;  
    
    public void install() {
        RootPanel.get().add(this);
    }

    public static Image getEmptyImage() {
        if (EMPTY_IMAGE == null) {
            EMPTY_IMAGE = Application.getImages(Images.class).noimage().createImage();
        }
        return EMPTY_IMAGE;
    }

    public void openInEditor(Object input) {
        Application.getContext().setInputObject(input);
        openEditor();
    }
    
    public abstract void showView(String name);
    
    public abstract void openEditor();
    
    //TODO add getView , showView etc
}
