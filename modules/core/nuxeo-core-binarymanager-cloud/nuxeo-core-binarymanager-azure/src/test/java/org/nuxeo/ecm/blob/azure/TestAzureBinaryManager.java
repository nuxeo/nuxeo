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
import static org.junit.Assume.assumeFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.blob.AbstractTestCloudBinaryManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobHeaders;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import com.microsoft.azure.storage.core.StreamMd5AndLength;
import com.microsoft.azure.storage.core.Utility;

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
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestAzureBinaryManager extends AbstractTestCloudBinaryManager<AzureBinaryManager> {

    protected final static List<String> PARAMETERS = Arrays.asList(AzureBinaryManager.ACCOUNT_KEY_PROPERTY,
            AzureBinaryManager.ACCOUNT_NAME_PROPERTY, AzureBinaryManager.CONTAINER_PROPERTY);

    protected static Map<String, String> properties = new HashMap<>();

    protected static final String PREFIX = "testfolder/";

    @BeforeClass
    public static void initialize() {
        AbstractCloudBinaryManager bm = new AzureBinaryManager();
        PARAMETERS.forEach(s -> {
            properties.put(s, Framework.getProperty(bm.getSystemPropertyName(s)));
        });

        // Ensure mandatory parameters are set
        PARAMETERS.forEach(s -> {
            assumeFalse(isBlank(properties.get(s)));
        });
        properties.put("prefix", PREFIX);
    }

    @AfterClass
    public static void afterClass() {
        // Cleanup keys
        Properties props = Framework.getProperties();
        PARAMETERS.forEach(props::remove);
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
        binaryManager.container.listBlobs(PREFIX, false).forEach(lb -> {
            try {
                if (lb instanceof CloudBlockBlob) { // ignore subdirectories
                    String name = ((CloudBlockBlob) lb).getName();
                    String digest = name.substring(PREFIX.length());
                    digests.add(digest);
                }
            } catch (URISyntaxException e) {
                // Do nothing.
            }
        });
        return digests;
    }

    protected Set<String> listAllObjects() {
        Set<String> names = new HashSet<>();
        binaryManager.container.listBlobs("", true).forEach(lb -> {
            try {
                String name = ((CloudBlockBlob) lb).getName();
                names.add(name);
            } catch (URISyntaxException e) {
                // Do nothing.
            }
        });
        return names;
    }

    @Test
    public void testSigning() throws IOException, URISyntaxException, StorageException, InvalidKeyException {
        CloudBlobContainer container = binaryManager.container;
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);

        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(CONTENT_MD5);
        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setPermissionsFromString("r");

        // rscd content-dispositoon
        // rsct content-type

        Instant endDateTime = LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant();
        policy.setSharedAccessExpiryTime(Date.from(endDateTime));

        SharedAccessBlobHeaders headers = new SharedAccessBlobHeaders();
        headers.setContentDisposition("attachment; filename=\"blabla.txt\"");
        headers.setContentType("text/plain");

        String something = blockBlobReference.generateSharedAccessSignature(policy, headers, null);
        System.out.println(something);

        CloudBlockBlob blob = new CloudBlockBlob(blockBlobReference.getUri(),
                new StorageCredentialsSharedAccessSignature(something));

        System.out.println(blob.getQualifiedUri());
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
            return RFC2231.encodeContentDisposition(blob.getFilename(), false, null);
        } else {
            return DownloadHelper.getRFC2231ContentDisposition(servletRequest, blob.getFilename());
        }
    }

    @Test
    public void ensureDigestDecode() throws IOException, StorageException {
        Blob blob = Blobs.createBlob(CONTENT2);
        String contentMd5;
        try (InputStream is = blob.getStream()) {
            StreamMd5AndLength md5 = Utility.analyzeStream(is, blob.getLength(), 2 * Constants.MB, true, true);
            contentMd5 = md5.getMd5();
        }

        assertTrue(AzureFileStorage.isBlobDigestCorrect(CONTENT2_MD5, contentMd5));
    }

    @Override
    @Test
    public void testBinaryManagerGC() throws Exception {
        if (binaryManager.prefix.isEmpty()) {
            // no additional test if no bucket name prefix
            super.testBinaryManagerGC();
            return;
        }

        // create a md5-looking extra file at the root
        String name1 = "12345678901234567890123456789012";
        try (InputStream in = new ByteArrayInputStream(new byte[] { '0' })) {
            CloudBlockBlob blob = binaryManager.container.getBlockBlobReference(name1);
            blob.upload(in, 1);
        }
        // create a md5-looking extra file in a "subdirectory" of the prefix
        String name2 = binaryManager.prefix + "subfolder/12345678901234567890123456789999";
        try (InputStream in = new ByteArrayInputStream(new byte[] { '0' })) {
            CloudBlockBlob blob = binaryManager.container.getBlockBlobReference(name2);
            blob.upload(in, 1);
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
    public void testRemoteURI() throws Exception {
        Blob blob = Blobs.createBlob(CONTENT);
        Binary binary = binaryManager.getBinary(blob);
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.digest = binary.getDigest();
        blobInfo.length = Long.valueOf(blob.getLength());
        blobInfo.filename = "caf\u00e9 corner.txt";
        blobInfo.mimeType = "text/plain";
        ManagedBlob mb = new SimpleManagedBlob(blobInfo );
        URI uri = binaryManager.getRemoteUri(binary.getDigest(), mb, null);
        String string = uri.toASCIIString();
        // %-escaped version
        assertTrue(string, string.contains("filename*%3DUTF-8%27%27caf%C3%A9%20corner.txt"));
    }

}
