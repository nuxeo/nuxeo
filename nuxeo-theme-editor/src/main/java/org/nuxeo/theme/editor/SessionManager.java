/*
 * (C) Copyright 2006-2008 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.editor;

import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.session.AbstractComponent;

public class SessionManager extends AbstractComponent {

    private static final long serialVersionUID = 1L;

    private static String SELECTED_ELEMENT_ID = "org.nuxeo.theme.editor.selected_element";

    public static synchronized void setSelectedElementId(WebContext ctx,
            String id) {
        ctx.getUserSession().put(SELECTED_ELEMENT_ID, id);
    }

    public static synchronized String getSelectedElementId(WebContext ctx) {
        return (String) ctx.getUserSession().get(SELECTED_ELEMENT_ID);
    }
}
