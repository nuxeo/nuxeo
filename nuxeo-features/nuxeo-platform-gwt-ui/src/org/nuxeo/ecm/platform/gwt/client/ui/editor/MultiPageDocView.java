/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
