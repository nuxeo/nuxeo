/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileBlobHolder;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;

/**
 * Custom implementation of the {@link ImporterDocumentModelFactory}. Provides : - container doc type configuration from
 * service - leaf doc type configuration from service
 *
 * @author Thierry Delprat
 */
public class ScanedFileFactory extends DefaultDocumentModelFactory implements ImporterDocumentModelFactory {

    protected static String targetContainerType = null;

    protected ImporterConfig config;

    public ScanedFileFactory(ImporterConfig config) {
        super();
        this.config = config;
    }

    protected String getTargetContainerType() {
        if (targetContainerType == null) {
            ScannedFileMapperService service = Framework.getLocalService(ScannedFileMapperService.class);
            targetContainerType = service.getTargetContainerType();
        }
        return targetContainerType;
    }

    @Override
    public DocumentModel createFolderishNode(CoreSession session, DocumentModel parent, SourceNode node) {

        String docType = getTargetContainerType();
        String name = getValidNameFromFileName(node.getName());
        boolean isUpdateDocument = false;

        PathRef ref = new PathRef(parent.getPathAsString(), name);
        if (parent.getPathAsString().equals(config.getTargetPath())
                && node.getSourcePath().equals(config.getSourcePath())) {
            // initial root creation
            if (!config.isCreateInitialFolder()) {
                return parent;
            }
            if (config.isMergeInitialFolder()) {
                if (session.exists(ref)) {
                    return session.getDocument(ref);
                }
            } else {
                if (session.exists(ref)) {
                    if (config.isUpdate()) {
                        isUpdateDocument = true;
                    } else {
                        name = name + "-" + System.currentTimeMillis();
                    }
                }
            }
        } else {
            if (session.exists(ref) && config.isUpdate()) {
                isUpdateDocument = true;
            }
        }

        Map<String, Object> options = new HashMap<>();
        DocumentModel doc;
        if (isUpdateDocument) {
            doc = session.getDocument(ref);
        } else {
            doc = session.createDocumentModel(docType, options);
            doc.setPathInfo(parent.getPathAsString(), name);
            doc.setProperty("dublincore", "title", node.getName());
            doc = session.createDocument(doc);
        }
        return doc;
    }

    @Override
    public DocumentModel createLeafNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException {

        BlobHolder bh = node.getBlobHolder();

        if (bh instanceof ScanFileBlobHolder) {
            ScanFileBlobHolder scanBH = (ScanFileBlobHolder) bh;
            String docType = scanBH.getTargetType();
            setLeafType(docType);
        }

        DocumentModel doc = defaultCreateLeafNode(session, parent, node);

        // XXX should be a callback on commit !!!
        if (node instanceof ScanedFileSourceNode) {
            ScanedFileSourceNode scanNode = (ScanedFileSourceNode) node;
            ScannedFileImporter.addProcessedDescriptor(scanNode.getDescriptorFileName());
        }

        return doc;
    }

    @Override
    protected DocumentModel defaultCreateLeafNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        BlobHolder bh = node.getBlobHolder();

        String name = getValidNameFromFileName(node.getName());
        String fileName = node.getName();

        DocumentRef docRef = new PathRef(parent.getPathAsString(), name);

        DocumentModel doc;
        boolean docExists = session.exists(docRef);
        if (docExists && config.isUpdate()) {
            doc = session.getDocument(docRef);
        } else {
            Map<String, Object> options = new HashMap<>();
            doc = session.createDocumentModel(leafType, options);
            doc.setPathInfo(parent.getPathAsString(), name);
            doc.setProperty("dublincore", "title", node.getName());
        }
        doc.setProperty("file", "filename", fileName);
        doc.setProperty("file", "content", bh.getBlob());

        if (docExists && config.isUpdate()) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }

        doc = setDocumentProperties(session, bh.getProperties(), doc);

        return doc;
    }
}
