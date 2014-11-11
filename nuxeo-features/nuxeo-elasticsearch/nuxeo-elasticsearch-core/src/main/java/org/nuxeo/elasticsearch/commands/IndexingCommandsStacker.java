/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.commands;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BINARYTEXT_UPDATED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Contains logic to stack ElasticSearch commands depending on Document events
 *
 * This class is mainly here to make testing easier
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public abstract class IndexingCommandsStacker {

    protected static final Log log = LogFactory.getLog(IndexingCommandsStacker.class);

    protected abstract Map<String, IndexingCommands> getAllCommands();

    protected IndexingCommands getCommands(DocumentModel doc) {
        return getAllCommands().get(doc.getId());
    }

    protected IndexingCommands getOrCreateCommands(DocumentModel doc) {
        IndexingCommands cmds = getCommands(doc);
        if (cmds == null) {
            cmds = new IndexingCommands(doc);
            getAllCommands().put(doc.getId(), cmds);
        }
        return cmds;
    }

    protected void stackCommand(DocumentModel doc, String eventId, boolean sync) {
        if (doc == null) {
            return;
        }
        IndexingCommands cmds = getOrCreateCommands(doc);

        if (DOCUMENT_CREATED.equals(eventId)) {
            cmds.add(IndexingCommand.INSERT, sync, false);
        } else if (BEFORE_DOC_UPDATE.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, false);
        } else if (DOCUMENT_CHECKEDOUT.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, false);
        } else if (DOCUMENT_CHECKEDIN.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, false);
        } else if (DOCUMENT_CREATED_BY_COPY.equals(eventId)) {
            cmds.add(IndexingCommand.INSERT, sync, doc.isFolder());
        } else if (LifeCycleConstants.TRANSITION_EVENT.equals(eventId)) {
            cmds.add(IndexingCommand.INSERT, sync, false);
        } else if (DOCUMENT_MOVED.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, doc.isFolder());
        } else if (DOCUMENT_SECURITY_UPDATED.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE_SECURITY, sync, doc.isFolder());
        } else if (DOCUMENT_REMOVED.equals(eventId)) {
            cmds.add(IndexingCommand.DELETE, sync, doc.isFolder());
        } else if (BINARYTEXT_UPDATED.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, false);
        }
    }

    protected boolean registerSynchronization(Synchronization sync) {
        try {
            TransactionManager tm = TransactionHelper.lookupTransactionManager();
            if (tm != null) {
                if (tm.getTransaction()!=null) {
                    tm.getTransaction().registerSynchronization(sync);
                    return true;
                }
                if (! Framework.isTestModeSet()) {
                    log.error(
                            "Unable to register synchronization : no active transaction");
                }
                return false;
            } else {
                log.error("Unable to register synchronization : no TransactionManager");
                return false;
            }
        } catch (Exception e) {
            log.error("Unable to register synchronization", e);
            return false;
        }
    }

    // never called because we don't have a proper hook for that !
    protected void prepareFlush() {
        Map<String, IndexingCommands> allCmds = getAllCommands();
        for (IndexingCommands cmds : allCmds.values()) {
            for (IndexingCommand cmd : cmds.getCommands()) {
                if (cmd.isSync()) {
                    cmd.computeIndexingEvent();
                }
            }
        }
    }

    protected void flushCommands() throws ClientException {
        Map<String, IndexingCommands> allCmds = getAllCommands();

        List<IndexingCommand> syncCommands = new ArrayList<>();
        List<IndexingCommand> asyncCommands = new ArrayList<>();

        for (IndexingCommands cmds : allCmds.values()) {
            for (IndexingCommand cmd : cmds.getCommands()) {
                if (cmd.isSync()) {
                    syncCommands.add(cmd);
                } else {
                    asyncCommands.add(cmd);
                }
            }
        }
        getAllCommands().clear();

        if (syncCommands.size() > 0) {
            fireSyncIndexing(syncCommands);
        }
        if (asyncCommands.size() > 0) {
            fireAsyncIndexing(asyncCommands);
        }
    }

    protected abstract void fireSyncIndexing(List<IndexingCommand> syncCommands)
            throws ClientException;

    protected abstract void fireAsyncIndexing(
            List<IndexingCommand> asyncCommands) throws ClientException;

}