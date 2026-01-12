/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.elasticsearch.versioning;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.elasticsearch.versioning.ReindexVersionsAction.REINDEX_VERSIONS_ACTION;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listens move and notify all moved children to update their versions in order to reindex their ecm:path. Even if
 * versions are placeless there are cases where we want to update their path. Activating this listener might impact
 * performance.
 *
 * @since 2025.14
 */
public class ReindexVersionsOnMoveListener implements EventListener {

    private static final Logger log = LogManager.getLogger(ReindexVersionsOnMoveListener.class);

    @Override
    public void handleEvent(Event event) {
        if (!event.getName().equals(DocumentEventTypes.DOCUMENT_MOVED)) {
            return;
        }
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        CoreSession session = ctx.getCoreSession();
        var srcDoc = ctx.getSourceDocument();
        reindexAllVersions(session, srcDoc);
        if (srcDoc.isFolder()) {
            // Schedule a BAF to trigger the reindexing of descendant's versions.
            var nxql = String.format("SELECT ecm:uuid FROM Document WHERE ecm:ancestorId = '%s'", srcDoc.getId());
            BulkService service = Framework.getService(BulkService.class);
            BulkCommand command = new BulkCommand.Builder(REINDEX_VERSIONS_ACTION, nxql, SYSTEM_USERNAME).repository(
                    session.getRepositoryName()).build();
            service.submitTransactional(command);
            log.debug("Submit reindex versions on move command: {}", command);
        }
    }

    protected static void reindexAllVersions(CoreSession session, DocumentModel srcDoc) {
        session.getVersions(srcDoc.getRef()).forEach(version -> {
            DocumentEventContext ctx = new DocumentEventContext(session, null, version);
            ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
            ctx.setProperty(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                    DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
            Event event = ctx.newEvent(DocumentEventTypes.BEFORE_DOC_UPDATE);
            Framework.getService(EventService.class).fireEvent(event);
        });
    }

}
