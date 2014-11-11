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

package org.nuxeo.theme.webengine.negotiation.engine;

import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.ViewDef;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public final class ViewId implements Scheme {

    public String getOutcome(final Object context) {
        WebContext webContext = (WebContext) context;
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        final ApplicationType application = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, webContext.getModulePath(),
                webContext.getModule().getName());
        if (application == null) {
            return null;
        }

        Resource targetObject = webContext.getTargetObject();
        if (targetObject == null) {
            return null;
        }
        final String rootPath = webContext.getRoot().getPath();
        final String viewId = targetObject.getPath().substring(
                rootPath.length());

        final ViewDef view = application.getViewById(viewId);
        if (view == null) {
            return null;
        }
        final String engineName = view.getEngine();
        if (engineName == null) {
            return null;
        }
        if (Manager.getTypeRegistry().lookup(TypeFamily.ENGINE, engineName) != null) {
            return engineName;
        }
        return null;
    }
}
