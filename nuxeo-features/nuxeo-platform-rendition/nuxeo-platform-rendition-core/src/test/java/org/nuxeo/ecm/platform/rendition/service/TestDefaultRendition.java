/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.platform.collections.core")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestDefaultRendition {

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void testDefaultRenditionOnContainer() throws Exception {
        DocumentModel folder01 = session.createDocumentModel("/", "dummy-folder", "Folder");
        folder01 = session.createDocument(folder01);
        TestRenditionProvider.createBlobDoc(folder01.getPathAsString(), "dummy-file01", "dummy-file01.txt", "File",
                session);
        TestRenditionProvider.createBlobDoc(folder01.getPathAsString(), "dummy-file02", "dummy-file02.txt", "File",
                session);
        DocumentModel collection = session.createDocumentModel(folder01.getPathAsString(), "dummy-collection",
                "Collection");
        collection = session.createDocument(collection);
        DocumentModel folder11 = session.createDocumentModel("/", "dummy-folder", "Folder");
        folder11 = session.createDocument(folder01);
        DocumentModel file11 = TestRenditionProvider.createBlobDoc("/", "dummy-file11", "dummy-file11.txt", "File",
                session);
        DocumentModel file12 = TestRenditionProvider.createBlobDoc("/", "dummy-file12", "dummy-file12.txt", "File",
                session);
        CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        collectionManager.addToCollection(collection, folder11, session);
        collectionManager.addToCollection(collection, file11, session);
        collectionManager.addToCollection(collection, file12, session);

        Rendition ren = renditionService.getDefaultRendition(folder01, "download", null);
        assertNotNull(ren);
        Blob resultBlob = ren.getBlob();
        assertEquals("application/zip", resultBlob.getMimeType());
        try (CloseableFile source = resultBlob.getCloseableFile()) {
            try (ZipFile zip = new ZipFile(source.getFile())) {
                assertEquals(3, zip.size());
                ZipEntry z1 = zip.getEntry("dummy-collection.zip");
                assertNotNull(z1);
                try (ZipInputStream zis = new ZipInputStream(zip.getInputStream(z1))) {
                    ZipEntry entry;
                    int i = 0;
                    while ((entry = zis.getNextEntry()) != null) {
                        i++;
                        assertEquals("dummy-file1" + i + ".txt", entry.getName());
                    }
                    assertEquals(2, i);
                }

            }
        }
    }

    @Test
    public void testDefaultRendition() throws Exception {
        DocumentModel file = TestRenditionProvider.createBlobDoc("File", session);
        Renderable renderable = file.getAdapter(Renderable.class);
        assertNotNull(renderable);

        Rendition ren = renditionService.getDefaultRendition(file, "download", null);
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals("text/plain", blob.getMimeType());
    }

}
