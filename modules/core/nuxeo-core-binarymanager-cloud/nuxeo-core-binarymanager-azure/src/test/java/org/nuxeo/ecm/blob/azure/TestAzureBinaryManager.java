/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.blob.azure;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.blob.AbstractTestCloudBinaryManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.platform.picture.core.ImagingFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

/**
 * WARNING: You must pass those variables to your test configuration:
 * <p>
 *
 * <pre>
 *   -Dnuxeo.storage.azure.account.name: Azure account name
 *   -Dnuxeo.storage.azure.account.key: Azure account key
 *   -Dnuxeo.storage.azure.container: A test container name
 * </pre>
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @deprecated since 2023.9, use {@link AzureBlobProvider} instead
 */
@Deprecated(since = "2023.9")
@RunWith(FeaturesRunner.class)
@Features(ImagingFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.azure.test:OSGI-INF/test-blob-provider-azure.xml")
public class TestAzureBinaryManager extends AbstractTestCloudBinaryManager<AzureBinaryManager> {

    private static final Logger log = LogManager.getLogger(TestAzureBinaryManager.class);

    protected static final List<String> PARAMETERS = Arrays.asList(AzureBinaryManager.ACCOUNT_KEY_PROPERTY,
            AzureBinaryManager.ACCOUNT_NAME_PROPERTY, AzureBinaryManager.CONTAINER_PROPERTY);

    protected static Map<String, String> properties = new HashMap<>();

    protected static final String PREFIX = "testfolder/";

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @BeforeClass
    public static void initialize() {
        AbstractCloudBinaryManager bm = new AzureBinaryManager();
        PARAMETERS.forEach(s -> properties.put(s, Framework.getProperty(bm.getSystemPropertyName(s))));

        // Ensure mandatory parameters are set
        PARAMETERS.forEach(s -> assumeFalse(isBlank(properties.get(s))));
        properties.put(AzureBinaryManager.PREFIX_PROPERTY, PREFIX);
    }

    @AfterClass
    public static void afterClass() {
        // Cleanup keys
        Properties props = Framework.getProperties();
        PARAMETERS.forEach(props::remove);
    }

    @After
    public void tearDown() {
        removeObjects();
    }

    /**
     * Removes all objects in the container, not only those under the configured prefix.
     *
     * @since 11.5
     */
    @Override
    protected void removeObjects() {
        binaryManager.client.listBlobs().iterator().forEachRemaining(item -> {
            binaryManager.client.getBlobClient(item.getName()).delete();
        });
    }

    @Override
    protected AzureBinaryManager getBinaryManager() throws IOException {
        AzureBinaryManager binaryManager = new AzureBinaryManager();
        binaryManager.initialize("azuretest", properties);
        return binaryManager;
    }

    @Override
    protected Set<String> listObjects() {
        Set<String> digests = new HashSet<>();
        binaryManager.client.listBlobsByHierarchy(PREFIX).forEach(blob -> {
            if (blob.isPrefix()) {
                // ignore sub directories
                return;
            }
            String name = blob.getName();
            String digest = name.substring(PREFIX.length());
            digests.add(digest);
        });
        return digests;
    }

    protected Set<String> listAllObjects() {
        Set<String> names = new HashSet<>();
        binaryManager.client.listBlobs().forEach(blob -> {
            String name = blob.getName();
            names.add(name);
        });
        return names;
    }

    @Test
    public void testSigning() throws IOException {
        BlobContainerClient client = binaryManager.client;
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);

        BlobClient blobClient = client.getBlobClient(CONTENT_MD5);

        // specify token properties
        BlobSasPermission permissions = BlobSasPermission.parse("r");

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        // build the token
        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        sasSignatureValues.setContentDisposition("attachment; filename=\"blabla.txt\"");
        sasSignatureValues.setContentType("text/plain");
        String something = blobClient.generateSas(sasSignatureValues);

    }

    protected String getContentTypeHeader(Blob blob) {
        String contentType = blob.getMimeType();
        String encoding = blob.getEncoding();
        if (contentType != null && !StringUtils.isBlank(encoding)) {
            int i = contentType.indexOf(';');
            if (i >= 0) {
                contentType = contentType.substring(0, i);
            }
            contentType += "; charset=" + encoding;
        }
        return contentType;
    }

    protected String getContentDispositionHeader(Blob blob, HttpServletRequest servletRequest) {
        if (servletRequest == null) {
            return RFC2231.encodeContentDisposition(blob.getFilename(), false);
        } else {
            return DownloadHelper.getRFC2231ContentDisposition(servletRequest, blob.getFilename());
        }
    }

    @Override
    @Test
    public void testBinaryManagerGC() throws IOException {
        if (binaryManager.prefix.isEmpty()) {
            // no additional test if no bucket name prefix
            super.testBinaryManagerGC();
            return;
        }

        String name1 = "12345678901234567890123456789012";
        String name2 = binaryManager.prefix + "subfolder/12345678901234567890123456789999";
        // create a md5-looking extra file at the root
        try (InputStream in = new ByteArrayInputStream(new byte[] { '0' })) {
            BlobClient blobClient = binaryManager.client.getBlobClient(name1);
            blobClient.upload(in, 1);
        }
        // create a md5-looking extra file in a "subdirectory" of the prefix
        try (InputStream in = new ByteArrayInputStream(new byte[] { '0' })) {
            BlobClient blobClient = binaryManager.client.getBlobClient(name2);
            blobClient.upload(in, 1);
        }
        // check that the files are here
        assertEquals(new HashSet<>(Arrays.asList(name1, name2)), listAllObjects());
        // run base test with the prefix
        super.testBinaryManagerGC();

        // check that the extra files are still here
        Set<String> res = listAllObjects();
        assertTrue(res.contains(name1));
        assertTrue(res.contains(name2));
    }

    @Test
    public void testRemoteURI() throws IOException {
        Blob blob = Blobs.createBlob(CONTENT);
        Binary binary = binaryManager.getBinary(blob);
        BlobInfo blobInfo = new BlobInfo();
        String digest = binary.getDigest();
        blobInfo.digest = digest;
        blobInfo.length = Long.valueOf(blob.getLength());
        blobInfo.filename = "caf\u00e9 corner.txt";
        blobInfo.mimeType = "text/plain";
        blobInfo.key = digest;
        ManagedBlob mb = new SimpleManagedBlob("unusedBlobProviderId", blobInfo);
        URI uri = binaryManager.getRemoteUri(digest, mb, null);
        String uriString = uri.toASCIIString();
        assertEquals(String.format("https://%s.blob.core.windows.net/%s/%s",
                properties.get(AzureBinaryManager.ACCOUNT_NAME_PROPERTY),
                properties.get(AzureBinaryManager.CONTAINER_PROPERTY), digest), uriString);
    }

    @Test
    public void testConcurrentUploadSameImage() throws InterruptedException {
        int docCount = 10;
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(docCount + 1));
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < docCount; i++) {
            final int docIndex = i;
            tpe.submit(() -> {
                try {
                    createPicture("pictureDoc_" + docIndex);
                } catch (IOException | NuxeoException e) {
                    exceptions.add(e);
                }
            });
        }

        tpe.shutdown();
        assertTrue("ThreadPoolExecutor timeout", tpe.awaitTermination(20, TimeUnit.SECONDS));
        exceptions.stream().forEach(e -> log.error(e, e));
        exceptions.stream().findFirst().ifPresent(e -> fail(e.getMessage()));
    }

    protected void createPicture(String name) throws IOException {

        // a transaction is needed, otherwise we get "Cannot use a session outside a transaction"
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        TransactionHelper.startTransaction();

        DocumentModel doc = session.createDocumentModel("/", name, "Picture");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/Montreal.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "Montreal.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        transactionalFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        @SuppressWarnings("unchecked")
        List<Serializable> pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        if (CollectionUtils.isEmpty(pictureViews)) {
            throw new NuxeoException(String.format("Picture views are null or empty for document: %s", doc));
        }
    }

    @Override
    protected Set<String> getKeys(List<String> digests) {
        return new HashSet<>(digests);
    }

}
