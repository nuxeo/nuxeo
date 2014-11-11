/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.webwidgets.ui;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.session.AbstractComponent;

public class SessionManager extends AbstractComponent {

    private static final long serialVersionUID = 1L;

    private static final String WIDGET_CATEGORY = "org.nuxeo.theme.widget_category";

    public static synchronized void setWidgetCategory(String id) {
        WebContext ctx = WebEngine.getActiveContext();
        ctx.getUserSession().put(WIDGET_CATEGORY, id);
    }

    public static synchronized String getWidgetCategory() {
        WebContext ctx = WebEngine.getActiveContext();
        return (String) ctx.getUserSession().get(WIDGET_CATEGORY);
    }

}
