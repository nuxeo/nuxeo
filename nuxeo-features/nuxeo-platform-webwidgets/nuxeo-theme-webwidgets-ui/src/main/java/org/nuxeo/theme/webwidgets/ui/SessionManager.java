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

import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.webengine.WebEngine;

public class SessionManager {

    private static final String WIDGET_CATEGORY = "org.nuxeo.theme.widget_category";

    private static HttpSession getHttpSession() {
        return WebEngine.getActiveContext().getRequest().getSession();
    }

    public static synchronized void setWidgetCategory(String id) {
        getHttpSession().setAttribute(WIDGET_CATEGORY, id);
    }

    public static synchronized String getWidgetCategory() {
        return (String) getHttpSession().getAttribute(WIDGET_CATEGORY);
    }

}
