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

import java.util.Map;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.perspectives.PerspectiveManager;

public final class RequestAttribute implements Scheme {

    public String getOutcome(final Object context) {
        final Map<String, Object> parameters = ((FacesContext) context).getExternalContext().getRequestMap();
        final String perspectiveName = (String) parameters.get("org.nuxeo.theme.perspective");
        if (PerspectiveManager.hasPerspective(perspectiveName)) {
            return perspectiveName;
        }
        return null;
    }
}
