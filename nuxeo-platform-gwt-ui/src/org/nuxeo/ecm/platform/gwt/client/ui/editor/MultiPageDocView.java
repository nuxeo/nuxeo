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

import org.nuxeo.ecm.platform.gwt.client.model.Document;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;
import org.nuxeo.ecm.platform.gwt.client.ui.view.MultiPageViewManager;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MultiPageDocView extends SmartView {
 
    protected MultiPageViewManager mgr;
    protected DocumentHeader header;
    
    public MultiPageDocView() {
        super ("mpe");
        this.mgr = new MultiPageViewManager(new TabsContainer());
    }
    
    public Document getDocument() {
        return (Document)input;
    }

    @Override
    protected void inputChanged() {
        site.updateTitle();
        refresh();
    }
    
    @Override
    public void refresh() {
        header.update(getDocument());
        mgr.open(input);
    }

//    @Override
//    public String getIcon() {
//        return Framework.getSkinPath("images/document.png");
//    }
    
    @Override
    public String getTitle() {
        return getDocument().getTitle();
    }

    public void addPage(String key, View view) {
        mgr.addView(key, view);
    }
    
    @Override
    protected Canvas createWidget() {
        VLayout panel = new VLayout();
        Canvas header = createHeader();        
        if (header != null) {
            panel.addMember(header);
        }
        TabSet tabs = ((TabsContainer)mgr.getContainer()).getWidget();
        tabs.setHeight100();
        panel.addMember(tabs);
        return panel;
    }
    
    public Canvas createHeader() {
        if (header == null) {
            header = new DocumentHeader(this);
        }
        return header; 
    }
    
}
