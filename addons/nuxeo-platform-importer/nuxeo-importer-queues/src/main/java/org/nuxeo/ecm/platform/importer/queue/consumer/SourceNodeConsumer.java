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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.3
 */
public class SourceNodeConsumer extends AbstractConsumer<SourceNode> {

    protected final FileManager fileManager;

    protected final String rootPath;

    public SourceNodeConsumer(ImporterLogger log, DocumentModel root, int batchSize, QueuesManager<SourceNode> queuesManager, int queue) {
        super(log, root, batchSize, queuesManager, queue);
        fileManager = Framework.getService(FileManager.class);
        rootPath = root.getPathAsString();
    }

    protected String getType() {
        return "File";
    }

    @Override
    public DocumentModel process(CoreSession session, SourceNode node) throws IOException {
        String fileName = null;
        String name = null;
        BlobHolder bh = node.getBlobHolder();
        if (bh != null) {
            Blob blob = bh.getBlob();
            if (blob != null) {
                fileName = blob.getFilename();
            }
            Map<String, Serializable> props = bh.getProperties();
            if (props != null) {
                name = (String) props.get("name");
            }
            if (name == null) {
                name = fileName;
            } else if (fileName == null) {
                fileName = name;
            }

            DocumentModel doc = session.createDocumentModel(rootPath, name, getType());

            doc.setProperty("dublincore", "title", name);
            doc.setProperty("file", "filename", fileName);
            doc.setProperty("file", "content", bh.getBlob());

            doc = setDocumentProperties(session, bh.getProperties(), doc);
            doc = session.createDocument(doc);
            return doc;
        }
        return null;
    }

    protected DocumentModel setDocumentProperties(CoreSession session, Map<String, Serializable> properties,
            DocumentModel doc) {
        if (properties != null) {

            for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
                try {
                    doc.setPropertyValue(entry.getKey(), entry.getValue());
                } catch (PropertyNotFoundException e) {
                    String message = String.format("Property '%s' not found on document type: %s. Skipping it.",
                            entry.getKey(), doc.getType());
                    log.error(message, e);
                }
            }
            doc = session.saveDocument(doc);
        }
        return doc;
    }

    @Override
    public double getNbDocsCreated() {
        return getNbProcessed();
    }

}
