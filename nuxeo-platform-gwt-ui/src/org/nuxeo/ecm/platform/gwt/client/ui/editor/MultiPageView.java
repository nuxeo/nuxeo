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

package org.nuxeo.ecm.platform.gwt.client.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class MultiPageView extends SmartView<Canvas> {

    protected List<Page> pages;
    
    public MultiPageView(String name) {
        super (name);
        this.pages = new ArrayList<Page>();
    }

    public void addPage(View<?> view) {
        String name = view.getName();
        for (Page page : pages) {
            if (name.equals(page.getName())) {
                page.views.add(view);
                return;
            }
        }
        pages.add(new Page(view));
    }   
    
    public Page[] getPages() {
        return pages.toArray(new Page[pages.size()]);
    }

    @Override
    public void setInput(Object input) {   
        for (Page page : pages) { //TODO only visible pages
            
            //if (page.panel.isVisible()) {
                page.setInput(input);                
            //}
        }
    }
        
    @Override
    protected Canvas createWidget() {
        VLayout panel = new VLayout();
        TabSet tabs = new TabSet();
        tabs.setTabBarPosition(Side.BOTTOM);
        for (Page page : pages) {
            Tab tab = new Tab();
            tab.setTitle(page.getTitle());
            tab.setIcon(page.getIcon());
            tab.setPane(page.panel);
            tabs.addTab(tab);
        }
        Canvas header = createHeader();        
        if (header != null) {
            panel.addMember(header);
        }
        tabs.setHeight100();
        panel.addMember(tabs);
        return panel;
    }
    
    public Canvas createHeader() {
        return null;
    }
    
    public class Page {     
        public View<?> selectedView;
        public List<View<?>> views;
        public VLayout panel;
        public Page(View<?> view) {
            views = new ArrayList<View<?>>();
            views.add(view);
            panel = new VLayout();
        }
        public String getName() {
            return views.get(0).getName();
        }
        public String getTitle() {
            return views.get(0).getTitle();
        }
        public String getIcon() {
            return views.get(0).getIcon();
        }
        public Canvas getWidget() {
            return panel;
        }
        public void setInput(Object input) {
            for (View<?> view : views) {
                if (view.acceptInput(input)) {
                    view.setInput(input);
                    if (selectedView != null) {
                        if (selectedView == view) {
                            return;
                        } else {
                            selectedView.detachWidget();
                            selectedView = view;
                        }
                    } else {
                        selectedView = view;
                    }
                    panel.addMember(selectedView.getWidget());
                    return;
                }
            }
        }
    }
    
}
