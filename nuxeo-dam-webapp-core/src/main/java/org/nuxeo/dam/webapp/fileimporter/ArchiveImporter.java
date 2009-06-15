/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand
 *
 * $Id$
 */

package org.nuxeo.dam.webapp.fileimporter;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

public class ArchiveImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typeService) throws ClientException, IOException {

        String extension = null;
        if (filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        java.io.File tmp = File.createTempFile("import", extension);
        try {
            File archive = new File(tmp);
            content.transferTo(archive);

            if (archive.isArchive()) {
                try {
                    FileManager fileManager = Framework.getService(FileManager.class);

                    for (java.io.File entry : archive.listFiles()) {
                        importFileRec(documentManager, entry, path, overwrite,
                                fileManager);
                    }
                } catch (Exception e) {
                    throw new ClientException("Failed to import archive: "
                            + e.getMessage(), e);
                }
            }
        } finally {
            tmp.delete();
        }
        return documentManager.getDocument(new PathRef(path));
    }

    protected void importFileRec(CoreSession documentManager,
            java.io.File file, String path, boolean overwrite,
            FileManager fileManager) throws Exception {
        if (file.isDirectory()) {
            for (java.io.File child : file.listFiles()) {
                // TODO: preserve the sub folder structure here by creating sub
                // folders accordingly
                importFileRec(documentManager, child, path, overwrite,
                        fileManager);
            }
        } else {
            // buidl a streaming blob without loading all the file content in
            // memory
            InputStream is = new FileInputStream(file);
            Blob blob = StreamingBlob.createFromStream(is);
            blob.setFilename(file.getName());
            fileManager.createDocumentFromBlob(documentManager, blob, path,
                    overwrite, file.getAbsolutePath());
        }
    }
}
