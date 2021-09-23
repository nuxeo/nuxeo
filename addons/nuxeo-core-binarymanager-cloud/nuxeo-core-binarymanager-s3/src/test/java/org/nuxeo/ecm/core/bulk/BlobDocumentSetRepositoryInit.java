/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.bulk;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;


/**
 * @since 2021.9
 */
public class BlobDocumentSetRepositoryInit implements RepositoryInit {
    @Override
    public void populate(CoreSession session) {
        DocumentModel folder = session.createDocumentModel("/", "test", "Folder");
        folder = session.createDocument(folder);

        DocumentModel doc = session.createDocumentModel(folder.getPathAsString(), "file1", "File");
        Blob blob = Blobs.createBlob("A blob content");
        blob.setFilename("test_content_ok.doc");
        doc.setProperty("file", "content", blob);
        doc = session.createDocument(doc);

        doc = session.createDocumentModel(folder.getPathAsString(), "file2", "File");
        blob = Blobs.createBlob("Another blob content");
        blob.setFilename("test_content_ok2.doc");
        doc.setProperty("file", "content", blob);
        doc = session.createDocument(doc);

        doc.setPropertyValue("dc:title", "new title");
        // create versions
        session.checkIn(doc.getRef(), VersioningOption.MINOR, "testing version");
        session.checkOut(doc.getRef());
        session.checkIn(doc.getRef(), VersioningOption.MINOR, "another version");

        doc = session.createDocumentModel(folder.getPathAsString(), "file-missing-length", "File");
        blob = new MissingLengthBlob("A blob with a missing length");
        blob.setFilename("test_file_missing_length.doc");
        doc.setProperty("file", "content", blob);
        doc = session.createDocument(doc);

        // create proxy
        DocumentModel folder2 = session.createDocumentModel("/", "folder", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel proxy = session.createProxy(doc.getRef(), folder2.getRef());
        proxy = session.saveDocument(proxy);

        doc = session.createDocumentModel(folder.getPathAsString(), "file-zero-length", "File");
        blob = new ZeroLengthBlob("A blob with an invalid length of 0");
        blob.setFilename("test_file_missing_length.doc");
        doc.setProperty("file", "content", blob);
        doc = session.createDocument(doc);

        doc = session.createDocumentModel(folder.getPathAsString(), "file-without-blob", "File");
        doc = session.createDocument(doc);
    }


}
