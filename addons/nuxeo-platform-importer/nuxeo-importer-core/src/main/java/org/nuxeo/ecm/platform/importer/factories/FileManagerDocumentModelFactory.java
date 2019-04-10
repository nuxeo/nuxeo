/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.factories;

import java.io.IOException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * DocumentModel factory based on the {@code FileManager}. Use the {@code FileManager} to create Folderish and Leaf
 * Nodes.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class FileManagerDocumentModelFactory extends AbstractDocumentModelFactory {

    protected FileManager fileManager;

    @Override
    public DocumentModel createFolderishNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {
        FileManager fileManager = getFileManager();
        return fileManager.createFolder(session, node.getName(), parent.getPathAsString());
    }

    @Override
    public DocumentModel createLeafNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException {
        FileManager fileManager = getFileManager();
        BlobHolder bh = node.getBlobHolder();
        FileImporterContext context = FileImporterContext.builder(session, bh.getBlob(), parent.getPathAsString())
                                                         .overwrite(true)
                                                         .fileName(node.getName())
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        doc = setDocumentProperties(session, bh.getProperties(), doc);
        return doc;
    }

    protected FileManager getFileManager() {
        if (fileManager == null) {
            fileManager = Framework.getService(FileManager.class);
        }
        return fileManager;
    }

}
