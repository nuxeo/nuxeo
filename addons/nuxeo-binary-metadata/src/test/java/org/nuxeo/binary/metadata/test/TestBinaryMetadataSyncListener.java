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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.FilterOn;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features({ BinaryMetadataFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-test.xml")
@Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-pdf-test.xml")
@Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-provider.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBinaryMetadataSyncListener extends BaseBinaryMetadataTest {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected LogCaptureFeature.Result logResult;

    @Test
    public void testListenerCreationRulesSync() throws Exception {
        testListener(true);
    }

    @Test
    public void testListenerUpdateRulesSync() throws Exception {
        testListener(false);
    }

    @Test
    @Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-async-test.xml")
    public void testListenerCreationRulesAsync() throws Exception {
        testListener(true);
    }

    @Test
    @Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-async-test.xml")
    public void testListenerUpdateRulesAsync() throws Exception {
        testListener(false);
    }

    protected void testListener(boolean attachOnCreation) throws Exception {
        // Test the following rule: 'If the attached binary is dirty and the document metadata are not dirty, the
        // listener reads the metadata from attached binary to document.'
        DocumentModel pdfDoc = createDocumentWithPDFBlob(attachOnCreation);

        // Assert blob properties
        Blob blob = (Blob) pdfDoc.getPropertyValue("file:content");
        Map<String, Object> blobProperties = binaryMetadataService.readMetadata(blob, true);
        assertNotNull(blobProperties);
        assertEquals("en-US", blobProperties.get("Language").toString());
        assertEquals("OpenOffice.org 3.2", blobProperties.get("Producer").toString());
        assertEquals(Arrays.asList("tag1", "tag2"), blobProperties.get("Keywords"));
        assertEquals("Mirko Nasato", blobProperties.get("Author").toString());
        assertEquals("No", blobProperties.get("Linearized").toString());
        assertEquals("Writer", blobProperties.get("Creator").toString());

        // Assert document properties
        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:source"));
        assertEquals("Writer", pdfDoc.getPropertyValue("dc:coverage"));
        assertEquals("Mirko Nasato", pdfDoc.getPropertyValue("dc:creator"));
        // Test if description has been overridden by higher order contribution
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:description"));

        // Test the following rule: 'If the attached binary is dirty and the document metadata are dirty, the listener
        // writes the metadata from the document to the attached binary.'
        File binary = FileUtils.getResourceFileFromContext("data/hello.pdf");
        Blob fb = Blobs.createBlob(binary, "application/pdf");
        pdfDoc.setPropertyValue("dc:description", "descriptionNotFromBlob");
        DocumentHelper.addBlob(pdfDoc.getProperty("file:content"), fb);
        session.saveDocument(pdfDoc);
        pdfDoc = session.getDocument(pdfDoc.getRef());

        txFeature.nextTransaction();

        // Assert blob properties
        blob = (Blob) pdfDoc.getPropertyValue("file:content");
        blobProperties = binaryMetadataService.readMetadata(blob, true);
        assertNotNull(blobProperties);
        assertEquals("descriptionNotFromBlob", blobProperties.get("Producer").toString());

        // Assert document properties
        assertEquals("descriptionNotFromBlob", pdfDoc.getPropertyValue("dc:description"));

        // Test the following rule: 'If the attached binary is not dirty and the document metadata are dirty, the
        // listener writes the metadata from the document to the attached binary.'
        pdfDoc.setPropertyValue("dc:description", "descriptionNotFromBlob-2");
        session.saveDocument(pdfDoc);
        pdfDoc = session.getDocument(pdfDoc.getRef());

        txFeature.nextTransaction();

        // Assert blob properties
        blob = (Blob) pdfDoc.getPropertyValue("file:content");
        blobProperties = binaryMetadataService.readMetadata(blob, true);
        assertNotNull(blobProperties);
        assertEquals("descriptionNotFromBlob-2", blobProperties.get("Producer").toString());

        // Assert document properties
        assertEquals("descriptionNotFromBlob-2", pdfDoc.getPropertyValue("dc:description"));
    }

    @Test
    @Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-sync-async-test.xml")
    @Deploy("org.nuxeo.binary.metadata:disable-binary-metadata-work-test.xml")
    public void testListenerCreationRulesSyncAsync() throws Exception {
        testListenerRulesSyncAsync(true);
    }

    @Test
    @Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-sync-async-test.xml")
    @Deploy("org.nuxeo.binary.metadata:disable-binary-metadata-work-test.xml")
    public void testListenerUpdateRulesSyncAsync() throws Exception {
        testListenerRulesSyncAsync(false);
    }

    public void testListenerRulesSyncAsync(boolean attachOnCreation) throws Exception {
        DocumentModel pdfDoc = createDocumentWithPDFBlob(attachOnCreation);

        // Test sync metadata have been update
        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:source"));

        // Test async metadata have not been updated as the work is disabled
        assertNull(pdfDoc.getPropertyValue("dc:coverage"));
        assertNull(pdfDoc.getPropertyValue("dc:creator"));
        assertNull(pdfDoc.getPropertyValue("dc:description"));
    }

    protected DocumentModel createDocumentWithPDFBlob(boolean attachOnCreation) throws IOException {
        // Create folder
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "Folder");
        session.createDocument(doc);

        // Create file
        doc = session.createDocumentModel("/folder", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        if (!attachOnCreation) {
            doc = session.createDocument(doc);
        }

        // Attach PDF
        File binary = FileUtils.getResourceFileFromContext("data/hello.pdf");
        Blob fb = Blobs.createBlob(binary, "application/pdf");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        if (attachOnCreation) {
            doc = session.createDocument(doc);
        } else {
            doc = session.saveDocument(doc);
        }

        txFeature.nextTransaction();

        return session.getDocument(doc.getRef());
    }

    @Test
    public void testCollaborativeSaveOnlyIncrementsVersionOnce() throws Exception {
        // create file with one contributor
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:lastContributor", "laurel");
        doc = session.createDocument(doc);

        // attach PDF with another contributor
        File file = FileUtils.getResourceFileFromContext("data/hello.pdf");
        Blob blob = Blobs.createBlob(file, "application/pdf");
        DocumentHelper.addBlob(doc.getProperty("file:content"), blob);
        doc.setPropertyValue("dc:lastContributor", "hardy");
        doc = session.saveDocument(doc);

        // version incremented only once by the collaborative-save versioning policy
        assertEquals("0.1+", doc.getVersionLabel());
    }

    @Test
    public void testEXIFandIPTC() throws IOException {
        // Create folder
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "Folder");
        session.createDocument(doc);

        // Create first picture
        doc = session.createDocumentModel("/folder", "picture", "Picture");
        doc.setPropertyValue("dc:title", "picture");
        doc = session.createDocument(doc);

        // Attach EXIF sample
        File binary = FileUtils.getResourceFileFromContext("data/china.jpg");
        Blob fb = Blobs.createBlob(binary, "image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        session.saveDocument(doc);

        // Verify
        DocumentModel picture = session.getDocument(doc.getRef());
        assertEquals("Horizontal (normal)", picture.getPropertyValue("imd:orientation"));
        assertEquals(2.4, picture.getPropertyValue("imd:fnumber"));

        // Create second picture
        doc = session.createDocumentModel("/folder", "picture1", "Picture");
        doc.setPropertyValue("dc:title", "picture");
        doc = session.createDocument(doc);

        // Attach IPTC sample
        binary = FileUtils.getResourceFileFromContext("data/iptc_sample.jpg");
        fb = Blobs.createBlob(binary, "image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        session.saveDocument(doc);

        // Verify
        picture = session.getDocument(doc.getRef());
        assertEquals("DDP", picture.getPropertyValue("dc:source"));
        assertEquals("ImageForum", picture.getPropertyValue("dc:rights"));
        assertEquals("Music", picture.getPropertyValue("dc:description").toString().substring(0, 5));

    }

    @Test
    public void testUpdateOnBlobsFromProvidersWhichPreventUserUpdate() {
        // Create a folder
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "Folder");
        session.createDocument(doc);

        // Create a doc
        doc = session.createDocumentModel("/folder", "picture", "Picture");
        doc.setPropertyValue("dc:title", "a picture file");
        doc = session.createDocument(doc);

        // Attach blob from provider
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = "testProvider:user@testProvider.com:0134cc5";
        blobInfo.digest = "5cc31b4305e2beb7191f717448";
        blobInfo.filename = "iptc_sample.jpg";
        blobInfo.mimeType = "image/jpeg";
        Blob blob = new SimpleManagedBlob(blobInfo);
        DocumentHelper.addBlob(doc.getProperty("file:content"), blob);
        session.save();

        // update metadata and check if the blob is still a blob from the provider
        doc.setPropertyValue("imd:user_comment", "a comment");
        session.saveDocument(doc);
        Blob anotherBlob = (Blob) doc.getProperty("file:content").getValue();
        // assert the blob was not modified even if the metadata was updated
        assertEquals(blob, anotherBlob);
    }

    // NXP-30858
    @Test
    @Deploy("org.nuxeo.binary.metadata:binary-metadata-contrib-async-test.xml")
    @FilterOn(loggerClass = AbstractWork.class, logLevel = "ERROR")
    public void testAsyncBinaryMetadataAreNotRunOnVersion() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        File binary = FileUtils.getResourceFileFromContext("data/hello.pdf");
        Blob fb = Blobs.createBlob(binary, "application/pdf");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        doc = session.createDocument(doc);

        // assert previous state
        assertTrue(logResult.getCaughtEventMessages().isEmpty());

        session.checkIn(doc.getRef(), VersioningOption.MINOR, "0.1");
        txFeature.nextTransaction();

        // assert no work has failed
        assertTrue(logResult.getCaughtEventMessages().isEmpty());
    }
}
