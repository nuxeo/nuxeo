/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.importer.stream.consumer;

import static org.nuxeo.runtime.transaction.TransactionHelper.commitOrRollbackTransaction;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.consumer.AbstractConsumer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Consumes DocumentMessage and produce Nuxeo document.
 *
 * @since 9.1
 */
public class DocumentMessageConsumer extends AbstractConsumer<DocumentMessage> {
    private static final Log log = LogFactory.getLog(DocumentMessageConsumer.class);

    protected final String rootPath;

    protected final String repositoryName;

    protected CoreSession session;

    public DocumentMessageConsumer(String consumerId, String repositoryName, String rootPath) {
        super(consumerId);
        this.rootPath = rootPath;
        this.repositoryName = repositoryName;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (session != null) {
            ((CloseableCoreSession) session).close();
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Override
    public void begin() {
        TransactionHelper.startTransaction();
        if (session == null) {
            this.session = CoreInstance.openCoreSessionSystem(repositoryName);
        }
    }

    @Override
    public void accept(DocumentMessage message) {
        DocumentModel doc = session.createDocumentModel(rootPath + message.getParentPath(), message.getName(),
                message.getType());
        doc.putContextData(CoreSession.SKIP_DESTINATION_CHECK_ON_CREATE, true);
        Blob blob = getBlob(message);
        if (blob != null) {
            // doc.setProperty("file", "filename", blob.getFilename());
            doc.setProperty("file", "content", blob);
        }
        Map<String, Serializable> props = message.getProperties();
        if (props != null && !props.isEmpty()) {
            setDocumentProperties(doc, props);
        }
        doc = session.createDocument(doc);
    }

    protected Blob getBlob(DocumentMessage message) {
        Blob blob = null;
        if (message.getBlob() != null) {
            blob = message.getBlob();
        } else if (message.getBlobInfo() != null) {
            BlobInfo blobInfo = message.getBlobInfo();
            blob = new SimpleManagedBlob(blobInfo);
        }
        return blob;
    }

    @Override
    public void commit() {
        log.debug("commit");
        session.save();
        // TODO: here if tx is in rollback we must throw something
        commitOrRollbackTransaction();
    }

    @Override
    public void rollback() {
        log.info("rollback");
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
    }

    protected void setDocumentProperties(DocumentModel doc, Map<String, Serializable> properties) {
        for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
            try {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            } catch (PropertyNotFoundException e) {
                String message = String.format("Property '%s' not found on document type: %s. Skipping it.",
                        entry.getKey(), doc.getType());
                log.error(message, e);
            }
        }
    }

}
