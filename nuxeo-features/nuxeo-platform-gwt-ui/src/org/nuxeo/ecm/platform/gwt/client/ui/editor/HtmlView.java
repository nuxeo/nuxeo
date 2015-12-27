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

import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.smartgwt.client.widgets.HTMLFlow;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class HtmlView implements Editor {


    public boolean acceptInput(Object input) {
        return input instanceof String;
    }

    public View getView() {
        View view = new EditorView();
        return view;
    }

    static class EditorView extends SmartView {
        private static int cnt = 0;

        public EditorView() {
            super("html#"+(cnt++));
        }

        @Override
        protected HTMLFlow createWidget() {
            return new HTMLFlow();
        }

        @Override
        protected void inputChanged() {
            refresh();
        }

        @Override
        public void refresh() {
            getWidget().setContents(input.toString());
        }

        @Override
        public Object getInput() {
            return getWidget().getContents();
        }

    }
}
