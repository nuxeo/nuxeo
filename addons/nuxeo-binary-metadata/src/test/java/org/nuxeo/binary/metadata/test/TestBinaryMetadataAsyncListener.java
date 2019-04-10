/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@LocalDeploy({ "org.nuxeo.binary.metadata:binary-metadata-contrib-async-test.xml",
        "org.nuxeo.binary.metadata:binary-metadata-contrib-pdf-test.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
public class TestBinaryMetadataAsyncListener {

    @Inject
    CoreSession session;

    @Inject
    EventService eventService;

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

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        eventService.waitForAsyncCompletion();

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
