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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.io.InputStream;
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
 * <p/>
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
        binaryManager.container.listBlobs().forEach(lb -> {
            try {
                digests.add(((CloudBlockBlob) lb).getName());
            } catch (URISyntaxException e) {
                // Do nothing.
            }
        });
        return digests;
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
}
