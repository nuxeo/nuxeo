/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.web;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
@Scope(ScopeType.CONVERSATION)
@Name("routeSecurityChecker")
@Install(precedence = Install.FRAMEWORK)
public class RouteSecurityChecker implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(required = true, create = false)
    protected NuxeoPrincipal currentUser;

    @In(required = true, create = false)
    protected CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @Deprecated
    /**
     * @deprecated use
     *             {@link #canValidateRoute(DocumentModel)}
     *             instead.
     */
    public boolean canValidateRoute() {
        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (!documentManager.hasChildren(currentDoc.getRef())) {
                // Cannot validate an empty route
                return false;
            }
        } catch (ClientException e) {
            new RuntimeException(e);
        }
        return getDocumentRoutingService().canUserValidateRoute(currentUser);
    }

    public boolean canValidateRoute(DocumentModel routeDocument)
            throws ClientException {
        return canValidateRoute()
                || getDocumentRoutingService().canValidateRoute(routeDocument,
                        documentManager);
    }

    public DocumentRoutingService getDocumentRoutingService() {
        try {
            return Framework.getService(DocumentRoutingService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
