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
