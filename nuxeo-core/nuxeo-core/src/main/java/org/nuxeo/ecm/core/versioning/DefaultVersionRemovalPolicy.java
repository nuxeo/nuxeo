/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.versioning;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.api.Framework;

/**
 * Removes the version history if no proxies exist, otherwise do nothing.
 *
 * @author Florent Guillaume
 */
public class DefaultVersionRemovalPolicy implements VersionRemovalPolicy {

    private static final Log log = LogFactory.getLog(DefaultVersionRemovalPolicy.class);

    public static final DefaultVersionRemovalPolicy INSTANCE = new DefaultVersionRemovalPolicy();

    public static final String ORPHAN_VERSION_REMOVE = "orphan_versions_to_remove";

    @Override
    public void removeVersions(Session session, Document doc, CoreSession coreSession) {
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
                    } catch (DocumentNotFoundException e) {
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
                log.debug(String.format("Removing %s versions for: %s", versionsIds.size(), doc.getUUID()));
            }

            if (versionsIds.size() > 0) {
                DocumentModel docModel = coreSession.getDocument(new IdRef(doc.getUUID()));
                EventContext evtctx = new EventContextImpl(coreSession, coreSession.getPrincipal(),
                        new ShallowDocumentModel(docModel), versionsIds);
                Event evt = evtctx.newEvent(ORPHAN_VERSION_REMOVE);
                Framework.getLocalService(EventService.class).fireEvent(evt);
            }
        }
    }
}
