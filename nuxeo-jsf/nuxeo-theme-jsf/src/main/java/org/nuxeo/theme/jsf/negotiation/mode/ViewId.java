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

package org.nuxeo.theme.jsf.negotiation.mode;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.ViewDef;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public final class ViewId implements Scheme {

    public String getOutcome(final Object context) {
        final String viewId = ((FacesContext) context).getViewRoot().getViewId();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();

        final String applicationPath = ((FacesContext) context).getExternalContext().getRequestContextPath();

        final ApplicationType application = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, applicationPath);
        if (application == null) {
            return null;
        }

        final ViewDef view = application.getViewById(viewId);
        if (view == null) {
            return null;
        }

        String mode = view.getMode();
        if (mode == null) {
            return null;
        }
        return mode;
    }
}
