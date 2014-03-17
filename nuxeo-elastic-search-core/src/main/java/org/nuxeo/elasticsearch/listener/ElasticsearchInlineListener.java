/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.elasticsearch.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.SESSION_SAVED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.commands.IndexingCommandsStacker;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;

/**
 * Synchronous Event listener used to schedule a reindexing
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class ElasticsearchInlineListener extends IndexingCommandsStacker
        implements EventListener {

    private static final Log log = LogFactory.getLog(ElasticsearchInlineListener.class);

    protected static ThreadLocal<Map<String, IndexingCommands>> transactionCommands = new ThreadLocal<Map<String, IndexingCommands>>() {
        @Override
        protected HashMap<String, IndexingCommands> initialValue() {
            return new HashMap<String, IndexingCommands>();
        }
    };

    @Override
    protected Map<String, IndexingCommands> getAllCommands() {
        return transactionCommands.get();
    }

    @Override
    public void handleEvent(Event event) throws ClientException {

        String eventId = event.getName();

        if (event.isCommitEvent()) {
            flushCommands(event.getContext().getCoreSession());
            return;
        }

        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            // don't process Events that are not tied to Documents
            return;
        }
        stackCommand(docCtx, eventId);
    }

    protected void stackCommand(DocumentEventContext docCtx, String eventId) {
        DocumentModel doc = docCtx.getSourceDocument();

        if (doc==null) {
            return;
        }

        Boolean block = (Boolean) docCtx.getProperty(EventConstants.DISABLE_AUTO_INDEXING);
        if (block != null && block) {
            // ignore the event - we are blocked by the caller
            log.debug("Skip indexing for doc " + docCtx.getSourceDocument());
            return;
        }

        Boolean sync = (Boolean) docCtx.getProperty(EventConstants.ES_SYNC_INDEXING_FLAG);
        if (sync == null) {
            sync = (Boolean) doc.getContextData().get(EventConstants.ES_SYNC_INDEXING_FLAG);
        }

        if (sync == null) {
            sync = false;
        }

        stackCommand(doc, eventId, sync);
    }

    protected void fireSyncIndexing(CoreSession session,
            List<IndexingCommand> syncCommands) throws ClientException {
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        for (IndexingCommand cmd : syncCommands) {
            esi.scheduleIndexing(cmd);
        }
    }

    protected void fireAsyncIndexing(CoreSession session,
            List<IndexingCommand> asyncCommands) throws ClientException {
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        for (IndexingCommand cmd : asyncCommands) {
            esi.scheduleIndexing(cmd);
        }
    }

}
