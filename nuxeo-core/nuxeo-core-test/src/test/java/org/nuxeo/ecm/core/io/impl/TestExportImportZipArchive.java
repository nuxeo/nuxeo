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
import java.io.IOException;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.function.ThrowableFunction;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestExportImportZipArchive {

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected TransactionalFeature txFeature;
    @Inject
    protected CoreSession session;

    DocumentModel rootDocument;

    DocumentModel workspace;

    protected static final String XML_DATA = "\n    <nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\">\n<nxdt:field name=\"htmlContent\" type=\"content\" source=\"htmlPreview\"></nxdt:field>\n   </nxdt:templateParams>\n    ";

    @Before
    public void createDocs() throws Exception {
        // deploy manually the contribution because we can't undeploy a contribution deployed with annotation
        deployer.deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/import-docTypes-facet.xml");

        rootDocument = session.getRootDocument();
        workspace = session.createDocumentModel(rootDocument.getPathAsString(), "ws1", "Workspace");
        workspace.setProperty("dublincore", "title", "test WS");
        workspace = session.createDocument(workspace);

        DocumentModel doc = session.createDocumentModel(workspace.getPathAsString(), "file1", "File");
        doc.setProperty("dublincore", "title", "MyDoc1");
        doc.setProperty("dublincore", "description", XML_DATA);

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        doc.setProperty("file", "content", blob);

        doc = session.createDocument(doc);

        doc.addFacet("HiddenInNavigation");
        doc.addFacet("Invoice");
        doc = session.saveDocument(doc);

        doc = session.createDocumentModel(workspace.getPathAsString(), "file2", "File");
        doc.setProperty("dublincore", "title", "MyDoc2");

        blob = Blobs.createBlob("AnotherDummyContent");
        blob.setFilename("anotherDummyBlob.txt");
        doc.setProperty("file", "content", blob);

        doc = session.createDocument(doc);

        session.save();
    }

    @Test
    public void testExportAsZipAndReimport() throws Exception {
        doTestExportAsZipAndReimport(NuxeoArchiveReader::new);
    }

    @Test
    public void testExportAsZipAndReimportStreaming() throws Exception {
        doTestExportAsZipAndReimport(file -> new NuxeoArchiveReader(new FileInputStream(file)));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-archive-exporter-extra-disable.xml")
    public void testExportAsZipAndReimportStreamingWithoutExtraField() throws Exception {
        doTestExportAsZipAndReimport(file -> new NuxeoArchiveReader(new FileInputStream(file)));
    }

    protected void doTestExportAsZipAndReimport(ThrowableFunction<File, NuxeoArchiveReader, IOException> archiveReader)
            throws Exception {
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
        assertEquals(3, nbDocs);
        assertEquals(2, nbBlobs);

        // now wipe DB
        txFeature.nextTransaction();
        session.removeDocument(workspace.getRef());
        session.save();
        assertEquals(0, session.getChildren(session.getRootDocument().getRef()).size());

        // NXP-14218: do not fail if a facet becomes unknown
        deployer.undeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/import-docTypes-facet.xml");

        // reimport
        reader = archiveReader.apply(archive);
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
        children.sort(Comparator.comparing(doc -> (String) doc.getPropertyValue("dc:title")));
        // assert MyDoc1
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
        // check attributes
        assertEquals("MyDoc1", importedDocument.getPropertyValue("dc:title"));
        assertEquals(XML_DATA, importedDocument.getPropertyValue("dc:description"));

        // check that facets have been reimported
        assertTrue(importedDocument.hasFacet("HiddenInNavigation"));
        assertFalse(importedDocument.hasFacet("Invoice"));

        // assert MyDoc2
        importedDocument = children.get(1);
        blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("anotherDummyBlob.txt", blob.getFilename());
        assertEquals("MyDoc2", importedDocument.getPropertyValue("dc:title"));
    }

}
