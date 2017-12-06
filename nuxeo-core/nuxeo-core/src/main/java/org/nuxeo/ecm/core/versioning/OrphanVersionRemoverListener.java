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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.versioning;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Async listener that is in charge to delete the versions. Before running the delete operation on the versions passed
 * as argument of the event, it will call the registred {@link OrphanVersionRemovalFilter} to allow them to mark some of
 * the orphan versions to be kept.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class OrphanVersionRemoverListener implements PostCommitEventListener {

    protected static final Log log = LogFactory.getLog(OrphanVersionRemoverListener.class);

    @Override
    public void handleEvent(EventBundle events) {
        if (events.containsEventName(DefaultVersionRemovalPolicy.ORPHAN_VERSION_REMOVE)) {
            for (Event event : events) {
                if (!event.getName().equals(DefaultVersionRemovalPolicy.ORPHAN_VERSION_REMOVE)) {
                    continue;
                }
                EventContext ctx = event.getContext();
                CoreSession session = ctx.getCoreSession();
                Object[] args = ctx.getArguments();
                if (args.length == 2) {
                    DocumentModel doc = (DocumentModel) args[0];
                    ShallowDocumentModel deletedLiveDoc = null;
                    if (doc instanceof ShallowDocumentModel) {
                        deletedLiveDoc = (ShallowDocumentModel) doc;
                    } else {
                        // cluster node has still no fetched invalidation
                        // so ShallowDocumentModel has been reconnected via the cache !
                        deletedLiveDoc = new ShallowDocumentModel(doc);
                    }
                    List<String> versionUUIDs = (List<String>) args[1];
                    removeIfPossible(session, deletedLiveDoc, versionUUIDs);
                }
            }
        }
    }

    protected Collection<OrphanVersionRemovalFilter> getFilters() {
        return Framework.getService(CoreService.class).getOrphanVersionRemovalFilters();
    }

    protected void removeIfPossible(CoreSession session, ShallowDocumentModel deletedLiveDoc, List<String> versionUUIDs)
            {
        session.save(); // receive invalidations if no tx

        for (OrphanVersionRemovalFilter filter : getFilters()) {
            versionUUIDs = filter.getRemovableVersionIds(session, deletedLiveDoc, versionUUIDs);
            if (versionUUIDs.size() == 0) {
                break;
            }
        }

        for (String id : versionUUIDs) {
            IdRef idRef = new IdRef(id);
            if (session.exists(idRef)) {
                log.debug("Removing version: " + id);
                session.removeDocument(idRef);
            }
        }

        session.save(); // send invalidations if no tx
    }

}
