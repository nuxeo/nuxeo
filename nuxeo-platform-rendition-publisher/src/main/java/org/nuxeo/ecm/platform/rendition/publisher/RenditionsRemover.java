/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.publisher;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Remove proxy to the same stored rendition with a different version.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class RenditionsRemover extends UnrestrictedSessionRunner {

    public static final String RENDITION_PROXY_PUBLISHED = "renditionProxyPublished";

    protected final DocumentModel proxy;

    protected RenditionsRemover(DocumentModel source) {
        super(source.getCoreSession());
        this.proxy = source;
    }

    @Override
    public void run() throws ClientException {

        String targetUUID = (String) proxy.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY);

        String query = "select * from Document where ";
        query = query + RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY + "='"
                + targetUUID + "' ";

        query = query + " AND ecm:parentId='" + proxy.getParentRef().toString()
                + "'";

        List<String> removedProxyIds = new ArrayList<String>();
        List<DocumentModel> docs = session.query(query);

        // Get removed proxy ids
        for (DocumentModel doc : docs) {
            if (!doc.getId().equals(proxy.getId())) {
                removedProxyIds.add(doc.getId());
            }
        }

        // Notify rendition published for copy of relations from removed proxies
        // to new proxy
        notifyRenditionPublished(removedProxyIds);

        // Perform remove
        for (String docId : removedProxyIds) {
            session.removeDocument(new IdRef(docId));
        }
    }

    protected void notifyRenditionPublished(List<String> removedProxyIds)
            throws ClientException {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(CoreEventConstants.REPLACED_PROXY_IDS,
                (Serializable) removedProxyIds);
        notifyEvent(RENDITION_PROXY_PUBLISHED, proxy, options);
    }

    protected void notifyEvent(String eventId, DocumentModel doc,
            Map<String, Serializable> options) throws ClientException {
        CoreSession session = doc.getCoreSession();
        DocumentEventContext ctx = new DocumentEventContext(session,
                session.getPrincipal(), doc);
        if (options != null) {
            ctx.setProperties(options);
        }
        ctx.setProperty("category",
                DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        Event event = ctx.newEvent(eventId);
        getEventService().fireEvent(event);
    }

    protected EventService getEventService() {
        return Framework.getLocalService(EventService.class);
    }

}
