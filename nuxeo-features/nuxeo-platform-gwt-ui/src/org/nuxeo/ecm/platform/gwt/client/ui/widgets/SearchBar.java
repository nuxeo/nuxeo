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

package org.nuxeo.ecm.platform.gwt.client.ui.widgets;

import org.nuxeo.ecm.platform.gwt.client.model.DocumentQuery;
import org.nuxeo.ecm.platform.gwt.client.ui.UI;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.FocusEvent;
import com.smartgwt.client.widgets.form.fields.events.FocusHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SearchBar extends HLayout {

    /**
     *
     */
    public SearchBar() {
        refresh();
    }

    public void refresh() {
        DynamicForm form = new DynamicForm();
//        form.setAutoFocus(true);
        form.setNumCols(1);
        form.setWidth100();
        form.setStyleName("searchBar");
        final TextItem searchBox = new TextItem("query");
        searchBox.setValue("Search ...");
        searchBox.setShowTitle(false);
        searchBox .setSelectOnFocus(true);
        searchBox.setAlign(Alignment.RIGHT);
        form.setFields(searchBox);


        addMember(form);

        searchBox.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                if ("Search ...".equals(searchBox.getValue())) {
                    searchBox.setValue("");
                }
            }
        });
        searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if ("Enter".equals(event.getKeyName())) {
                    String query = (String)searchBox.getValue();
                    if (query == null) {
                        query = "";
                    }
                    UI.openInEditor(new DocumentQuery(query));
                }
            }
        });
    }



}
