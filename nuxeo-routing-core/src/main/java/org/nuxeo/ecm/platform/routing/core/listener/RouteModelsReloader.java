/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Event listener that reloads the contributed route models.
 *
 * @since 5.6
 */
public class RouteModelsReloader implements EventListener {

    private static final Log log = LogFactory.getLog(RouteModelsReloader.class);

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
            // Transaction management
            final boolean txStarted = !TransactionHelper.isTransactionActive() && TransactionHelper.startTransaction();
            boolean txSucceed = false;
            try {
                new UnrestrictedSessionRunner(rm.getDefaultRepositoryName()) {
                    @Override
                    public void run() {
                        DocumentRoutingService service = Framework.getLocalService(DocumentRoutingService.class);
                        service.importAllRouteModels(session);
                    }
                }.runUnrestricted();
                txSucceed = true;
            } finally {
                if (txStarted) {
                    if (!txSucceed) {
                        TransactionHelper.setTransactionRollbackOnly();
                        log.warn("Rollbacking import of route models");
                    }
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        } catch (NuxeoException e) {
            log.error("Error while reloading the route models", e);
        }
    }
}
