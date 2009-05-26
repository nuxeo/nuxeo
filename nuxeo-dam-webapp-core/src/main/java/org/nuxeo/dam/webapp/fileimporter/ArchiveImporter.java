/*
 * (C) Copyright 2002 - 2009 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     qlamerand
 *
 *
 *
 */

package org.nuxeo.dam.webapp.fileimporter;


import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

public class ArchiveImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 5516654934499879209L;

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typeService) throws ClientException, IOException {

        java.io.File tmp = File.createTempFile("import",
                filename.substring(filename.lastIndexOf(".")));
        File archive = new File(tmp);
        content.transferTo(archive);

        if (archive.isArchive()) {
            try {
                FileManager service = Framework.getService(FileManager.class);

                for (java.io.File entry : archive.listFiles()) {
                    byte[] entryContent = ArchiveImporter.getBytesFromFile(entry);
                    ByteArrayBlob input = new ByteArrayBlob(entryContent);

                    service.createDocumentFromBlob(
                            documentManager, input, path, overwrite,
                            entry.getAbsolutePath());
                }
            } catch (Exception e) {
                throw new ClientException("Failed to import archive", e);
            }
        }

        tmp.delete();

        return documentManager.getDocument(new PathRef(path));
    }

    public static byte[] getBytesFromFile(java.io.File file) throws IOException {

        FileInputStream is = new FileInputStream(file);

        long length = file.length();
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];
        try {
            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read InputStream");
            }
        } finally {
            // Close the input stream and return bytes
            is.close();
        }
        return bytes;
    }

}
