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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.liveconnect.google.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.liveconnect.google.drive.GoogleDriveBlobProvider.PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.blob.apps.LinkedAppsProvider;

public class TestGoogleDriveBlobProvider extends GoogleDriveTestCase {

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected CoreSession session;

    protected BlobInfo newBlobInfo() {
        BlobInfo blobInfo = new BlobInfo();
        // we need a digest otherwise async convert jobs fail to compute a cache key when there's no actual stream
        // linked to the blob (native Google documents)
        blobInfo.digest = UUID.randomUUID().toString();
        return blobInfo;
    }

    @Test
    public void testStreamUploaded() throws Exception {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + JPEG_FILEID;
        Blob blob = new SimpleManagedBlob(blobInfo);
        try (InputStream is = blobManager.getStream(blob)) {
            assertNotNull(is);
            byte[] bytes = IOUtils.toByteArray(is);
            assertEquals(JPEG_SIZE, bytes.length);
        }
    }

    @Test
    public void testStreamRevisionUploaded() throws Exception {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + JPEG_FILEID + ":" + JPEG_REVID;
        Blob blob = new SimpleManagedBlob(blobInfo);
        try (InputStream is = blobManager.getStream(blob)) {
            assertNotNull(is);
            byte[] bytes = IOUtils.toByteArray(is);
            assertEquals(JPEG_REV_SIZE, bytes.length);
        }
    }

    @Test
    public void testStreamNative() throws Exception {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + GOOGLEDOC_FILEID;
        Blob blob = new SimpleManagedBlob(blobInfo);
        try (InputStream is = blobManager.getStream(blob)) {
            assertNull(is);
        }
    }

    @Test
    public void testStreamRevisionNative() throws Exception {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + GOOGLEDOC_FILEID + ":" + GOOGLEDOC_REVID;
        Blob blob = new SimpleManagedBlob(blobInfo);
        try (InputStream is = blobManager.getStream(blob)) {
            assertNull(is);
        }
    }

    @Test
    public void testExportedLinksUploaded() throws IOException {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + JPEG_FILEID;
        ManagedBlob blob = new SimpleManagedBlob(blobInfo);
        Map<String, URI> map = blobManager.getAvailableConversions(blob, null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testExportedLinksNative() throws IOException {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + GOOGLEDOC_FILEID;
        ManagedBlob blob = new SimpleManagedBlob(blobInfo);
        Map<String, URI> map = blobManager.getAvailableConversions(blob, null);
        assertTrue(map.containsKey("application/pdf"));
    }

    @Test
    public void testAppLinks() throws IOException {
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + JPEG_FILEID;
        ManagedBlob blob = new SimpleManagedBlob(blobInfo);

        LinkedAppsProvider provider = (LinkedAppsProvider) blobManager.getBlobProvider(GoogleDriveBlobProvider.PREFIX);
        List<AppLink> appLinks = provider.getAppLinks(USERNAME, blob);

        assertEquals(2, appLinks.size());

        AppLink app = appLinks.get(0);
        assertEquals("App #1", app.getAppName());
        assertEquals("editor_16.png", app.getIcon());

        assertEquals("App #2", appLinks.get(1).getAppName());
    }

    @Test
    public void testSaveBlob() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");

        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + JPEG_FILEID;
        Blob blob = new SimpleManagedBlob(blobInfo);
        doc.setPropertyValue("file:content", (Serializable) blob);

        session.createDocument(doc);
        session.save();

        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");

        assertTrue(blob instanceof ManagedBlob);
        ManagedBlob mb = (ManagedBlob) blob;
        assertEquals(PREFIX, mb.getProviderId());
        assertEquals(PREFIX + ":" + USERID + ":" + JPEG_FILEID, mb.getKey());
    }

    @Test
    public void testBlobCheckInUploaded() throws Exception {
        testBlobCheckIn(JPEG_FILEID, JPEG_REVID);
    }

    @Test
    public void testBlobCheckInNative() throws Exception {
        DocumentModel version = testBlobCheckIn(GOOGLEDOC_FILEID, GOOGLEDOC_REVID);
        assertTrue(version.hasFacet(GoogleDriveBlobProvider.BLOB_CONVERSIONS_FACET));

        Blob blob = (Blob) version.getPropertyValue("file:content");

        // native files do not support downloading revision conversions
        assertTrue(blobManager.getAvailableConversions(blob, BlobManager.UsageHint.STREAM).isEmpty());

        // still we can get our stored conversion
        assertNotNull(blobManager.getConvertedStream(blob, GoogleDriveBlobProvider.DEFAULT_EXPORT_MIMETYPE, version));

    }

    protected DocumentModel testBlobCheckIn(String fileId, String revisionId) {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        BlobInfo blobInfo = newBlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + fileId;
        Blob blob = new SimpleManagedBlob(blobInfo);
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
        session.save();

        // now check in
        DocumentRef verRef = session.checkIn(doc.getRef(), VersioningOption.MAJOR, null);
        DocumentModel version = session.getDocument(verRef);

        Blob verBlob = (Blob) version.getPropertyValue("file:content");
        assertTrue(verBlob instanceof ManagedBlob);
        ManagedBlob mvb = (ManagedBlob) verBlob;
        assertEquals(PREFIX, mvb.getProviderId());
        assertEquals(PREFIX + ":" + USERID + ":" + fileId + ":" + revisionId, mvb.getKey());

        return version;
    }

}
