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
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ BlobManagerFeature.class, MockitoFeature.class })
@LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/dummy-blob-provider.xml")
public class TestBlobManager {

    private static final String DUMMY = "dummy";

    protected Mockery mockery = new JUnit4Mockery();

    @Inject
    protected BlobManager blobManager;

    @Mock
    @RuntimeService
    RepositoryManager repositoryManager;

    @Before
    public void mockRepositoryManager() throws Exception {
        when(repositoryManager.getRepositoryNames()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testDummyRegistration() throws Exception {
        BlobProvider dummyBlobProvider = blobManager.getBlobProvider(DUMMY);
        assertNotNull(dummyBlobProvider);
    }

    @Test
    public void testGetSetMetadata() throws Exception {
        // read without prefix
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = "1234";
        blobInfo.mimeType = "test/type";
        blobInfo.encoding = "UTF-8";
        blobInfo.filename = "doc.ext";
        blobInfo.length = Long.valueOf(123);
        blobInfo.digest = "55667788";
        ManagedBlob blob = (ManagedBlob) blobManager.readBlob(blobInfo, DUMMY);
        assertNotNull(blob);
        assertEquals("1234", blob.getKey());
        assertEquals("test/type", blob.getMimeType());
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals("doc.ext", blob.getFilename());
        assertEquals(123, blob.getLength());
        assertEquals("55667788", blob.getDigest());

        // read with prefix
        blobInfo.key = DUMMY + ":1234";
        blob = (ManagedBlob) blobManager.readBlob(blobInfo, null);
        assertNotNull(blob);
        assertEquals("dummy:1234", blob.getKey());
        assertEquals("test/type", blob.getMimeType());
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals("doc.ext", blob.getFilename());
        assertEquals(123, blob.getLength());
        assertEquals("55667788", blob.getDigest());

        // write
        Document doc = mockery.mock(Document.class);
        mockery.checking(new Expectations() {
            {
                allowing(doc).getRepositoryName();
                will(returnValue(DUMMY));

            }
        });
        String key = blobManager.writeBlob(blob, doc);
        assertEquals("dummy:1234", key);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/test-blob-dispatch.xml")
    public void testDispatch() throws Exception {
        // blob that's not a video gets stored on the first dummy repo
        Blob blob = Blobs.createBlob("foo", "text/plain");
        Document doc = mockery.mock(Document.class, "doc1");
        String key = blobManager.writeBlob(blob, doc);
        assertEquals("dummy:1", key);
        // videos get stored in the second one
        blob = Blobs.createBlob("bar", "video/mp4");
        key = blobManager.writeBlob(blob, doc);
        assertEquals("dummy2:1", key);

        // read first one
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = "dummy:1";
        blob = blobManager.readBlob(blobInfo, null);
        assertEquals("foo", IOUtils.toString(blob.getStream()));

        // read second one
        blobInfo.key = "dummy2:1";
        blob = blobManager.readBlob(blobInfo, null);
        assertEquals("bar", IOUtils.toString(blob.getStream()));
    }

}
