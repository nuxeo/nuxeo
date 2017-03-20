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
        String key = blobManager.writeBlob(blob, doc, "somexpath");
        assertEquals("dummy:1234", key);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/test-blob-dispatch.xml")
    public void testDispatch() throws Exception {
        DummyBlobProvider.resetAllCounters();
        // blob that's not a video gets stored on the first dummy repo
        Blob blob = Blobs.createBlob("foo", "text/plain");
        Document doc = mockery.mock(Document.class, "doc1");
        String key = blobManager.writeBlob(blob, doc, "somexpath");
        assertEquals("dummy:1", key);
        // videos get stored in the second one
        blob = Blobs.createBlob("bar", "video/mp4");
        key = blobManager.writeBlob(blob, doc, "somexpath");
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

    @Test
    @LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/test-blob-dispatch-xpath.xml")
    public void testDispatchXPath() throws Exception {
        DummyBlobProvider.resetAllCounters();
        Blob blob = Blobs.createBlob("foo", "text/plain");
        Document doc = mockery.mock(Document.class, "doc1");
        String key = blobManager.writeBlob(blob, doc, "content");
        assertEquals("dummy:1", key);

        // files/0/file gets stored in the second blob provider
        blob = Blobs.createBlob("bar", "text/plain");
        key = blobManager.writeBlob(blob, doc, "files/0/file");
        assertEquals("dummy2:1", key);
    }

}
