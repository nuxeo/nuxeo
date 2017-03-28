/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
    public void handleEvent(Event event) {
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
        try {
            if (getAllCommands().isEmpty()) {
                // return and un hook the current listener even if there's no commands to index
                // unless, during next transaction this listener won't be hooked to it
                return;
            }
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
