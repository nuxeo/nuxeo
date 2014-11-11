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

package org.nuxeo.ecm.core.listener;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.operation.DefaultOperationEvent;
import org.nuxeo.ecm.core.api.operation.Modification;
import org.nuxeo.ecm.core.api.operation.ModificationSet;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.OperationEvent;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.lifecycle.LifeCycleEventTypes;

/**
 * @author Max Stepanov
 *
 */
@SuppressWarnings("deprecation")
public class OperationEventFactory {

    private static final Set<String> acceptedEvents = new HashSet<String>();

    static {
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_UPDATED);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_SECURITY_UPDATED);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_LOCKED);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_UNLOCKED);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_CREATED);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_REMOVED);
        acceptedEvents.add(DocumentEventTypes.DOCUMENT_MOVED);
        acceptedEvents.add(LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT);
    }

    private OperationEventFactory() {
    }

    public static OperationEvent createEvent(Operation<?> cmd) {
        return new DefaultOperationEvent(cmd.getSession(), cmd.getName(),
                cmd.getModifications(), null);
    }

    public static OperationEvent createEvent(CoreEvent coreEvent) {
        Map<String, ?> info = coreEvent.getInfo();
        String sessionId = null;
        DocumentModel doc = (DocumentModel) coreEvent.getSource();
        if (doc != null) {
            sessionId = doc.getSessionId();
        }
        if (info != null) {
            sessionId = (String) info.get(CoreEventConstants.SESSION_ID);
        }
        CoreSession session = null;
        if (sessionId != null) {
            session = CoreInstance.getInstance().getSession(sessionId);
        }
        if (session != null) {
            return createEvent(session, coreEvent);
        } else {
            System.out.println("WARNING: may be a compatibility bug: "
                    + "session id could not be found. Ignoring ...");
            String repositoryName = null;
            if (doc != null) {
                repositoryName = doc.getRepositoryName();
            }
            return createEvent(sessionId, repositoryName,
                    SecurityConstants.SYSTEM_USERNAME, coreEvent);
        }
    }

    public static OperationEvent createEvent(CoreSession session,
            CoreEvent coreEvent) {
        return createEvent(session.getSessionId(), session.getRepositoryName(),
                session.getPrincipal().getName(), coreEvent);
    }

    public static OperationEvent createEvent(String sessionId,
            String repository, String principal, CoreEvent coreEvent) {
        String id = coreEvent.getEventId();
        if (!acceptedEvents.contains(id)) {
            return null;
        }
        Object source = coreEvent.getSource();
        if (!(source instanceof DocumentModel)) {
            return null;
        }
        ModificationSet modifs = new ModificationSet();
        DocumentModel docModel = (DocumentModel) source;
        DocumentRef docRef = docModel.getRef();
        Serializable details = null;

        if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)
                || DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(id)) {
            modifs.add(docRef, Modification.CREATE);
            // TODO getParentRef() is a PATH reference -> should put a ID ref!
            modifs.add(docModel.getParentRef(), Modification.ADD_CHILD);
        } else if (DocumentEventTypes.DOCUMENT_REMOVED.equals(id)) {
            modifs.add(docRef, Modification.REMOVE);
            // TODO getParentRef() is a PATH reference -> should put a ID ref!
            modifs.add(docModel.getParentRef(), Modification.REMOVE_CHILD);
        } else if (LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT.equals(id)) {
            details = (Serializable) coreEvent.getInfo().get(
                    LifeCycleEventTypes.OPTION_NAME_TRANSITION);
            modifs.add(docRef, Modification.STATE);
        } else if (DocumentEventTypes.DOCUMENT_LOCKED.equals(id)) {
            modifs.add(docRef, Modification.STATE);
            details = Boolean.TRUE;
        } else if (DocumentEventTypes.DOCUMENT_UNLOCKED.equals(id)) {
            modifs.add(docRef, Modification.STATE);
            details = Boolean.FALSE;
        } else if (DocumentEventTypes.DOCUMENT_MOVED.equals(id)) {
            // TODO srcParent is a PATH reference -> should put a ID ref!
            DocumentRef srcParent = (DocumentRef) coreEvent.getInfo().get(
                    CoreEventConstants.PARENT_PATH);
            modifs.add(docRef, Modification.MOVE);
            modifs.add(srcParent, Modification.REMOVE_CHILD);
            modifs.add(docModel.getParentRef(), Modification.ADD_CHILD);
        } else if (DocumentEventTypes.DOCUMENT_UPDATED.equals(id)) {
            modifs.add(docRef, Modification.CONTENT);
        }

        return new DefaultOperationEvent(sessionId, repository, principal, id,
                modifs, details);
    }

}
