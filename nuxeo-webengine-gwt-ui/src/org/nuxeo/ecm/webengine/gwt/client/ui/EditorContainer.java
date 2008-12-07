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

package org.nuxeo.ecm.webengine.gwt.client.ui;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EditorContainer extends ViewDeck {

    public EditorContainer() {
        super("editorContainer");
    }

    public void open(Object input) {
        DeckPanel panel = getDeckPanel();
        int cnt = panel.getWidgetCount();
        for (int i=0; i<cnt; i++) {
            Editor view = (Editor)panel.getWidget(i);
            if (view.acceptInput(input)) {
                view.open(input);
                panel.showWidget(i);
                return;
            }
        }
        // no suitable view was found. create a new view to catch all contexts
        DefaultEditor v = new DefaultEditor();
        panel.add(v);
        v.open(input);
        panel.showWidget(panel.getWidgetCount()-1);
    }

    @Override
    public void add(View item) {
        // we need to make sure the last item is always the default one
        DeckPanel panel = getDeckPanel();
        if (panel.getWidget(panel.getWidgetCount()-1) instanceof DefaultEditor) {
            panel.insert(item, panel.getWidgetCount()-1);
        } else {
            panel.add(item);
        }
    }

    @Override
    public void insert(View item, int beforeIndex) {
        DeckPanel panel = getDeckPanel();
        if (beforeIndex >= panel.getWidgetCount()) {
            if (panel.getWidget(panel.getWidgetCount()-1) instanceof DefaultEditor) {
                beforeIndex = panel.getWidgetCount()-1;
            }
        }
        panel.insert(item, beforeIndex);
    }

    static class DefaultEditor extends Editor {
        public DefaultEditor() {
            super("_default_", new HTML());
        }
        @Override
        public boolean acceptInput(Object input) {
            return true;
        }
        public void setInput(Object input) {
            if (input instanceof Widget) {
                ((HTML)getWidget()).setHTML(input.toString());
            } else {
                ((HTML)getWidget()).setText("No editor was registered for the object: "+input.toString());
            }
        }
    }
}
