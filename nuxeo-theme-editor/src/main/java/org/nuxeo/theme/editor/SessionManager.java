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

    private static String STYLE_EDIT_MODE = "org.nuxeo.theme.editor.style_edit_mode";

    private static String STYLE_LAYER_ID = "org.nuxeo.theme.editor.style_layer";

    private static String STYLE_SELECTOR = "org.nuxeo.theme.editor.style_selector";

    public static synchronized void setSelectedElementId(WebContext ctx,
            String id) {
        ctx.getUserSession().put(SELECTED_ELEMENT_ID, id);
    }

    public static synchronized String getSelectedElementId(WebContext ctx) {
        return (String) ctx.getUserSession().get(SELECTED_ELEMENT_ID);
    }

    public static synchronized String getStyleEditMode(WebContext ctx) {
        return (String) ctx.getUserSession().get(STYLE_EDIT_MODE);
    }

    public static synchronized void setStyleEditMode(WebContext ctx, String mode) {
        ctx.getUserSession().put(STYLE_EDIT_MODE, mode);
    }

    public static synchronized String getSelectedStyleLayerId(WebContext ctx) {
        return (String) ctx.getUserSession().get(STYLE_LAYER_ID);
    }

    public static synchronized void setSelectedStyleLayerId(WebContext ctx,
            String id) {
        ctx.getUserSession().put(STYLE_LAYER_ID, id);
    }

    public static synchronized String getSelectedStyleSelector(WebContext ctx) {
        return (String) ctx.getUserSession().get(STYLE_SELECTOR);
    }

    public static synchronized void setSelectedStyleSelector(WebContext ctx,
            String selector) {
        ctx.getUserSession().put(STYLE_SELECTOR, selector);
    }

}
