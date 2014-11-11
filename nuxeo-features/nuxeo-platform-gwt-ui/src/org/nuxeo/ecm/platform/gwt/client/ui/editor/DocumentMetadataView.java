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

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.model.Document;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.google.gwt.user.client.Window;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentMetadataView extends SmartView implements DSCallback {

    protected View parent;
    protected DynamicForm form;

    public DocumentMetadataView() {
        super ("metadata");
    }

    public void setParent(View view) {
        this.parent = view;
    }

    public Document getDocument() {
        return (Document)input;
    }

    @Override
    public void uninstall() {
        super.uninstall();
        form = null;
    }

    @Override
    public boolean acceptInput(Object input) {
        return true;
    }


    @Override
    protected void inputChanged() {
        refresh();
    }


    @Override
    public String getTitle() {
        return "Metadata";
    }

    @Override
    public void refresh() {
        Document doc = getDocument();
        if (form != null && doc != null) {
            form.setValue("ref", doc.getId());
            form.setValue("dc:title", doc.getTitle());
            form.setValue("dc:description", doc.getDescription());
        }
    }

    @Override
    protected Canvas createWidget() {
        VLayout layout = new VLayout();
        form = new DynamicForm();
        form.setBorder("1px solid black");
        form.setWidth("50%");
        form.setAction(Framework.getResourcePath("/doc"));
        form.setCanSubmit(true);
        form.setMethod(FormMethod.POST);
        form.setNumCols(2);
        form.setColWidths("20%", "80%");
        HiddenItem ref = new HiddenItem("ref");
        TextItem title = new TextItem("dc:title");
        title.setWrapTitle(false);
        title.setTitle("Title");
        TextAreaItem description = new TextAreaItem("dc:description");
        description.setTitle("Description");
        description.setColSpan(2);
        ButtonItem submit = new ButtonItem();
        submit.setTitle("Save");
        submit.setColSpan(2);
        submit.setAlign(Alignment.RIGHT);
        submit.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                //form.submit(DocumentMetadataView.this);
                form.saveData();
            }
        });
        form.setFields(ref, title, description, submit);
        layout.addMember(form);
        return layout;
    }

    public void execute(DSResponse response, Object rawData, DSRequest request) {
        if (response.getHttpResponseCode() == 200) {
            Window.alert("Save succeed - TODO Refresh editor title and navigator if needed");
        } else if (response.getHttpResponseCode() == 401) {
            Window.alert("You do not have permissions to modify this document.");
        } else {
            Window.alert("A server error ("+response.getHttpResponseCode()+") occurred");
        }
    }
}
