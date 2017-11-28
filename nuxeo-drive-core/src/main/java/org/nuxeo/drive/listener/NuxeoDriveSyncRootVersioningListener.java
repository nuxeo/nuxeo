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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;

/**
 * Event listener to reset the synchronization root registrations on a versioned document.
 *
 * @since 9.10
 */
public class NuxeoDriveSyncRootVersioningListener implements EventListener {

    @Override
    @SuppressWarnings("resource")
    public void handleEvent(Event event) {
        EventContext context = event.getContext();
        DocumentRef checkedInVersionRef = (DocumentRef) context.getProperty("checkedInVersionRef");
        if (checkedInVersionRef == null) {
            return;
        }
        CoreSession session = context.getCoreSession();
        DocumentModel doc = session.getDocument(checkedInVersionRef);
        if (!(doc.isVersion() && doc.hasFacet(NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET))) {
            return;
        }
        doc.setPropertyValue(NuxeoDriveManagerImpl.DRIVE_SUBSCRIPTIONS_PROPERTY, null);
        doc.putContextData(CoreSession.ALLOW_VERSION_WRITE, Boolean.TRUE);
        doc.putContextData("source", "drive");
        doc.putContextData(CoreSession.SOURCE, "drive");
        session.saveDocument(doc);
    }

}
