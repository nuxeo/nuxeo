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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.FileStorage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobHeaders;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureBinaryManager extends AbstractCloudBinaryManager {

    private static final Log log = LogFactory.getLog(AzureBinaryManager.class);

    private static final String STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=%s;" + "AccountName=%s;"
            + "AccountKey=%s";

    public static final String ENDPOINT_PROTOCOL_PROPERTY = "endpointProtocol";

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.storage.azure";

    public static final String ACCOUNT_NAME_PROPERTY = "account.name";

    public static final String ACCOUNT_KEY_PROPERTY = "account.key";

    public static final String CONTAINER_PROPERTY = "container";

    /** @since 10.10 */
    public static final String PREFIX_PROPERTY = "prefix";

    protected CloudStorageAccount storageAccount;

    protected CloudBlobClient blobClient;

    protected CloudBlobContainer container;

    protected String prefix;

    @Override
    protected String getSystemPropertyPrefix() {
        return SYSTEM_PROPERTY_PREFIX;
    }

    @Override
    protected void setupCloudClient() throws IOException {
        if (StringUtils.isBlank(properties.get(AzureBinaryManager.ACCOUNT_KEY_PROPERTY))) {
            properties.put(AzureBinaryManager.ACCOUNT_NAME_PROPERTY, System.getenv("AZURE_STORAGE_ACCOUNT"));
            properties.put(AzureBinaryManager.ACCOUNT_KEY_PROPERTY, System.getenv("AZURE_STORAGE_ACCESS_KEY"));
        }

        String connectionString = String.format(STORAGE_CONNECTION_STRING,
                getProperty(ENDPOINT_PROTOCOL_PROPERTY, "https"), getProperty(ACCOUNT_NAME_PROPERTY),
                getProperty(ACCOUNT_KEY_PROPERTY));
        try {
            storageAccount = CloudStorageAccount.parse(connectionString);

            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(getProperty(CONTAINER_PROPERTY));
            container.createIfNotExists();
        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            throw new IOException("Unable to initialize Azure binary manager", e);
        }
        prefix = StringUtils.defaultIfBlank(properties.get(PREFIX_PROPERTY), "");
        String delimiter = blobClient.getDirectoryDelimiter();
        if (StringUtils.isNotBlank(prefix) && !prefix.endsWith(delimiter)) {
            prefix += delimiter;
        }
        if (StringUtils.isNotBlank(namespace)) {
            // use namespace as an additional prefix
            prefix += namespace;
            if (!prefix.endsWith(delimiter)) {
                prefix += delimiter;
            }
        }
    }

    @Override
    protected BinaryGarbageCollector instantiateGarbageCollector() {
        return new AzureGarbageCollector(this);
    }

    @Override
    protected FileStorage getFileStorage() {
        return new AzureFileStorage(container, prefix);
    }

    @Override
    protected URI getRemoteUri(String digest, ManagedBlob blob, HttpServletRequest servletRequest) throws IOException {
        try {
            CloudBlockBlob blockBlobReference = container.getBlockBlobReference(digest);
            SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
            policy.setPermissionsFromString("r");

            Instant endDateTime = LocalDateTime.now()
                                               .plusSeconds(directDownloadExpire)
                                               .atZone(ZoneId.systemDefault())
                                               .toInstant();
            policy.setSharedAccessExpiryTime(Date.from(endDateTime));

            SharedAccessBlobHeaders headers = new SharedAccessBlobHeaders();
            headers.setContentDisposition(getContentDispositionHeader(blob, servletRequest));
            headers.setContentType(getContentTypeHeader(blob));

            String sas = blockBlobReference.generateSharedAccessSignature(policy, headers, null);

            CloudBlockBlob signedBlob = new CloudBlockBlob(blockBlobReference.getUri(),
                    new StorageCredentialsSharedAccessSignature(sas));
            return signedBlob.getSnapshotQualifiedUri();
        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected String getContentDispositionHeader(Blob blob, HttpServletRequest servletRequest) {
        // Azure will do the %-encoding itself, pass it a String directly
        return "attachment; filename*=UTF-8''" + blob.getFilename();
    }

    protected void removeBinary(String digest) {
        try {
            container.getBlockBlobReference(prefix + digest).delete();
        } catch (StorageException | URISyntaxException e) {
            log.error("Unable to remove binary " + digest, e);
        }
    }

    @Override
    public void removeBinaries(Collection<String> digests) {
        digests.forEach(this::removeBinary);
    }

    /**
     * @since 11.5
     * @return the length of the blob with the given {@code digest}, or -1 if missing
     */
    protected long lengthOfBlob(String digest) throws URISyntaxException, StorageException {
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(prefix + digest);
            blob.downloadAttributes();
            return blob.getProperties().getLength();
        } catch (StorageException e) {
            if (isMissingKey(e)) {
                return -1;
            }
            throw e;
        }
    }

    /**
     * @since 11.5
     */
    protected static boolean isMissingKey(StorageException e) {
        return (e.getHttpStatusCode() == 404) || "BlobNotFound".equals(e.getErrorCode())
                || "The specified blob does not exist.".equals(e.getMessage());
    }

}
