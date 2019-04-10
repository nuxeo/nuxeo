/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import static org.nuxeo.ecm.core.trash.TrashService.DOCUMENT_TRASHED;
import static org.nuxeo.ecm.core.trash.TrashService.DOCUMENT_UNTRASHED;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Notify the NuxeoDriveManager service in case of document deletions so as to make it possible to invalidate any cache.
 */
public class NuxeoDriveCacheInvalidationListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            // not interested in event that are not related to documents
            return;
        }
        String transition = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
        String eventName = event.getName();
        if (!DOCUMENT_TRASHED.equals(eventName) && !DOCUMENT_UNTRASHED.equals(eventName) && transition != null
                && !(LifeCycleConstants.DELETE_TRANSITION.equals(transition)
                        || LifeCycleConstants.UNDELETE_TRANSITION.equals(transition))) {
            // not interested in lifecycle transitions that are not related to
            // document deletion
            return;
        }
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        if (CollectionConstants.ADDED_TO_COLLECTION.equals(event.getName())
                || CollectionConstants.REMOVED_FROM_COLLECTION.equals(event.getName())) {
            driveManager.invalidateCollectionSyncRootMemberCache();
        } else {
            driveManager.handleFolderDeletion((IdRef) docCtx.getSourceDocument().getRef());
        }
    }

}
