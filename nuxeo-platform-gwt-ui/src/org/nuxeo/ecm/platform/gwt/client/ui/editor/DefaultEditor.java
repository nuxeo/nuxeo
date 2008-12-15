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
import org.nuxeo.ecm.platform.gwt.client.ui.Editor;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.smartgwt.client.widgets.HTMLFlow;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultEditor implements Editor {
    
    
    public boolean acceptInput(Object input) {
        return true;
    }
    
    public View<?> open(Object input) {
        EditorView view = new EditorView();
        view.setInput(input);
        view.setIcon(Framework.getSkinPath("images/document.png"));
        return view;
    }
    
    public boolean canReuseViews() {
        return true;
    }
    
    static class EditorView extends SmartView<HTMLFlow> {
        private static int cnt = 0;
        
        protected Object input;

        public EditorView() {
            super("default#"+(cnt++));
            setTitle("Error");
        }

        @Override
        protected HTMLFlow createWidget() {
            return  new HTMLFlow();
        }

        @Override
        public void setInput(Object input) {
            this.input = input;
            getWidget().setContents("<h3>Unknown input type - There is no editor installed for this input.</h3>"+input);
        }

        @Override
        public Object getInput() {
            return input;
        }

    }
}
