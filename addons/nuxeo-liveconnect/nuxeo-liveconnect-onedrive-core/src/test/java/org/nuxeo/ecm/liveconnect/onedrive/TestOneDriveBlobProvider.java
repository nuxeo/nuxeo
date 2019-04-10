/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.onedrive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@Deploy("org.nuxeo.ecm.liveconnect.onedrive.core.tests:OSGI-INF/test-config.xml")
public class TestOneDriveBlobProvider extends OneDriveTestCase {

    @Inject
    private RuntimeHarness harness;

    @Inject
    private BlobManager blobManager;

    private OneDriveBlobProvider blobProvider;

    @Before
    public void before() {
        blobProvider = spy((OneDriveBlobProvider) blobManager.getBlobProvider(SERVICE_ID));
        assertNotNull(blobProvider);
    }

    @Test
    public void testGetURIWithEmbed() throws Exception {
        SimpleManagedBlob blob = createBlob();
        URI uri = blobProvider.getURI(blob, UsageHint.EMBED, null);
        assertNull(uri);
    }

    @Test
    public void testGetURIWithStream() throws Exception {
        SimpleManagedBlob blob = createBlob();
        URI uri = blobProvider.getURI(blob, UsageHint.STREAM, null);
        assertNotNull(uri);
        assertEquals(
                "https://nuxeofr-my.sharepoint.com/personal/kleturc_nuxeofr_onmicrosoft_com/_layouts/15/download.aspx?userid=3&authurl=True&NeverAuth=True",
                uri.toString());
    }

    @Test
    public void testGetURIWithDownload() throws Exception {
        SimpleManagedBlob blob = createBlob();
        URI uri = blobProvider.getURI(blob, UsageHint.DOWNLOAD, null);
        assertNotNull(uri);
        assertEquals(
                "https://nuxeofr-my.sharepoint.com/personal/kleturc_nuxeofr_onmicrosoft_com/_layouts/15/download.aspx?userid=3&authurl=True&NeverAuth=True",
                uri.toString());
    }

    @Test
    public void testGetURIWithView() throws Exception {
        SimpleManagedBlob blob = createBlob();
        URI uri = blobProvider.getURI(blob, UsageHint.VIEW, null);
        assertNotNull(uri);
        assertEquals(
                "https://onedrive.live.com/redir?resid=5D33DD65C6932946!70859&authkey=!AL7N1QAfSWcjNU8&ithint=folder%2cgif",
                uri.toString());
    }

    @Test
    public void testGetURIWithEdit() throws Exception {
        SimpleManagedBlob blob = createBlob();
        URI uri = blobProvider.getURI(blob, UsageHint.EDIT, null);
        assertNotNull(uri);
        assertEquals(
                "https://onedrive.live.com/redir?resid=5D33DD65C6932946!70859&authkey=!AL7N1QAfSWcjNU8&ithint=folder%2cgif",
                uri.toString());
    }

    @Test
    public void testGetAppLinksWithNonOfficeDocument() throws Exception {
        SimpleManagedBlob blob = createBlob();
        blob.setFilename("example.txt");
        blob.setMimeType("text/plain");
        List<AppLink> appLinks = blobProvider.getAppLinks(USERID, blob);
        assertNotNull(appLinks);
        assertTrue(appLinks.isEmpty());
    }

    @Test
    public void testGetAppLinksWithDocumentWithRevision() throws Exception {
        // Non office document with revision
        SimpleManagedBlob blob = createBlob("revisionId");
        blob.setFilename("example.txt");
        blob.setMimeType("text/plain");
        List<AppLink> appLinks = blobProvider.getAppLinks(USERID, blob);
        assertNotNull(appLinks);
        assertTrue(appLinks.isEmpty());

        // Office document with revision
        blob = createBlob("revisionId");
        blob.setFilename("example.docx");
        blob.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        appLinks = blobProvider.getAppLinks(USERID, blob);
        assertNotNull(appLinks);
        assertTrue(appLinks.isEmpty());
    }

    @Test
    public void testGetAppLinksWithOfficeDocument() throws Exception {
        SimpleManagedBlob blob = createBlob();
        blob.setFilename("example.docx");
        blob.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        List<AppLink> appLinks = blobProvider.getAppLinks(USERID, blob);
        assertNotNull(appLinks);
        assertEquals(1, appLinks.size());
        AppLink appLink = appLinks.get(0);
        assertNotNull(appLink.getLink());
        assertEquals("Microsoft OneDrive", appLink.getAppName());
        assertEquals("icons/OneDrive.png", appLink.getIcon());
    }

}
