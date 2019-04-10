/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Event listener that reloads the contributed route models.
 *
 * @since 5.6
 */
public class RouteModelsReloader implements EventListener {

    private static final Log log = LogFactory.getLog(RouteModelsReloader.class);

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (!Framework.isDevModeSet()) {
            log.info("Do not flush the directory caches: dev mode is not set");
            return;
        }
        if (!ReloadEventNames.RELOAD_EVENT_ID.equals(event.getId())) {
            return;
        }
        try {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            if (rm == null) {
                throw new ClientException("Can not acces the RepositoryManager");
            }
            new UnrestrictedSessionRunner(rm.getDefaultRepository().getName()) {
                @Override
                public void run() throws ClientException {
                    DocumentRoutingService service = Framework.getLocalService(DocumentRoutingService.class);
                    List<URL> routeModelTemplateResouces = service.getRouteModelTemplateResources();
                    for (URL url : routeModelTemplateResouces) {
                        service.importRouteModel(url, true, session);
                    }
                }
            }.runUnrestricted();
        } catch (Exception e) {
            log.error("Error while reloading the route models", e);
        }
    }
}
