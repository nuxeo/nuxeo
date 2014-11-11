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

package org.nuxeo.theme.webengine.negotiation.perspective;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.perspectives.PerspectiveManager;

public class CookieValue implements Scheme {

    private static final Log log = LogFactory.getLog(CookieValue.class);

    public String getOutcome(final Object context) {
        final WebContext webContext = (WebContext) context;
        String perspectiveName = null;
        // FIXME AbstractContext.getCookie triggers a NullPointerException
        // (WEB-157)
        try {
            perspectiveName = webContext.getCookie("nxthemes.perspective");
        } catch (Exception e) {
            log.error(e, e);
        }
        if (perspectiveName == null) {
            return null;
        }
        if (PerspectiveManager.hasPerspective(perspectiveName)) {
            return perspectiveName;
        }
        return null;
    }

}
