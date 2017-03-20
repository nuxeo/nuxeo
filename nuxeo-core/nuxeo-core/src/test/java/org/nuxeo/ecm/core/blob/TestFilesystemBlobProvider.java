/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features({ BlobManagerFeature.class, MockitoFeature.class })
@LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/test-fs-blobprovider.xml")
public class TestFilesystemBlobProvider {

    private static final String CONTENT = "hello";

    private static final String CONTENT_MD5 = "5d41402abc4b2a76b9719d911017c592";

    private static final String PROVIDER_ID = "testfs";

    protected Mockery mockery = new JUnit4Mockery();

    @Mock
    @RuntimeService
    RepositoryManager repositoryManager;

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected BlobManager blobManager;

    protected Path tmpFile;

    protected String tmpFilePath;

    @Before
    public void mockRepositoryManager() throws Exception {
        when(repositoryManager.getRepositoryNames()).thenReturn(Collections.emptyList());
    }

    @Before
    public void setUp() throws Exception {
        tmpFile = Framework.createTempFilePath("tmp", ".txt");
        try (OutputStream out = Files.newOutputStream(tmpFile)) {
            InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
            IOUtils.copy(in, out);
        }
        tmpFilePath = tmpFile.toString();
    }

    @After
    public void tearDown() throws Exception {
        if (tmpFile != null && Files.exists(tmpFile)) {
            Files.delete(tmpFile);
        }
    }

    @Test
    public void testSupportsUserUpdate() throws Exception {
        BlobProvider blobProvider = blobManager.getBlobProvider(PROVIDER_ID);
        assertFalse(blobProvider.supportsUserUpdate());

        // check that we can allow user updates of blobs by configuration
        harness.deployContrib("org.nuxeo.ecm.core.tests", "OSGI-INF/test-fs-blobprovider-override.xml");
        try {
            blobProvider = blobManager.getBlobProvider(PROVIDER_ID);
            assertTrue(blobProvider.supportsUserUpdate());
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.core.tests", "OSGI-INF/test-fs-blobprovider-override.xml");
        }
    }

    @Test
    public void testRead() throws Exception {
        String key = PROVIDER_ID + ":" + tmpFilePath;

        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        ManagedBlob blob = (ManagedBlob) blobManager.readBlob(blobInfo, null);
        assertNotNull(blob);
        assertEquals(key, blob.getKey());
        try (InputStream in = blob.getStream()) {
            assertEquals(CONTENT, IOUtils.toString(in));
        }

        // same with explicit blob
        blobInfo.key = tmpFilePath;
        blobInfo.mimeType = "text/plain";
        blob = ((FilesystemBlobProvider) blobManager.getBlobProvider(PROVIDER_ID)).createBlob(blobInfo);
        assertEquals(key, blob.getKey());
        assertEquals(tmpFile.getFileName().toString(), blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(CONTENT.length(), blob.getLength());
        assertEquals(CONTENT_MD5, blob.getDigest());
        try (InputStream in = blob.getStream()) {
            assertEquals(CONTENT, IOUtils.toString(in));
        }

        // write it, it has a prefix so doesn't need a doc
        String writtenKey = blobManager.writeBlob(blob, null, "somexpath");
        assertEquals(key, writtenKey);
    }

    @Test
    public void testReadNotFound() throws Exception {
        String path = "/NO_SUCH_FILE_EXISTS";
        assertFalse(Files.exists(Paths.get(path)));
        String key = PROVIDER_ID + ":" + path;

        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        ManagedBlob blob = (ManagedBlob) blobManager.readBlob(blobInfo, null);
        try {
            blob.getStream();
            fail("Should not be able to read non-existent file");
        } catch (NoSuchFileException e) {
            // expected
        }
    }

    @Test
    public void testRoot() throws Exception {
        String PROVIDER_ID2 = "testfs2";
        BlobProviderDescriptor descr = new BlobProviderDescriptor();
        descr.klass = FilesystemBlobProvider.class;
        descr.name = PROVIDER_ID2;
        descr.properties = Collections.singletonMap(FilesystemBlobProvider.ROOT_PROP, tmpFile.getParent().toString());
        ((BlobManagerComponent) blobManager).registerBlobProvider(descr);
        try {
            // use relative path under root
            String key = PROVIDER_ID2 + ":" + tmpFile.getFileName().toString();
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = key;
            ManagedBlob blob = (ManagedBlob) blobManager.readBlob(blobInfo, null);
            assertNotNull(blob);
            assertEquals(key, blob.getKey());
            try (InputStream in = blob.getStream()) {
                assertEquals(CONTENT, IOUtils.toString(in));
            }
        } finally {
            ((BlobManagerComponent) blobManager).unregisterBlobProvider(descr);
        }
    }

    @Test
    public void testIllegalPath() throws Exception {
        String illegalPath = "../foo";

        String PROVIDER_ID2 = "testfs2";
        BlobProviderDescriptor descr = new BlobProviderDescriptor();
        descr.klass = FilesystemBlobProvider.class;
        descr.name = PROVIDER_ID2;
        descr.properties = Collections.singletonMap(FilesystemBlobProvider.ROOT_PROP, tmpFile.getParent().toString());
        ((BlobManagerComponent) blobManager).registerBlobProvider(descr);
        try {
            String key = PROVIDER_ID2 + ":" + illegalPath;
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = key;
            ManagedBlob blob = (ManagedBlob) blobManager.readBlob(blobInfo, null);
            assertNotNull(blob);
            assertEquals(key, blob.getKey());
            try {
                blob.getStream();
                fail("Should not be able to read file with illegal path");
            } catch (FileNotFoundException e) {
                // ok
            }
        } finally {
            ((BlobManagerComponent) blobManager).unregisterBlobProvider(descr);
        }
    }

}
