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
import static org.nuxeo.ecm.liveconnect.google.drive.GoogleDriveBlobProvider.PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.blob.apps.LinkedAppsProvider;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

public class TestGoogleDriveBlobProvider extends GoogleDriveTestCase {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testReadBlobStreamUploaded() throws Exception {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + FILEID_JPEG;
        Blob blob = new SimpleManagedBlob(blobInfo);
        try (InputStream is = blobManager.getStream(blob)) {
            assertNotNull(is);
            byte[] bytes = IOUtils.toByteArray(is);
            assertEquals(SIZE, bytes.length);
        }
    }

    @Test
    public void testReadBlobStreamNative() throws Exception {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + FILEID_DOC;
        Blob blob = new SimpleManagedBlob(blobInfo);
        try (InputStream is = blobManager.getStream(blob)) {
            assertNull(is);
        }
    }

    @Test
    public void testAppLinks() throws IOException {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = PREFIX + ":" + USERID + ":" + FILEID_JPEG;
        ManagedBlob blob = new SimpleManagedBlob(blobInfo);

        LinkedAppsProvider provider = (LinkedAppsProvider) blobManager.getBlobProvider(GoogleDriveBlobProvider.PREFIX);
        List<AppLink> appLinks = provider.getAppLinks(USERNAME, blob);

        assertEquals(2, appLinks.size());

        AppLink app = appLinks.get(0);
        assertEquals("App #1", app.getAppName());
        assertEquals("editor_16.png", app.getIcon());

        assertEquals("App #2", appLinks.get(1).getAppName());
    }
}
