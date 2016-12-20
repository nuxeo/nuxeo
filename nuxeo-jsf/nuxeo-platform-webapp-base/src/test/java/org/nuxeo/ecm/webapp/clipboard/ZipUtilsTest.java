/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.webapp.clipboard;

import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class ZipUtilsTest {

    @Inject
    protected CoreSession session;

    protected DocumentModel createTestFolder() {
        DocumentModel parent = new DocumentModelImpl("/", "parent", "Folder");
        parent.setPropertyValue("dc:title", "Parent");
        parent = session.createDocument(parent);

        DocumentModel file = new DocumentModelImpl("/parent", "éèà", "File");
        file.setPropertyValue("dc:title", "éèà");

        Blob blob = Blobs.createBlob("ééà");
        blob.setFilename("éèà");
        file.setPropertyValue("file:content", (Serializable) blob);
        file.setPropertyValue("dc:title", "éèà");
        file = session.createDocument(file);
        return parent;
    }

    protected DocumentModel createHeavyFile() {
        DocumentModel heavyFile = session.createDocumentModel("/", "heavyFile", "File");
        heavyFile.setPropertyValue("dc:title", "Heavy File");

        Blob blob1 = Blobs.createBlob("abc");
        blob1.setFilename("blob1.raw");
        heavyFile.setPropertyValue("file:content", (Serializable) blob1);

        Blob blob2 = Blobs.createBlob("123");
        blob2.setFilename("blob2.raw");

        HashMap<String, Serializable> blob = new HashMap<String, Serializable>();
        blob.put("file", (Serializable) blob2);

        ArrayList<HashMap<String, Serializable>> blobs = new ArrayList<HashMap<String, Serializable>>();
        blobs.add(blob);

        heavyFile.setPropertyValue("files:files", blobs);
        heavyFile = session.createDocument(heavyFile);

        return heavyFile;
    }

    @Test
    public void testExportSimpleFile() throws Exception {
        DocumentModel folder = createTestFolder();
        List<DocumentModel> documents = new ArrayList<DocumentModel>();
        documents.add(folder);

        DocumentListZipExporter zipExporter = new DocumentListZipExporter();
        Blob blob = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(blob);
        try (ZipFile zipFile = new ZipFile(blob.getFile())) {
            assertNotNull(zipFile.getEntry("Parent/éèà"));
            Framework.getProperties().setProperty(DocumentListZipExporter.ZIP_ENTRY_ENCODING_PROPERTY,
                    DocumentListZipExporter.ZIP_ENTRY_ENCODING_OPTIONS.ascii.name());
            blob = zipExporter.exportWorklistAsZip(documents, session, true);
            assertNotNull(blob);
        }
        try (ZipFile zipFile = new ZipFile(blob.getFile())) {
            assertNotNull(zipFile.getEntry("Parent/eea"));
        }
    }

    @Test
    public void testExportAllBlobs() throws Exception {
        DocumentModel heavyFile = createHeavyFile();
        List<DocumentModel> documents = new ArrayList<DocumentModel>();
        documents.add(heavyFile);

        DocumentListZipExporter zipExporter = new DocumentListZipExporter();
        Blob blob = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(blob);
        try (ZipFile zipFile = new ZipFile(blob.getFile())) {
            assertNotNull(zipFile.getEntry("blob2.raw"));
            assertNotNull(zipFile.getEntry("blob1.raw"));
        }
    }
}
