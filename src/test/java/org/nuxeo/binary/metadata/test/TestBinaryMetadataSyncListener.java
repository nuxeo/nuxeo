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
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert", "org.nuxeo.ecm.platform.rendition.core",
        "org.nuxeo.ecm.automation.core" })
@LocalDeploy({ "org.nuxeo.binary.metadata:binary-metadata-contrib-test.xml",
        "org.nuxeo.binary.metadata:binary-metadata-contrib-pdf-test.xml",
        "org.nuxeo.binary.metadata:binary-metadata-contrib-provider.xml"})
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBinaryMetadataSyncListener {

    @Inject
    CoreSession session;

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

        DocumentModel pdfDoc = session.getDocument(doc.getRef());

        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:source"));
        assertEquals("Writer", pdfDoc.getPropertyValue("dc:coverage"));
        assertEquals("Mirko Nasato", pdfDoc.getPropertyValue("dc:creator"));

        // Test if description has been overriden by higher order contribution
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:description"));

        // Test the following rule: 'If the attached binary is dirty and the document metadata are not dirty, the
        // listener reads the metadata from attached binary to document.'

        // Changing the title to see after if the blob title is well propagated.
        pdfDoc.setPropertyValue("dc:title", "notFromBlob");
        pdfDoc.setPropertyValue("file:content", null);
        session.saveDocument(pdfDoc);

        pdfDoc = session.getDocument(pdfDoc.getRef());

        assertEquals("notFromBlob", pdfDoc.getPropertyValue("dc:title"));

        // Updating only the blob and simulate the same change on dc:title -> title should not be dirty.
        pdfDoc.setPropertyValue("dc:title", "notFromBlob");
        DocumentHelper.addBlob(pdfDoc.getProperty("file:content"), fb);
        session.saveDocument(pdfDoc);

        pdfDoc = session.getDocument(pdfDoc.getRef());

        // Confirm the blob was dirty but not metadata -> title should be updated properly.
        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
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
}
