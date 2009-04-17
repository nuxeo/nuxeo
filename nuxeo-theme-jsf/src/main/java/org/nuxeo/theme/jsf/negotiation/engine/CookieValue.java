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

package org.nuxeo.theme.jsf.negotiation.engine;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.jsf.negotiation.CookieManager;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.types.TypeFamily;

public class CookieValue implements Scheme {

    public String getOutcome(final Object context) {
        final String engineName = CookieManager.getCookie("nxthemes.engine",
                ((FacesContext) context).getExternalContext());
        if (engineName == null) {
            return null;
        }
        if (Manager.getTypeRegistry().lookup(TypeFamily.ENGINE, engineName) != null) {
            return engineName;
        }
        return null;
    }
}
