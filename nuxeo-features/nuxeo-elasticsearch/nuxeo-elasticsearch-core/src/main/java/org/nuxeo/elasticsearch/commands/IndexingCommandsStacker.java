/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.commands;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BINARYTEXT_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_IMPORTED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PROXY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_RESTORED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_TAG_UPDATED;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_TRASHED;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_UNTRASHED;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Contains logic to stack ElasticSearch commands depending on Document events This class is mainly here to make testing
 * easier
 */
public abstract class IndexingCommandsStacker {

    protected static final Log log = LogFactory.getLog(IndexingCommandsStacker.class);

    protected abstract Map<String, IndexingCommands> getAllCommands();

    protected abstract boolean isSyncIndexingByDefault();

    protected IndexingCommands getCommands(DocumentModel doc) {
        return getAllCommands().get(getDocKey(doc));
    }

    public void stackCommand(DocumentEventContext docCtx, String eventId) {
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null) {
            return;
        }
        if ("/".equals(doc.getPathAsString())) {
            if (log.isDebugEnabled()) {
                log.debug("Skip indexing command for root document");
            }
            if (eventId.equals(DOCUMENT_SECURITY_UPDATED)) {
                if (log.isDebugEnabled()) {
                    log.debug("Indexing root document children to update their permissions");
                }
                DocumentModelList children = doc.getCoreSession().getChildren(doc.getRef());
                children.forEach(child -> stackCommand(child, docCtx, eventId));
            }
        } else {
            stackCommand(doc, docCtx, eventId);
        }
    }

    protected void stackCommand(DocumentModel doc, DocumentEventContext docCtx, String eventId) {
        Boolean block = (Boolean) docCtx.getProperty(ElasticSearchConstants.DISABLE_AUTO_INDEXING);
        if (block != null && block) {
            if (log.isDebugEnabled()) {
                log.debug("Indexing is disable, skip indexing command for doc " + doc);
            }
            return;
        }
        boolean sync = isSynchronous(docCtx, doc);
        stackCommand(doc, eventId, sync);
    }

    protected boolean isSynchronous(DocumentEventContext docCtx, DocumentModel doc) {
        // 1. look at event context
        Boolean sync = (Boolean) docCtx.getProperty(ElasticSearchConstants.ES_SYNC_INDEXING_FLAG);
        if (sync != null) {
            return sync;
        }
        // 2. look at document context
        sync = (Boolean) doc.getContextData(ElasticSearchConstants.ES_SYNC_INDEXING_FLAG);
        if (sync != null) {
            return sync;
        }
        // 3. get the default
        sync = isSyncIndexingByDefault();
        return sync;
    }

    protected void stackCommand(DocumentModel doc, String eventId, boolean sync) {
        IndexingCommands cmds = getOrCreateCommands(doc);
        Type type;
        boolean recurse = false;
        switch (eventId) {
        case DOCUMENT_CREATED:
        case DOCUMENT_IMPORTED:
            type = Type.INSERT;
            break;
        case DOCUMENT_CREATED_BY_COPY:
            type = Type.INSERT;
            recurse = isFolderish(doc);
            break;
        case BEFORE_DOC_UPDATE:
        case DOCUMENT_CHECKEDOUT:
        case BINARYTEXT_UPDATED:
        case DOCUMENT_TAG_UPDATED:
        case DOCUMENT_PROXY_UPDATED:
        case LifeCycleConstants.TRANSITION_EVENT:
        case DOCUMENT_TRASHED:
        case DOCUMENT_UNTRASHED:
        case DOCUMENT_RESTORED:
            if (doc.isProxy() && !doc.isImmutable()) {
                stackCommand(doc.getCoreSession().getDocument(new IdRef(doc.getSourceId())), BEFORE_DOC_UPDATE, false);
            }
            type = Type.UPDATE;
            break;
        case DOCUMENT_CHECKEDIN:
            if (indexIsLatestVersion()) {
                CoreSession session = doc.getCoreSession();
                if (session != null) {
                    // The previous doc version with isLastestVersion and isLatestMajorVersion need to be updated
                    // Here we have no way to get this exact doc version so we reindex all versions
                    for (DocumentModel version : doc.getCoreSession().getVersions(doc.getRef())) {
                        stackCommand(version, BEFORE_DOC_UPDATE, false);
                    }
                }
            }
            type = Type.UPDATE;
            break;
        case DOCUMENT_MOVED:
            type = Type.UPDATE;
            recurse = isFolderish(doc);
            break;
        case DOCUMENT_REMOVED:
            type = Type.DELETE;
            recurse = isFolderish(doc);
            break;
        case DOCUMENT_SECURITY_UPDATED:
            type = Type.UPDATE_SECURITY;
            recurse = isFolderish(doc);
            break;
        case DOCUMENT_CHILDREN_ORDER_CHANGED:
            type = Type.UPDATE_DIRECT_CHILDREN;
            recurse = true;
            break;
        default:
            return;
        }
        if (sync && recurse) {
            // split into 2 commands one sync and an async recurse
            cmds.add(type, true, false);
            cmds.add(type, false, true);
        } else {
            cmds.add(type, sync, recurse);
        }
    }

    private boolean indexIsLatestVersion() {
        return !Framework.isBooleanPropertyTrue(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY);
    }

    private boolean isFolderish(DocumentModel doc) {
        return doc.isFolder() && !doc.isVersion();
    }

    protected IndexingCommands getOrCreateCommands(DocumentModel doc) {
        IndexingCommands cmds = getCommands(doc);
        if (cmds == null) {
            cmds = new IndexingCommands(doc);
            getAllCommands().put(getDocKey(doc), cmds);
        }
        return cmds;
    }

    protected String getDocKey(DocumentModel doc) {
        // Don't merge commands with different session, so we work only on attached doc
        return doc.getId() + "#" + doc.getSessionId();
    }

}
