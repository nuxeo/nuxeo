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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Notify the NuxeoDriveManager service in case of document deletions so as to
 * make it possible to invalidate any cache.
 */
public class NuxeoDriveCacheInvalidationListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            // not interested in event that are not related to documents
            return;
        }
        String transition = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
        if (transition != null
                && !(LifeCycleConstants.DELETE_TRANSITION.equals(transition) || LifeCycleConstants.UNDELETE_TRANSITION.equals(transition))) {
            // not interested in lifecycle transitions that are not related to
            // document deletion
            return;
        }
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        if (CollectionConstants.ADDED_TO_COLLECTION.equals(event.getName())
                || CollectionConstants.REMOVED_FROM_COLLECTION.equals(event.getName())) {
            driveManager.invalidateCollectionSyncRootMemberCache();
        } else {
            driveManager.handleFolderDeletion((IdRef) docCtx.getSourceDocument().getRef());
        }
    }

}
