/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.events;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.lifecycle.LifeCycleEventTypes;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;
import org.nuxeo.ecm.platform.events.api.RepositoryChangeEvent;
import org.nuxeo.ecm.platform.events.api.impl.RepositoryChangeEventImpl;

/**
 * @author Max Stepanov
 *
 */
public class EventFactory {

    private EventFactory() {
    }

    public static NXCoreEvent createEvent(CoreEvent coreEvent) {
        Object source = coreEvent.getSource();
        if (!(source instanceof DocumentModel)) {
            return null;
        }
        DocumentModel docModel = (DocumentModel) source;
        String eventId = coreEvent.getEventId();

        int type = 0;
        DocumentRef targetRef = docModel.getRef();
        Map info = coreEvent.getInfo();

        // TODO: should be Serializable
        Object details = null;

        if (DocumentEventTypes.DOCUMENT_UPDATED.equals(eventId)) {
            type = RepositoryChangeEvent.UPDATED;
        } else if (DocumentEventTypes.DOCUMENT_SECURITY_UPDATED.equals(eventId)) {
            type = RepositoryChangeEvent.PERMISSIONS;
        } else if (DocumentEventTypes.DOCUMENT_LOCKED.equals(eventId)
                || DocumentEventTypes.DOCUMENT_UNLOCKED.equals(eventId)) {
            type = RepositoryChangeEvent.LOCK;
            details = DocumentEventTypes.DOCUMENT_LOCKED.equals(eventId);
        } else if (DocumentEventTypes.DOCUMENT_CREATED.equals(eventId)
                || DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.endsWith(eventId)) {
            type = RepositoryChangeEvent.ADDED;
            details = docModel.getParentRef();
        } else if (DocumentEventTypes.DOCUMENT_REMOVED.equals(eventId)) {
            type = RepositoryChangeEvent.REMOVED;
            details = docModel.getParentRef();
        } else if (DocumentEventTypes.DOCUMENT_MOVED.equals(eventId)) {
            type = RepositoryChangeEvent.MOVED;
            details = (DocumentRef) info.get(CoreEventConstants.PARENT_PATH);
        } else if (LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT.equals(eventId)) {
            type = RepositoryChangeEvent.LIFECYCLE;
            details = coreEvent.getInfo().get(LifeCycleEventTypes.OPTION_NAME_TRANSITION);
        }
        /* TODO: DOCUMENT_PUBLISHED */
        if (type == 0) {
            return null;
        }

        String sessionId = (String) info.get(CoreEventConstants.SESSION_ID);
        String repositoryName = (String) info.get(CoreEventConstants.REPOSITORY_NAME);

        return new RepositoryChangeEventImpl(sessionId, repositoryName, type,
                targetRef, details);
    }

}
