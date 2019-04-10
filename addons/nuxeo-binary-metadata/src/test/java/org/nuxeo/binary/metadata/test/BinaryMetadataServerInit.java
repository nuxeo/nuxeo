/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.test;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * @since 7.1
 */
public class BinaryMetadataServerInit implements RepositoryInit {

    public static DocumentModel getFile(int index, CoreSession session) {
        return session.getDocument(new PathRef("/folder/file_" + index));
    }

    @Override
    public void populate(CoreSession session) {
        // Create folder
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "Folder");
        session.createDocument(doc);

        // Create files
        for (int i = 0; i < 5; i++) {
            doc = session.createDocumentModel("/folder", "file_" + i, "File");
            doc.setPropertyValue("dc:title", "file " + i);
            doc = session.createDocument(doc);
        }

        // Attach binaries
        try {
            // Sound - MP3
            doc = getFile(0, session);
            File binary = FileUtils.getResourceFileFromContext("data/twist.mp3");
            Blob fb = Blobs.createBlob(binary);
            fb.setMimeType("audio/mpeg3");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
            session.saveDocument(doc);

            // PDF
            doc = getFile(1, session);
            binary = FileUtils.getResourceFileFromContext("data/hello.pdf");
            fb = Blobs.createBlob(binary);
            fb.setMimeType("application/pdf");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
            session.saveDocument(doc);

            // Image - PNG
            doc = getFile(2, session);
            binary = FileUtils.getResourceFileFromContext("data/training.png");
            fb = Blobs.createBlob(binary);
            fb.setMimeType("image/png");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
            session.saveDocument(doc);

            // Photoshop - PSD
            doc = getFile(3, session);
            binary = FileUtils.getResourceFileFromContext("data/montagehp.psd");
            fb = Blobs.createBlob(binary);
            fb.setMimeType("application/octet-stream");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
            session.saveDocument(doc);

            // Image - JPG
            doc = getFile(4, session);
            binary = FileUtils.getResourceFileFromContext("data/china.jpg");
            fb = Blobs.createBlob(binary);
            fb.setMimeType("image/jpeg");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
            session.saveDocument(doc);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        session.save();
    }
}
