/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.webapp.clipboard;


import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class ZipUtilsTest {

    @Inject
    protected CoreSession session;

    protected DocumentModel createTestFolder() throws ClientException {
        DocumentModel parent = new DocumentModelImpl("/", "parent", "Folder");
        parent.setPropertyValue("dc:title", "Parent");
        parent = session.createDocument(parent);

        DocumentModel file = new DocumentModelImpl("/parent", "éèà", "File");
        file.setPropertyValue("dc:title", "éèà");

        StringBlob blob = new StringBlob("ééà");
        blob.setFilename("éèà");
        file.setPropertyValue("file:content", blob);
        file.setPropertyValue("dc:title", "éèà");
        file = session.createDocument(file);
        return parent;
    }

    protected DocumentModel createHeavyFile() throws ClientException {
        DocumentModel heavyFile = session.createDocumentModel("/", "heavyFile",
                "File");
        heavyFile.setPropertyValue("dc:title", "Heavy File");

        StringBlob blob1 = new StringBlob("abc");
        blob1.setFilename("blob1.raw");
        heavyFile.setPropertyValue("file:content", blob1);

        StringBlob blob2 = new StringBlob("123");
        blob2.setFilename("blob2.raw");

        HashMap<String, Serializable> blob = new HashMap<String, Serializable>();
        blob.put("file", (Serializable) blob2);
        blob.put("filename", blob2.getFilename());

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
        File file = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(file);
        ZipFile zipFile = new ZipFile(file);
        assertNotNull(zipFile.getEntry("Parent/éèà"));
        Framework.getProperties().setProperty(
                DocumentListZipExporter.ZIP_ENTRY_ENCODING_PROPERTY,
                DocumentListZipExporter.ZIP_ENTRY_ENCODING_OPTIONS.ascii.name());
        file = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(file);
        zipFile = new ZipFile(file);
        assertNotNull(zipFile.getEntry("Parent/eea"));
    }

    @Test
    public void testExportAllBlobs() throws Exception {
        DocumentModel heavyFile = createHeavyFile();
        List<DocumentModel> documents = new ArrayList<DocumentModel>();
        documents.add(heavyFile);

        DocumentListZipExporter zipExporter = new DocumentListZipExporter();
        File file = zipExporter.exportWorklistAsZip(documents, session, true);
        assertNotNull(file);
        ZipFile zipFile = new ZipFile(file);
        assertNotNull(zipFile.getEntry("blob2.raw"));
        assertNotNull(zipFile.getEntry("blob1.raw"));
    }
}