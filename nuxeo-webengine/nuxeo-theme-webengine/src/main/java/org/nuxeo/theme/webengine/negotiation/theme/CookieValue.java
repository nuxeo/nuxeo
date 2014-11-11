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

package org.nuxeo.theme.webengine.negotiation.theme;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.negotiation.Scheme;

public class CookieValue implements Scheme {

    private static final Log log = LogFactory.getLog(CookieValue.class);

    public String getOutcome(final Object context) {
        final WebContext webContext = (WebContext) context;
        String path = null;
        // FIXME AbstractContext.getCookie triggers a NullPointerException
        // (WEB-157)
        try {
            path = webContext.getCookie("nxthemes.theme");
        } catch (Exception e) {
            log.error(e, e);
        }
        if (path == null) {
            return null;
        }
        final PageElement page = Manager.getThemeManager().getPageByPath(path);
        if (page != null) {
            return path;
        }
        return null;
    }

}
