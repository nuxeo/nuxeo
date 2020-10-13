/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.threed.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_FACET;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_SCHEMA;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_TYPE;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.threed.core")
@Deploy("org.nuxeo.ecm.platform.threed.api")
public class Test3DCore {

    protected static final String TEST_ZIP3D_FILE_NAME = "dummy3ds.zip";

    protected static final String[] TEST_FILE_NAMES = { "suzanne.dae", "suzanne.x3d", TEST_ZIP3D_FILE_NAME };

    @Inject
    private CoreSession session;

    @Inject
    private FileManager fileManager;

    @Test
    public void testThreeDType() {
        DocumentType threeDType = session.getDocumentType(THREED_TYPE);
        assertNotNull(threeDType);
        DocumentModel documentModel = session.createDocumentModel("/", "doc", THREED_TYPE);
        assertTrue(documentModel.hasSchema("uid"));
        assertTrue(documentModel.hasSchema("file"));
        assertTrue(documentModel.hasSchema("common"));
        assertTrue(documentModel.hasSchema("files"));
        assertTrue(documentModel.hasSchema("dublincore"));
        assertTrue(documentModel.hasSchema(THREED_SCHEMA));
        assertTrue(documentModel.hasFacet("Versionable"));
        assertTrue(documentModel.hasFacet("Publishable"));
        assertTrue(documentModel.hasFacet("Commentable"));
        assertTrue(documentModel.hasFacet(THREED_FACET));
    }

    @Test
    public void testThreeDImporter() throws IOException {
        for (String testFileName : TEST_FILE_NAMES) {
            try (InputStream is = Test3DCore.class.getResourceAsStream("/test-data/" + testFileName)) {
                assertNotNull("Failed to load resource: test-data/" + testFileName, is);
                Blob blob = Blobs.createBlob(is);
                blob.setFilename(testFileName);
                FileImporterContext context = FileImporterContext.builder(session, blob, "/").overwrite(true).build();
                DocumentModel doc = fileManager.createOrUpdateDocument(context);
                assertNotNull(doc);
                assertEquals(THREED_TYPE, doc.getType());
                assertTrue(doc.hasFacet(THREED_FACET));
                if (!TEST_ZIP3D_FILE_NAME.equals(testFileName)) {
                    assertEquals(blob.getFilename(), doc.getName());
                }
                assertEquals(blob, doc.getPropertyValue("file:content"));
            }
        }
    }
}
