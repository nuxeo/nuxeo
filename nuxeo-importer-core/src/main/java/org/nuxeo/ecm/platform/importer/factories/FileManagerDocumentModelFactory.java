/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.factories;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * DocumentModel factory based on the {@code FileManager}. Use the {@code
 * FileManager} to create Folderish and Leaf Nodes.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class FileManagerDocumentModelFactory extends
        AbstractDocumentModelFactory {

    protected FileManager fileManager;

    public DocumentModel createFolderishNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception {
        FileManager fileManager = getFileManager();
        return fileManager.createFolder(session,
                node.getName(),
                parent.getPathAsString());
    }

    public DocumentModel createLeafNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception {
        FileManager fileManager = getFileManager();
        BlobHolder bh = node.getBlobHolder();
        DocumentModel doc = fileManager.createDocumentFromBlob(session,
                bh.getBlob(), parent.getPathAsString(), true,
                node.getName());
        doc = setDocumentProperties(session, bh.getProperties(), doc);
        return doc;
    }

    protected FileManager getFileManager() throws Exception {
        if (fileManager == null) {
            fileManager = Framework.getService(FileManager.class);
        }
        return fileManager;
    }

}
