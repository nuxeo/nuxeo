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
import org.nuxeo.ecm.platform.gwt.client.ui.AbstractView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultEditor implements Editor {
    
    
    public boolean acceptInput(Object input) {
        return true;
    }
    
    public View getView() {
        EditorView view = new EditorView();
        return view;
    }
    
    
    static class EditorView extends AbstractView {
        private static int cnt = 0;

        public EditorView() {
            super("default#"+(cnt++));
        }

        @Override
        protected HTMLFlow createWidget() {
            return  new HTMLFlow();
        }
        
        public Canvas getCanvas() {
            return (Canvas)getWidget();
        }
        
        @Override
        protected void inputChanged() {
            refresh();
        }
        
        @Override
        public void refresh() {
            getCanvas().setContents("<h3>Unknown input type - There is no editor installed for this input.</h3>"+input);
        }
        
        public String getTitle() {
            return "Error";
        }
        
        public String getIcon() {
            return Framework.getSkinPath("images/document.png");
        }

    }
}
