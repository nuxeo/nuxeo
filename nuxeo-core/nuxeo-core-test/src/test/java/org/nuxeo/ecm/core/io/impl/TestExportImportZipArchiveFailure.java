/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.FacetDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.core.io.test:OSGI-INF/import-docTypes.xml")
public class TestExportImportZipArchiveFailure {

    protected static final String DOC_TYPE = "CustomFile";

    protected static final String FACET = "Invoice";

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected CoreSession session;

    protected DocumentModel workspace;

    protected File archive;

    @After
    public void tearDown() {
        if (archive != null) {
            archive.delete();
            archive = null;
        }
    }

    protected void createDocs(boolean addFacet) throws Exception {
        workspace = session.createDocumentModel("/", "ws", "Workspace");
        workspace.setPropertyValue("dc:title", "My Workspace");
        workspace = session.createDocument(workspace);

        DocumentModel doc = session.createDocumentModel("/ws", "file", DOC_TYPE);
        doc.setPropertyValue("dc:title", "My File");
        Blob blob = Blobs.createBlob("SomeDummyContent", "text/plain", null, "dummyBlob.txt");
        doc.setPropertyValue("file:content", (Serializable) blob);
        if (addFacet) {
            doc.addFacet(FACET);
        }
        session.createDocument(doc);
        session.save();
    }

    @Test
    public void testExportAsZipAndReimportFailureWithoutFacet() throws Exception {
        testExportAsZipAndReimportFailure(false);
    }

    @Test
    public void testExportAsZipAndReimportFailureWithFacet() throws Exception {
        testExportAsZipAndReimportFailure(true);
    }

    protected void testExportAsZipAndReimportFailure(boolean addFacet) throws Exception {
        createDocs(addFacet);

        archive = File.createTempFile("core-io-archive", "zip");

        DocumentReader reader = new DocumentTreeReader(session, workspace);
        DocumentWriter writer = new NuxeoArchiveWriter(archive);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        writer.close();
        reader.close();

        // now wipe DB
        Framework.getService(EventService.class).waitForAsyncCompletion();
        session.removeDocument(workspace.getRef());
        session.save();
        assertEquals(0, session.getChildren(session.getRootDocument().getRef()).size());

        SchemaManagerImpl sm = (SchemaManagerImpl) schemaManager;
        DocumentTypeDescriptor dtd = null;
        FacetDescriptor fd = null;
        try {
            // NXP-23035: do not fail if a type becomes unknown
            dtd = sm.getDocumentTypeDescriptor(DOC_TYPE);
            sm.unregisterDocumentType(dtd);
            if (addFacet) {
                // NXP-14218: do not fail if a facet becomes unknown
                fd = sm.getFacetDescriptor(FACET);
                sm.unregisterFacet(fd);
            }
            sm.checkDirty();

            // reimport
            reader = new NuxeoArchiveReader(archive);
            writer = new DocumentModelWriter(session, "/");

            pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);

            try {
                pipe.run();
                fail("Import should have failed due to missing document type");
            } catch (IllegalArgumentException e) {
                assertEquals(DOC_TYPE + " is not a registered core type", e.getMessage());
            }
        } finally {
            // cleanup by re-registering doc type and facet
            if (dtd != null) {
                sm.registerDocumentType(dtd);
            }
            if (fd != null) {
                sm.registerFacet(fd);
            }
            sm.checkDirty();
        }
    }

}
