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

import java.util.Map;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.types.TypeFamily;

public final class RequestParameter implements Scheme {

    public String getOutcome(final Object context) {
        final Map<String, String> parameters = ((FacesContext) context).getExternalContext().getRequestParameterMap();
        final String engineName = parameters.get("engine");
        if (Manager.getTypeRegistry().lookup(TypeFamily.ENGINE, engineName) != null) {
            return engineName;
        }
        return null;
    }

}
