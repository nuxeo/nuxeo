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

package org.nuxeo.ecm.platform.gwt.client.ui.impl;

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;
import org.nuxeo.ecm.platform.gwt.client.ui.view.DefaultViewManager;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.layout.SectionStack;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewStack extends SmartView implements Extensible {

    protected DefaultViewManager mgr;
    
    public ViewStack() {
        super("views");
        mgr = new DefaultViewManager(new StackContainer());
    }
    
    @Override
    protected void inputChanged() {
        mgr.open(input); 
    }

    public SectionStack createWidget() { 
        setInput(null); // force sections creation
        return ((StackContainer)mgr.getContainer()).getWidget();
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.VIEWS_XP.equals(target)) {
            try {
                View v = (View)extension;
                mgr.addView(v.getName(), v);
            } catch (ClassCastException e) {
                GWT.log("Invalid contribution to extension point: "+ExtensionPoints.VIEWS_XP, e);
            }
        }
    }

}
