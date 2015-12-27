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

package org.nuxeo.ecm.platform.gwt.client.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.model.DocumentQuery;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.Editor;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SearchEditor implements Editor, Extensible {

    protected List<View> pageViews = new ArrayList<View>();

    public boolean acceptInput(Object input) {
        return input instanceof DocumentQuery;
    }

    public View getView() {
        SearchView view = new SearchView();
        return view;
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.EDITOR_PAGES_XP.equals(target)) {
            pageViews.add((View)extension);
        }
    }

    public class SearchView extends SmartView {
        HTMLFlow todo;
        public SearchView() {
            super("search");
        }
        public DocumentQuery getQuery() {
            return (DocumentQuery)input;
        }
        @Override
        protected void inputChanged() {
            refresh();
        }
        @Override
        public String getTitle() {
            return "Search Result";
        }
        @Override
        public void refresh() {
            if (todo != null) {
                todo.setContents("<font color=red>Not Yet implemented.</font> Query: <i>"+getQuery()+"</i>");
            }
        }
        @Override
        protected Canvas createWidget() {
            VLayout panel = new VLayout();
            ToolStrip tbar = new ToolStrip();
            tbar.setStyleName("navbar");
            tbar.setAlign(Alignment.RIGHT);
            todo = new HTMLFlow();
            todo.setWidth100();
            todo.setContents("<font color=red>Not yet implemented.</font> Query: <i>"+getQuery()+"</i>");
            tbar.addMember(todo);
            tbar.setHeight(18);
            ListGrid grid = new ListGrid();
            panel.addMember(tbar);
            panel.addMember(grid);
            return panel;
        }
    }

}
