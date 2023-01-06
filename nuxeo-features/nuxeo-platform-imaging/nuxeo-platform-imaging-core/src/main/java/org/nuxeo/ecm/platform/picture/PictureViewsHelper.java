/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 *     Thomas Roger<troger@nuxeo.com>
 *     Antoine Taillefer<ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.picture;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTOMATIC_VERSIONING;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTO_CHECKOUT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;
import static org.nuxeo.ecm.platform.picture.listener.PictureViewsGenerationListener.DISABLE_PICTURE_VIEWS_GENERATION_LISTENER;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Helper to compute the picture views of a document.
 *
 * @since 11.5
 */
public class PictureViewsHelper {

    private static final Logger log = LogManager.getLogger(PictureViewsHelper.class);

    public static final String PICTURE_VIEWS_TX_TIMEOUT_PROPERTY = "nuxeo.picture.views.transaction.timeout.seconds";

    public static final int DEFAULT_TX_TIMEOUT_SECONDS = 300;

    public static final String NOTHING_TO_PROCESS_MESSAGE = "Nothing to process";

    protected Integer transactionTimeout;

    /**
     * Computes the picture views of the document with the given {@code id} and property with the given {@code xpath}.
     * <p>
     * Uses the given {@code statusSetter} to eventually set a status at the different stages of the computation.
     */
    public void computePictureViews(CoreSession session, String id, String xpath, Consumer<String> statusSetter) {
        if (!session.exists(new IdRef(id))) {
            log.debug("Doc id doesn't exist: {}", id);
            statusSetter.accept(NOTHING_TO_PROCESS_MESSAGE);
            return;
        }

        DocumentModel workingDocument = session.getDocument(new IdRef(id));
        Blob blob = null;
        try {
            Property fileProp = workingDocument.getProperty(xpath);
            blob = (Blob) fileProp.getValue();
        } catch (PropertyNotFoundException e) {
            log.debug("No property: {} for doc: {}", xpath, id);
        }
        if (blob == null) {
            // do nothing
            log.debug("No blob for doc: {}", workingDocument);
            statusSetter.accept(NOTHING_TO_PROCESS_MESSAGE);
            return;
        }

        String title = workingDocument.getTitle();
        statusSetter.accept("Generating views");
        try {
            PictureResourceAdapter picture = workingDocument.getAdapter(PictureResourceAdapter.class);
            log.debug("Fill picture views for doc: {}", workingDocument);
            picture.fillPictureViews(blob, blob.getFilename(), title, null);
        } catch (DocumentNotFoundException e) {
            // a parent of the document may have been deleted.
            statusSetter.accept(NOTHING_TO_PROCESS_MESSAGE);
            return;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        if (!session.exists(new IdRef(id))) {
            log.debug("Doc id doesn't exist: {}", id);
            statusSetter.accept(NOTHING_TO_PROCESS_MESSAGE);
            return;
        }
        statusSetter.accept("Saving");
        if (workingDocument.isVersion()) {
            workingDocument.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        workingDocument.putContextData("disableNotificationService", Boolean.TRUE);
        workingDocument.putContextData(PARAM_DISABLE_AUDIT, Boolean.TRUE);
        workingDocument.putContextData(DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
        workingDocument.putContextData(DISABLE_PICTURE_VIEWS_GENERATION_LISTENER, Boolean.TRUE);
        workingDocument.putContextData(DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
        workingDocument.putContextData(DISABLE_AUTOMATIC_VERSIONING, Boolean.TRUE);
        session.saveDocument(workingDocument);

        statusSetter.accept("Done");
    }

    /**
     * Commits and starts a new transaction with a custom timeout.
     */
    public void newTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        log.debug("Commit and start transaction with timeout {}s", this::getTransactionTimeout);
        TransactionHelper.startTransaction(getTransactionTimeout());
        // timeout of command line executions will be aligned with the transaction timeout
    }

    public int getTransactionTimeout() {
        if (transactionTimeout == null) {
            String maxDurationStr = Framework.getProperty(PICTURE_VIEWS_TX_TIMEOUT_PROPERTY,
                    String.valueOf(DEFAULT_TX_TIMEOUT_SECONDS));
            transactionTimeout = Integer.parseInt(maxDurationStr);
        }
        return transactionTimeout;
    }

}
