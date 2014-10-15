/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Event listener to unlock a document removed from the "Locally Edited"
 * collection.
 */
public class NuxeoDriveUnlockListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext ctx;
        if (event.getContext() instanceof DocumentEventContext) {
            ctx = (DocumentEventContext) event.getContext();
        } else {
            // Not interested in events that are not related to documents
            return;
        }
        DocumentModel doc = ctx.getSourceDocument();
        IdRef collectionRef = (IdRef) ctx.getProperty(CollectionConstants.COLLECTION_REF_EVENT_CTX_PROP);
        if (collectionRef != null) {
            if (NuxeoDriveManager.LOCALLY_EDITED_COLLECTION_NAME.equals(ctx.getCoreSession().getDocument(
                    collectionRef).getName())) {
                if (doc.isLocked()) {
                    ctx.getCoreSession().removeLock(doc.getRef());
                }
            }
        }
    }

}
