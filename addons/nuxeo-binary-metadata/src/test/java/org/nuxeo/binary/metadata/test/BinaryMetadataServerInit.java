/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
