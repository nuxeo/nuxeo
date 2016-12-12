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
 */
package org.nuxeo.ecm.platform.importer.queue.consumer;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.source.ImmutableNode;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @since 9.1
 */
public class ImmutableNodeConsumer extends AbstractConsumer<ImmutableNode> {

    protected final FileManager fileManager;

    protected final String rootPath;

    public ImmutableNodeConsumer(ImporterLogger log, DocumentModel root, int batchSize, QueuesManager<ImmutableNode> queuesManager, int queue) {
        super(log, root, batchSize, queuesManager, queue);
        fileManager = Framework.getService(FileManager.class);
        rootPath = root.getPathAsString();
    }

    @Override
    public DocumentModel process(CoreSession session, ImmutableNode node) throws IOException {
        DocumentModel doc = session.createDocumentModel(rootPath + node.getParentPath(), node.getName(), node.getType());
        Blob blob = node.getBlob();
        if (blob != null) {
            doc.setProperty("file", "filename", blob.getFilename());
            doc.setProperty("file", "content", blob);
        }
        Map<String, Serializable> props = node.getProperties();
        if (props != null && ! props.isEmpty()) {
            setDocumentProperties(session, props, doc);
        }
        doc = session.createDocument(doc);
        return doc;
    }



    protected void setDocumentProperties(CoreSession session, Map<String, Serializable> properties,
                                                  DocumentModel doc) {
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

    @Override
    public double getNbDocsCreated() {
        return getNbProcessed();
    }

}
