/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.schema.FacetDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/import-docTypes.xml")
public class TestExportImportZipArchive {

    @Inject
    protected CoreSession session;

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    protected static final String XML_DATA = "\n    <nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\">\n<nxdt:field name=\"htmlContent\" type=\"content\" source=\"htmlPreview\"></nxdt:field>\n   </nxdt:templateParams>\n    ";

    private void createDocs() throws Exception {
        rootDocument = session.getRootDocument();
        workspace = session.createDocumentModel(rootDocument.getPathAsString(), "ws1", "Workspace");
        workspace.setProperty("dublincore", "title", "test WS");
        workspace = session.createDocument(workspace);

        docToExport = session.createDocumentModel(workspace.getPathAsString(), "file", "File");
        docToExport.setProperty("dublincore", "title", "MyDoc");

        docToExport.setProperty("dublincore", "description", XML_DATA);

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        docToExport.setProperty("file", "content", blob);

        docToExport = session.createDocument(docToExport);

        docToExport.addFacet("HiddenInNavigation");
        docToExport.addFacet("Invoice");
        docToExport = session.saveDocument(docToExport);

        session.save();
    }

    @Test
    public void testExportAsZipAndReimport() throws Exception {
        createDocs();

        File archive = Framework.createTempFile("core-io-archive", "zip");

        DocumentReader reader = new DocumentTreeReader(session, workspace);
        DocumentWriter writer = new NuxeoArchiveWriter(archive);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        assertTrue(archive.exists());
        assertTrue(archive.length() > 0);

        writer.close();
        reader.close();

        // check the zip contents
        ZipInputStream zin = new ZipInputStream(new FileInputStream(archive));
        ZipEntry entry = zin.getNextEntry();
        int nbDocs = 0;
        int nbBlobs = 0;
        while (entry != null) {
            if (entry.getName().endsWith(ExportConstants.DOCUMENT_FILE)) {
                nbDocs++;
            } else if (entry.getName().endsWith(".blob")) {
                nbBlobs++;
            }
            entry = zin.getNextEntry();
        }
        assertEquals(2, nbDocs);
        assertEquals(1, nbBlobs);

        // now wipe DB
        Framework.getService(EventService.class).waitForAsyncCompletion();
        session.removeDocument(workspace.getRef());
        session.save();
        assertEquals(0, session.getChildren(session.getRootDocument().getRef()).size());

        //NXP-14218: do not fail if a facet becomes unknown
        SchemaManagerImpl schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);
        FacetDescriptor fd = schemaManager.getFacetDescriptor("Invoice");
        schemaManager.unregisterFacet(fd);
        // Recompute available facets
        schemaManager.recomputeDynamicFacets();

        // reimport
        reader = new NuxeoArchiveReader(archive);
        writer = new DocumentModelWriter(session, "/");

        pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        archive.delete();

        // check result
        DocumentModelList children = session.getChildren(session.getRootDocument().getRef());
        assertEquals(1, children.size());
        DocumentModel importedWS = children.get(0);
        assertEquals(workspace.getTitle(), importedWS.getTitle());

        children = session.getChildren(importedWS.getRef());
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
        // check attributes
        assertEquals("MyDoc", importedDocument.getPropertyValue("dc:title"));
        assertEquals(XML_DATA, importedDocument.getPropertyValue("dc:description"));

        // check that facets have been reimported
        assertTrue(importedDocument.hasFacet("HiddenInNavigation"));
        assertFalse(importedDocument.hasFacet("Invoice"));
    }

}
