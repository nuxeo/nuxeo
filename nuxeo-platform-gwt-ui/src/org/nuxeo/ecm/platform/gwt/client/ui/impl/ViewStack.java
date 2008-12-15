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

import java.util.ArrayList;

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.ui.ControlContainer;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartViewContainer;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewStack extends SmartViewContainer<SectionStack> implements Extensible {

    public ViewStack() {
        super ("views");
        views = new ArrayList<View<?>>();
    }
    
        
    protected SectionStack createWidget() {
        SectionStack stack = new SectionStack();        
        if (!views.isEmpty()) {
            View<?> view = views.get(0); 
            SectionStackSection section = new SectionStackSection(view.getTitle());
            VLayout vl =new VLayout();
            vl.addMember(view.getWidget());
            section.setExpanded(true);
            section.setCanCollapse(true);
            section.addItem(vl);
            if (view instanceof ControlContainer) {
                section.setControls(((ControlContainer)view).getControls());
            }
            stack.addSection(section);
            for (int i=1, len=views.size(); i<len; i++) {
                view = views.get(i);
                section = new SectionStackSection(view.getTitle());
                section.setExpanded(false);
                section.setCanCollapse(true);
                section.addItem((Canvas)view.getWidget());
                if (view instanceof ControlContainer) {
                    section.setControls(((ControlContainer)view).getControls());
                }
                stack.addSection(section);
            }        
        }
        stack.setVisibilityMode(VisibilityMode.MULTIPLE);
        stack.setOverflow(Overflow.HIDDEN);
        stack.setAnimateSections(Boolean.parseBoolean(Framework.getSetting("animations", "false")));
        stack.setHeight100();
        return stack;
    }
    
    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.VIEWS_XP.equals(target)) {
            try {
                views.add((View<?>)extension);
            } catch (ClassCastException e) {
                GWT.log("Invalid contribution to extension point: "+ExtensionPoints.VIEWS_XP, e);
            }
        }
    }    

    
    public void selectView(int i) {
        widget.expandSection(i);
    }
        
}
