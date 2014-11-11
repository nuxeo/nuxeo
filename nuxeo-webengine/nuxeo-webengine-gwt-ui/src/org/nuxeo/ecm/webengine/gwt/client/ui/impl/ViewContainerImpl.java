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

import org.nuxeo.ecm.webengine.gwt.client.Extensible;
import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.ui.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.webengine.gwt.client.ui.View;
import org.nuxeo.ecm.webengine.gwt.client.ui.ViewStack;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewContainerImpl extends ViewStack implements Extensible,
        ContextListener {


    public ViewContainerImpl() {
        super("view_container");
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        UI.addContextListener(this);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        UI.removeContextListener(this);
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.VIEWS_XP.equals(target)) {
            if (extension instanceof View) {
                View item = (View)extension;
                add(item);
            } else if (extension instanceof Widget) {
                Widget w = (Widget)extension;
                getStackPanel().add(w, View.getHeaderString(w.getTitle(), UI.getEmptyImage()), true);
            } else {
                GWT.log("Extension is not a widget. Ignoring", null);
            }
        } else {
            GWT.log("Unknown extension point: "+target, null);
        }
    }

    public void onContextEvent(int event) {
        refresh();
    }


    public ViewContainerImpl register() {
        Framework.registerExtension(ExtensionPoints.VIEW_CONTAINER_XP, this);
        Framework.registerExtensionPoint(ExtensionPoints.VIEWS_XP, this);
        return this;
    }

}
