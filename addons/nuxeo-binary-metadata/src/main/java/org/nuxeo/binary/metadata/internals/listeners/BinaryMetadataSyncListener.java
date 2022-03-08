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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.binary.metadata.internals.listeners;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.partitioningBy;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.DISABLE_BINARY_METADATA_LISTENER;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.binary.metadata.internals.BinaryMetadataUpdateWork;
import org.nuxeo.binary.metadata.internals.MetadataMappingUpdate;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Handle document and blob updates according to following rules in an event context:
 * <ul>
 * <li>Define if rule should be executed in async or sync mode.
 * <li>If creation, write metadata from Blob to doc.
 * <li>If Blob dirty and document metadata dirty, write metadata from doc to Blob.
 * <li>If Blob dirty and document metadata not dirty, write metadata from Blob to doc.
 * <li>If Blob not dirty and document metadata dirty, write metadata from doc to Blob.
 * </ul>
 *
 * @since 7.1
 */
public class BinaryMetadataSyncListener implements EventListener, Synchronization {

    private static final Logger log = LogManager.getLogger(BinaryMetadataSyncListener.class);

    protected static final ThreadLocal<Boolean> IS_ENLISTED = ThreadLocal.withInitial(() -> FALSE);

    protected static final ThreadLocal<List<DocumentMetadataMappingUpdates>> TRANSACTION_UPDATES = ThreadLocal.withInitial(
            ArrayList::new);

    @Override
    public void handleEvent(Event event) {
        String eventName = event.getName();
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        boolean creation = ABOUT_TO_CREATE.equals(eventName) || DOCUMENT_CREATED.equals(eventName);
        boolean update = BEFORE_DOC_UPDATE.equals(eventName);
        if (!creation && !update) {
            return;
        }
        // skip processing if document is a proxy/version or it is disabled (useful when the work updates the metadata)
        DocumentModel doc = ((DocumentEventContext) ctx).getSourceDocument();
        if (doc.isProxy() || doc.isVersion() || isTrue((Boolean) ctx.getProperty(DISABLE_BINARY_METADATA_LISTENER))) {
            return;
        }

        if (!IS_ENLISTED.get()) {
            // try to enlist our listener
            IS_ENLISTED.set(registerSynchronization(this));
        }

        BinaryMetadataService binaryMetadataService = Framework.getService(BinaryMetadataService.class);
        Map<Boolean, List<MetadataMappingUpdate>> metadataUpdates = binaryMetadataService.getMetadataUpdates(doc,
                creation).stream().collect(partitioningBy(MetadataMappingUpdate::isAsync));

        // apply metadata updates that need to be done synchronously
        // - on aboutToCreate to keep backward compatibility
        // - on beforeDocumentModification because getMetadataUpdates checks the dirty state
        if (ABOUT_TO_CREATE.equals(eventName) || BEFORE_DOC_UPDATE.equals(eventName)) {
            binaryMetadataService.applyUpdates(doc, metadataUpdates.get(FALSE));
        }
        // collect metadata updates to perform asynchronously
        // - on documentCreated because we need the docId to schedule the work
        // - on beforeDocumentModification because getMetadataUpdates checks the dirty state
        if (DOCUMENT_CREATED.equals(eventName) || BEFORE_DOC_UPDATE.equals(eventName)) {
            List<MetadataMappingUpdate> asyncUpdates = metadataUpdates.get(TRUE);
            if (!asyncUpdates.isEmpty()) {
                TRANSACTION_UPDATES.get().add(new DocumentMetadataMappingUpdates(doc, asyncUpdates));
            }
        }
    }

    @Override
    public void beforeCompletion() {
        // nothing to do
    }

    @Override
    public void afterCompletion(int status) {
        try {
            if (STATUS_MARKED_ROLLBACK == status || STATUS_ROLLEDBACK == status) {
                return;
            }
            WorkManager workManager = Framework.getService(WorkManager.class);
            TRANSACTION_UPDATES.get()
                               .stream()
                               .map(tu -> new BinaryMetadataUpdateWork(tu.doc.getRepositoryName(), tu.doc.getId(),
                                       tu.updates))
                               .forEach(workManager::schedule);
        } finally {
            IS_ENLISTED.set(FALSE);
            TRANSACTION_UPDATES.get().clear();
        }
    }

    protected boolean registerSynchronization(Synchronization sync) {
        try {
            TransactionManager tm = TransactionHelper.lookupTransactionManager();
            if (tm.getTransaction() != null) {
                tm.getTransaction().registerSynchronization(sync);
                return true;
            }
            if (!Framework.isTestModeSet()) {
                log.error("Unable to register synchronization : no active transaction");
            }
            return false;
        } catch (NamingException | IllegalStateException | SystemException | RollbackException e) {
            log.error("Unable to register synchronization", e);
            return false;
        }
    }

    // @since 2021.13
    protected static class DocumentMetadataMappingUpdates {

        protected final DocumentModel doc;

        protected final List<MetadataMappingUpdate> updates;

        public DocumentMetadataMappingUpdates(DocumentModel doc, List<MetadataMappingUpdate> updates) {
            this.doc = doc;
            this.updates = updates;
        }
    }
}
