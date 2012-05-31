/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.net.URL;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Imports route models in the root folder defined by the current persister from
 * a contributed zip resource. Uses the IO core service, through @{link
 * FileManager}
 * 
 * @since 5.6
 * 
 */
public class RouteModelsInitializator extends RepositoryInitializationHandler {

    DocumentRoutingService service;

    @Override
    public void doInitializeRepository(CoreSession session)
            throws ClientException {
        // This method gets called as a system user
        // so we have all needed rights to do the check and the creation
        List<URL> routeModelTemplateResouces = getDocumentRoutingService().getRouteModelTemplateResouces();
        for (URL url : routeModelTemplateResouces) {
            getDocumentRoutingService().importRouteModel(url, true, session);
        }
        session.save();
    }

    private DocumentRoutingService getDocumentRoutingService() {
        try {
            if (service == null) {
                service = Framework.getService(DocumentRoutingService.class);
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
        return service;
    }

}
