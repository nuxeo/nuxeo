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
import java.util.List;

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.BaseWidget;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Left extends SmartView<SectionStack> implements Extensible {

    protected List<View<?>> contributions;
    
    public Left() {
        super ("left");
        contributions = new ArrayList<View<?>>();
    }
    
    
    
    protected SectionStack createWidget() {
        SectionStack stack = new SectionStack();
        
        if (!contributions.isEmpty()) {
            View<?> view = contributions.get(0); 
            SectionStackSection section = new SectionStackSection(view.getTitle());
            VLayout vl =new VLayout();
            vl.addMember(view.getWidget());
            section.setExpanded(true);
            section.setCanCollapse(true);
            section.addItem(vl);
            stack.addSection(section);
            for (int i=1, len=contributions.size(); i<len; i++) {
                view = contributions.get(i);
//                vl =new VLayout();
//                vl.addMember(view.getWidget());
                section = new SectionStackSection(view.getTitle());
                section.setExpanded(false);
                section.setCanCollapse(true);
                section.addItem((Canvas)view.getWidget());                
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
                contributions.add((View<?>)extension);
            } catch (ClassCastException e) {
                GWT.log("Invalid contribution to extension point: "+ExtensionPoints.VIEWS_XP, e);
            }
        }
    }
    
    @Override
    public void refresh() {
        if (isAttached()) {
            for (View<?> view : contributions) {
                if (view.isAttached() && view.getWidget().isVisible()) {
                    view.refresh();
                }
            }            
        }
    }
    
    protected JavaScriptObject getAttributeAsJavaScriptObject(BaseWidget widget, String attribute) {
        if (widget.isCreated()) {
            return JSOHelper.getAttributeAsJavaScriptObject(widget.getJsObj(), attribute);
        } else {
            return JSOHelper.getAttributeAsJavaScriptObject(widget.getConfig(), attribute);
        }
    }

}
