/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.jsf.negotiation.perspective;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.jsf.negotiation.CookieManager;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.perspectives.PerspectiveManager;

public class CookieValue implements Scheme {

    public String getOutcome(final Object context) {
        final String perspectiveName = CookieManager.getCookie(
                "nxthemes.perspective",
                ((FacesContext) context).getExternalContext());
        if (perspectiveName == null) {
            return null;
        }
        if (PerspectiveManager.hasPerspective(perspectiveName)) {
            return perspectiveName;
        }
        return null;
    }
}
