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
