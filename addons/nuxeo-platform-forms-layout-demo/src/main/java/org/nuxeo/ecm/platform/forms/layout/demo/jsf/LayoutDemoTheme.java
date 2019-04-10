/*
 * (C) Copyright 2006-2010 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.theme.negotiation.Scheme;

/**
 * Overrides default theme negotiator to return the "galaxy/popup" mode
 * whenever browsing layoutDemo pages.
 *
 * @author Anahide Tchertchian
 */
public final class LayoutDemoTheme implements Scheme {

    public String getOutcome(final Object context) {
        ExternalContext eContext = ((FacesContext) context).getExternalContext();
        String servletPath = eContext.getRequestServletPath();
        if (servletPath != null
                && servletPath.startsWith("/"
                        + LayoutDemoManager.APPLICATION_PATH)) {
            return "galaxy/popup";
        }
        final Map<String, Object> requestMap = eContext.getRequestMap();
        return (String) requestMap.get("org.nuxeo.theme.default.theme");
    }

}
