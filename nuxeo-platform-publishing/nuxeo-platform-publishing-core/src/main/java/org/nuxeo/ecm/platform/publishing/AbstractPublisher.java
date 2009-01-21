/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;

/**
 * @author arussel
 *
 */
public abstract class AbstractPublisher {
    protected CoreSession getCoreSession(DocumentModel document,
            NuxeoPrincipal principal) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("principal", principal);
        return CoreInstance.getInstance().open(document.getRepositoryName(),
                context);
    }

    protected DocumentModel publish(DocumentModel document, DocumentModel sectionToPublishTo,
            NuxeoPrincipal principal) throws ClientException {
        CoreSession coreSession = getCoreSession(document, principal);
        document.setProperty("dublincore", "issued", Calendar.getInstance());
        return coreSession.publishDocument(document, sectionToPublishTo);
    }

    public void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm, CoreSession coreSession)
            throws ClientException {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        properties.put(CoreEventConstants.REPOSITORY_NAME,
                dm.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                coreSession.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());
        CoreEvent event = new CoreEventImpl(eventId, dm, properties,
                coreSession.getPrincipal(), category, comment);
        EventMessage message = new DocumentMessageImpl(dm, event);
        try {
            DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer().produce(
                    message);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
