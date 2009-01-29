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

package org.nuxeo.theme.jsf.negotiation.theme;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.negotiation.Scheme;

public final class RequestAttribute implements Scheme {

    public String getOutcome(final Object context) {
        final Map<String, Object> parameters = ((FacesContext) context).getExternalContext().getRequestMap();
        final String path = (String) parameters.get("org.nuxeo.theme.theme");
        final PageElement page = Manager.getThemeManager().getPageByPath(path);
        if (page != null) {
            return path;
        }
        return null;
    }

}
