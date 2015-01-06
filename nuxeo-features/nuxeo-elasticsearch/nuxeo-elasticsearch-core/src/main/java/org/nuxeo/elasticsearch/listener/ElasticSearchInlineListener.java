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
 *     Thierry Delprat
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.commands.IndexingCommandsStacker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Synchronous Event listener used to record indexing command, submitted after commit completion.
 */
public class ElasticSearchInlineListener extends IndexingCommandsStacker implements EventListener, Synchronization {

    private static final Log log = LogFactory.getLog(ElasticSearchInlineListener.class);

    protected static ThreadLocal<Map<String, IndexingCommands>> transactionCommands = new ThreadLocal<Map<String, IndexingCommands>>() {
        @Override
        protected HashMap<String, IndexingCommands> initialValue() {
            return new HashMap<>();
        }
    };

    protected static ThreadLocal<Boolean> isEnlisted = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    @Override
    protected Map<String, IndexingCommands> getAllCommands() {
        return transactionCommands.get();
    }

    @Override
    protected boolean isSyncIndexingByDefault() {
        Boolean ret = useSyncIndexing.get();
        if (ret == null) {
            ret = false;
        }
        return ret;
    }

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();
        if (!isEnlisted.get()) {
            if (event.isCommitEvent()) {
                // manual flush on save if TxManager is not hooked
                afterCompletion(Status.STATUS_COMMITTED);
                return;
            }
            // try to enlist our listener
            isEnlisted.set(registerSynchronization(this));
        }
        if (!(event.getContext() instanceof DocumentEventContext)) {
            // don't process Events that are not tied to Documents
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        stackCommand(docCtx, eventId);
    }

    @Override
    public void beforeCompletion() {

    }

    @Override
    public void afterCompletion(int status) {
        if (getAllCommands().isEmpty()) {
            return;
        }
        try {
            if (Status.STATUS_MARKED_ROLLBACK == status || Status.STATUS_ROLLEDBACK == status) {
                return;
            }
            List<IndexingCommand> commandList = new ArrayList<>();
            for (IndexingCommands cmds : getAllCommands().values()) {
                for (IndexingCommand cmd : cmds.getCommands()) {
                    commandList.add(cmd);
                }
            }
            ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
            esi.runIndexingWorker(commandList);
        } finally {
            isEnlisted.set(false);
            getAllCommands().clear();
            useSyncIndexing.set(null);
        }
    }

    public static ThreadLocal<Boolean> useSyncIndexing = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }

        @Override
        public void set(Boolean value) {
            super.set(value);
            if (Boolean.TRUE.equals(value)) {
                // switch existing stack to sync
                for (IndexingCommands cmds : transactionCommands.get().values()) {
                    for (IndexingCommand cmd : cmds.getCommands()) {
                        cmd.makeSync();
                    }
                }
            }
        }
    };

    protected boolean registerSynchronization(Synchronization sync) {
        try {
            TransactionManager tm = TransactionHelper.lookupTransactionManager();
            if (tm != null) {
                if (tm.getTransaction() != null) {
                    tm.getTransaction().registerSynchronization(sync);
                    return true;
                }
                if (!Framework.isTestModeSet()) {
                    log.error("Unable to register synchronization : no active transaction");
                }
                return false;
            } else {
                log.error("Unable to register synchronization : no TransactionManager");
                return false;
            }
        } catch (NamingException | IllegalStateException | SystemException | RollbackException e) {
            log.error("Unable to register synchronization", e);
            return false;
        }
    }

}
