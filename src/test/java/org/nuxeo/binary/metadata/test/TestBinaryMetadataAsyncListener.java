/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-async-test.xml")
@Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-pdf-test.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBinaryMetadataAsyncListener {

    @Inject
    CoreSession session;

    @Inject
    EventService eventService;

    @Inject
    TransactionalFeature txFeature;

    @Test
    public void testListener() throws Exception {
        // Create folder
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "Folder");
        session.createDocument(doc);

        // Create file
        doc = session.createDocumentModel("/folder", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        doc = session.createDocument(doc);

        // Attach PDF
        File binary = FileUtils.getResourceFileFromContext("data/hello.pdf");
        Blob fb = Blobs.createBlob(binary, "application/pdf");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        session.saveDocument(doc);

        txFeature.nextTransaction();

        DocumentModel pdfDoc = session.getDocument(doc.getRef());

        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:source"));

        // Updates done by async listener
        assertEquals("Writer", pdfDoc.getPropertyValue("dc:coverage"));
        assertEquals("Mirko Nasato", pdfDoc.getPropertyValue("dc:creator"));

        // Test if description has been overriden by higher order contribution
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:description"));
    }

}
