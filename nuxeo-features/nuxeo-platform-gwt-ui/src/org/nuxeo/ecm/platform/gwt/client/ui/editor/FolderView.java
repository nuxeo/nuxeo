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

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.model.Document;
import org.nuxeo.ecm.platform.gwt.client.model.GetDocument;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.UI;
import org.nuxeo.ecm.platform.gwt.client.ui.navigator.ChildrenDS;

import com.google.gwt.user.client.Window;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FolderView extends SmartView implements CellDoubleClickHandler {

    protected ListGrid grid;

    public FolderView() {
        super ("view");
    }

    @Override
    public void uninstall() {
        super.uninstall();
        grid = null;
    }


    public Document getDocument() {
        return (Document)input;
    }

    @Override
    public boolean acceptInput(Object input) {
        return ((Document)input).isFolder();
    }

    @Override
    protected Canvas createWidget() {
        VLayout panel = new VLayout();
        ToolStrip tbar = createToolbar();
        panel.addMember(tbar);
        grid = createGrid();
        panel.addMember(grid);
        return panel;
    }

    protected ListGrid createGrid() {
        ListGrid grid = new ListGrid();
        ListGridField fIcon = new ListGridField("type", "&nbsp;");
        fIcon.setWidth(20);
        fIcon.setType(ListGridFieldType.IMAGE);
        fIcon.setImageURLPrefix(Framework.getSkinPath()+"/images/types/");
        fIcon.setImageURLSuffix(".gif");
        ListGridField fTitle = new ListGridField("title", "Title");
        fTitle.setType(ListGridFieldType.TEXT);
        grid.setFields(fIcon, fTitle);
        grid.setDataSource(ChildrenDS.getInstance());
        grid.addCellDoubleClickHandler(this);
        grid.setHeight100();
        return grid;
    }

    protected ToolStrip createToolbar() {
        ToolStrip tbar = new ToolStrip();
        tbar.setStyleName("docToolbar");
        tbar.setHeight(20);
        tbar.setAlign(Alignment.RIGHT);
        Button addButton = new Button();
        addButton.setStyleName("docButton");
        addButton.setIcon(Framework.getSkinPath("/images/add.gif"));
        addButton.setTitle("Create");
//        addButton.setShowTitle(true);
//        addButton.setSrc(UI.getSkinPath("/images/add.gif"));
//        addButton.setSize(16);
        addButton.setShowRollOver(false);
        addButton.setShowDown(false);
        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Window.alert("Add new doc - TODO");
            }
        });

        Button removeButton = new Button();
//        removeButton.setShowTitle(true);
//        removeButton.setSrc(UI.getSkinPath("/images/delete.gif"));
//        removeButton.setSize(16);
        removeButton.setStyleName("docButton");
        removeButton.setShowRollOver(false);
        removeButton.setShowDown(false);
        removeButton.setTitle("Remove");
        removeButton.setIcon(Framework.getSkinPath("/images/delete.gif"));
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                getGrid().removeSelectedData();
            }
        });

        tbar.setMembers(addButton, removeButton);
        return tbar;
    }

    public ListGrid getGrid() {
        getWidget();
        return grid;
    }

    @Override
    public String getTitle() {
        return "View";
    }


    @Override
    protected void inputChanged() {
        getGrid().fetchData(new Criteria("parentId", getDocument().getId()));/*, new DSCallback() {
        public void execute(DSResponse response, Object rawData,
                DSRequest request) {
            System.out.println("execute cbk: "+response.getStatus());
        }
    });*/
    }

    public void onCellDoubleClick(CellDoubleClickEvent event) {
        ListGridRecord record = event.getRecord();
        String id = record.getAttribute("id");
        new GetDocument(id) {
            @Override
            protected void openDocument(Document doc) {
                UI.openDocumentInActiveEditor(doc);
            }
        }.execute();
    }


}
