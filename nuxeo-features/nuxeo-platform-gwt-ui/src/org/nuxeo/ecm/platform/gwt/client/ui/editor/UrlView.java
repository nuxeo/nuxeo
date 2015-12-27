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

import org.nuxeo.ecm.platform.gwt.client.model.Url;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLPane;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UrlView implements Editor {

    public boolean acceptInput(Object input) {
        return input instanceof Url;
    }

    public View getView() {
        EditorView view = new EditorView();
        return view;
    }

    static class EditorView extends SmartView {
        private static int cnt = 0;

            public EditorView() {
            super("iframe#"+(cnt++));
        }

        @Override
        protected Canvas createWidget() {
            HTMLPane html = new HTMLPane();
            html.setContentsType(ContentsType.PAGE);
//            html.setRedrawOnResize(true);
            html.setWidth100();
            html.setHeight100();
            return html;
        }

        @Override
        protected void inputChanged() {
            refresh();
        }

        @Override
        public void refresh() { // using HTMLPane is not working -  on resize is not working
            if (input == null) {
                return;
            }
            ((HTMLPane)getWidget()).setContentsURL(input.toString());
        }

        @Override
        public String getTitle() {
            return "URL View";
        }

        @Override
        public Object getInput() {
            return ((HTMLPane)getWidget()).getContentsURL();
        }

    }


}
