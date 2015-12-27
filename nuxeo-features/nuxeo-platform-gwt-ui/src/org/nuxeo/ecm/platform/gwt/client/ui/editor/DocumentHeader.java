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
import org.nuxeo.ecm.platform.gwt.client.ui.UI;

import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.types.ImageStyle;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HStack;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentHeader extends HStack implements ClickHandler {

    protected MultiPageDocView parent;
    protected String parentId;
    protected HTMLFlow title;
    protected Img up;
    protected Document input;

    public DocumentHeader(MultiPageDocView parent) {
        setWidth("80%");
        setHeight(50);
        setStyleName("docHeader");
        this.title = new HTMLFlow();
        this.parent = parent;
        title.setWidth100();
        this.up = new Img();
        up.addClickHandler(this);
        up.setSrc(Framework.getSkinPath("/images/up.png"));
        up.setSize(30);
        //up.setSize(16);
        up.setCursor(Cursor.HAND);
        up.setImageType(ImageStyle.CENTER);
        up.setValign(VerticalAlignment.BOTTOM);
        up.setTitle("Up ...");

        addMember(up);
        addMember(title);
        title.setStyleName("docHeader");
//        up.setBorder("1px solid black");
//        title.setBorder("1px solid black");
    }


    public void update(Document doc) {
        input = doc;
        parentId  = doc.getParentId();
        title.setContents(doc.getTitle());
    }


    public void onClick(ClickEvent event) {
        if (parentId == null) {
            return;
        }
        new GetDocument(parentId) {
            @Override
            protected void openDocument(Document doc) {
                UI.openDocumentInActiveEditor(doc);
            }
        }.execute();
    }

}
