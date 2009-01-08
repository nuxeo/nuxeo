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

import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.view.ViewSite;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EditorSite extends ViewSite {
    protected Editor editor;
    protected static int cnt = 0;
    
    public EditorSite(Editor editor) {
        this ("editor", editor);
    }
    
    public EditorSite(String name, Editor editor) {
        super(name+"#"+(cnt++), editor.getView()); //TODO
        this.editor = editor;
    }
    
    public void open(Container container, Object input) {
        this.container = container;
        if (handle == null) {
            handle = container.createHandle(this);
        }
        if (!view.isInstalled()) {
            view.install(this, input);
            container.installWidget(this);
            container.updateSiteTitle(this); 
            container.updateSiteIcon(this);
        } else {
            view.setInput(input);
        }
    }
   
    public Editor getEditor() {
        return editor;
    }
}
