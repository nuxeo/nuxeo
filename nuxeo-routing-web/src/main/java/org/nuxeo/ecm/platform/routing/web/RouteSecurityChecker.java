/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (!documentManager.hasChildren(currentDoc.getRef())) {
            // Cannot validate an empty route
            return false;
        }
        return getDocumentRoutingService().canUserValidateRoute(currentUser);
    }

    public boolean canValidateRoute(DocumentModel routeDocument) {
        return canValidateRoute() || getDocumentRoutingService().canValidateRoute(routeDocument, documentManager);
    }

    public DocumentRoutingService getDocumentRoutingService() {
        return Framework.getService(DocumentRoutingService.class);
    }

}
