/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Event listener to reset the synchronization root registrations on a copied document and its children.
 *
 * @since 9.1
 */
public class NuxeoDriveSyncRootCopyListener implements EventListener {

    public static final String RESET_SYNC_ROOTS_ON_COPY_CONFIGURATION_PROPERTY = "org.nuxeo.drive.resetSyncRootsOnCopy";

    @Override
    public void handleEvent(Event event) {
        if (Framework.getService(ConfigurationService.class)
                     .isBooleanPropertyFalse(RESET_SYNC_ROOTS_ON_COPY_CONFIGURATION_PROPERTY)) {
            return;
        }
        EventContext context = event.getContext();
        if (!(context instanceof DocumentEventContext)) {
            return;
        }
        DocumentModel doc = ((DocumentEventContext) context).getSourceDocument();
        CoreSession session = context.getCoreSession();
        DocumentModelList syncRoots = getSyncRoots(doc, session);
        for (DocumentModel syncRoot : syncRoots) {
            syncRoot.setPropertyValue(NuxeoDriveManagerImpl.DRIVE_SUBSCRIPTIONS_PROPERTY, null);
            syncRoot.putContextData("source", "drive");
            syncRoot.putContextData(CoreSession.SOURCE, "drive");
            session.saveDocument(syncRoot);
        }
    }

    protected DocumentModelList getSyncRoots(DocumentModel doc, CoreSession session) {
        String nxql = "SELECT * FROM Document WHERE ecm:mixinType = '" + NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET
                + "' AND ecm:path STARTSWITH " + NXQL.escapeString(doc.getPathAsString());
        DocumentModelList syncRoots = session.query(nxql);
        if (doc.hasFacet(NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET)) {
            syncRoots.add(doc);
        }
        return syncRoots;
    }

}
