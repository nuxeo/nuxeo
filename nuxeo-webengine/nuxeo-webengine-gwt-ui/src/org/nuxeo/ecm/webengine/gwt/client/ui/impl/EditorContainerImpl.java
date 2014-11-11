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

package org.nuxeo.ecm.webengine.gwt.client.ui.impl;

import org.nuxeo.ecm.webengine.gwt.client.Extensible;
import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.ui.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.ui.Editor;
import org.nuxeo.ecm.webengine.gwt.client.ui.EditorContainer;
import org.nuxeo.ecm.webengine.gwt.client.ui.ExtensionPoints;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DeckPanel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EditorContainerImpl extends EditorContainer implements Extensible, ContextListener {


    public EditorContainerImpl() {
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        UI.addContextListener(this);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        UI.removeContextListener(this);
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.EDITORS_XP.equals(target)) {
            DeckPanel panel = getDeckPanel();
            Editor editor = (Editor)extension;
            panel.add(editor);
        } else if (ExtensionPoints.EDITORS_XP.equals(target)) {
            GWT.log("Unknown extension point: "+target, null);
        }
    }

    public EditorContainerImpl register() {
        Framework.registerExtension(ExtensionPoints.EDITOR_CONTAINER_XP, this);
        Framework.registerExtensionPoint(ExtensionPoints.EDITORS_XP, this);
        return this;
    }

    public void onContextEvent(int event) {
        refresh();
    }

}
