/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.listeners;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.service.PublisherServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle Domain creation, deletion and lifecycle changes. Register new {@code
 * PublicationTreeConfigDescriptor} according to the new Domain, if at least one
 * descriptor is pending. Unregister {@code PublicationTreeConfigDescriptor}
 * associated to the Domain when it is removed or if its lifecycle has changed
 * (ie. to delete state).
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DomainEventsListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc != null && "Domain".equals(doc.getType())) {
                String eventName = event.getName();
                if (DocumentEventTypes.DOCUMENT_CREATED.equals(eventName)) {
                    registerNewPublicationTrees(doc);
                } else if (DocumentEventTypes.DOCUMENT_UPDATED.equals(eventName)) {
                    // re-register in case of title update for instance
                    unregisterPublicationTrees(doc);
                    registerNewPublicationTrees(doc);
                } else if (DocumentEventTypes.DOCUMENT_REMOVED.equals(eventName)) {
                    unregisterPublicationTrees(doc);
                } else if (LifeCycleConstants.TRANSITION_EVENT.equals(eventName)) {
                    handleDomainLifeCycleChanged(docCtx, doc);
                }
            }
        }
    }

    protected void registerNewPublicationTrees(DocumentModel doc)
            throws ClientException {
        try {
            PublisherServiceImpl service = (PublisherServiceImpl) Framework.getService(PublisherService.class);
            service.registerTreeConfigFor(doc);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected void unregisterPublicationTrees(DocumentModel doc)
            throws ClientException {
        try {
            PublisherServiceImpl service = (PublisherServiceImpl) Framework.getService(PublisherService.class);
            service.unRegisterTreeConfigFor(doc);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected void handleDomainLifeCycleChanged(DocumentEventContext docCtx,
            DocumentModel doc) throws ClientException {
        String from = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_FROM);
        String to = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TO);

        if (LifeCycleConstants.DELETED_STATE.equals(to)) {
            handleDomainGoesToDeletedState(doc);
        } else if (LifeCycleConstants.DELETED_STATE.equals(from)) {
            handleDomainGoesFromDeletedState(doc);
        }
    }

    protected void handleDomainGoesToDeletedState(DocumentModel doc)
            throws ClientException {
        unregisterPublicationTrees(doc);
    }

    protected void handleDomainGoesFromDeletedState(DocumentModel doc)
            throws ClientException {
        registerNewPublicationTrees(doc);
    }

}
