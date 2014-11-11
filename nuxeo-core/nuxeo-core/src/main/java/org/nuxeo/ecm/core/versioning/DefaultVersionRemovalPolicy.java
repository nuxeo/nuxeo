/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.versioning;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.api.Framework;

/**
 * Removes the version history if no proxies exist, otherwise do nothing.
 *
 * @author Florent Guillaume
 */
public class DefaultVersionRemovalPolicy implements VersionRemovalPolicy {

    private static final Log log = LogFactory.getLog(DefaultVersionRemovalPolicy.class);

    public static final String ORPHAN_VERSION_REMOVE = "orphan_versions_to_remove";

    @Override
    public void removeVersions(Session session, Document doc,
            CoreSession coreSession) throws ClientException {
        try {
            Collection<Document> proxies = session.getProxies(doc, null);
            if (doc.isProxy()) {
                // if doc is itself a proxy it should not be considered
                // in the list of remaining proxies
                proxies.remove(doc);
                if (proxies.isEmpty()) {
                    // removal of last proxy
                    Document source = doc.getSourceDocument();
                    if (source.isVersion()) {
                        // get live doc from version
                        try {
                            source = source.getSourceDocument();
                        } catch (NoSuchDocumentException e) {
                            // live already removed
                            source = null;
                        }
                    } // else was a live proxy
                    // if a live doc remains, still don't remove versions
                    if (source != null) {
                        return;
                    }
                }
            }
            if (proxies.isEmpty()) {
                List<String> versionsIds = doc.getVersionsIds();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Removing %s versions for: %s",
                            versionsIds.size(), doc.getUUID()));
                }

                if (versionsIds.size() > 0) {
                    DocumentModel docModel = coreSession.getDocument(new IdRef(
                            doc.getUUID()));
                    EventContext evtctx = new EventContextImpl(coreSession,
                            coreSession.getPrincipal(),
                            new ShallowDocumentModel(docModel), versionsIds);
                    Event evt = evtctx.newEvent(ORPHAN_VERSION_REMOVE);
                    Framework.getLocalService(EventService.class).fireEvent(evt);
                }
            }
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }
}
