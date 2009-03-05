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
