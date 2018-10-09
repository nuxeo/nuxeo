/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Remove proxy to the same stored rendition with a different version.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class RenditionsRemover extends UnrestrictedSessionRunner {

    public static final String RENDITION_PROXY_PUBLISHED = "renditionProxyPublished";

    protected final DocumentModel proxy;

    public RenditionsRemover(DocumentModel source) {
        super(source.getCoreSession());
        this.proxy = source;
    }

    @Override
    public void run() {

        String targetUUID = (String) proxy.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY);

        String query = "select * from Document where ";
        query = query + RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY + "='" + targetUUID + "' ";

        query = query + " AND ecm:parentId='" + proxy.getParentRef().toString() + "'";

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

    protected void notifyRenditionPublished(List<String> removedProxyIds) {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(CoreEventConstants.REPLACED_PROXY_IDS, (Serializable) removedProxyIds);
        notifyEvent(RENDITION_PROXY_PUBLISHED, proxy, options);
    }

    protected void notifyEvent(String eventId, DocumentModel doc, Map<String, Serializable> options)
            {
        CoreSession session = doc.getCoreSession();
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        if (options != null) {
            ctx.setProperties(options);
        }
        ctx.setProperty("category", DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        Event event = ctx.newEvent(eventId);
        getEventService().fireEvent(event);
    }

    protected EventService getEventService() {
        return Framework.getService(EventService.class);
    }

}
