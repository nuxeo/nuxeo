/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.blob.s3;

import static org.nuxeo.ecm.core.blob.KeyStrategy.VER_SEP;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobStatus;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.CachingBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.TransactionalBlobStore;
import org.nuxeo.ecm.core.io.download.DownloadHelper;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * Blob provider that stores files in S3.
 * <p>
 * This blob provider supports transactional record mode.
 *
 * @since 11.1
 */
public class S3BlobProvider extends BlobStoreBlobProvider implements S3ManagedTransfer {

    private static final Logger log = LogManager.getLogger(S3BlobProvider.class);

    // public for tests
    public S3BlobStoreConfiguration config;

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        config = getConfiguration(properties);
        log.info("Registering S3 blob provider '" + blobProviderId);
        KeyStrategy keyStrategy = getKeyStrategy();

        // main S3 blob store wrapped in a caching store
        BlobStore store = new S3BlobStore(blobProviderId, "S3", config, keyStrategy);
        boolean caching = !config.getBooleanProperty("nocache");
        if (caching) {
            store = new CachingBlobStore(blobProviderId, "Cache", store, config.cachingConfiguration);
        }

        // maybe wrap into a transactional store
        if (isTransactional()) {
            BlobStore transientStore;
            if (store.hasVersioning()) {
                // if versioning is used, we don't need a separate transient store for transactions
                transientStore = store;
            } else {
                // transient store is another S3 blob store wrapped in a caching store
                S3BlobStoreConfiguration transientConfig = config.withNamespace("tx");
                transientStore = new S3BlobStore(blobProviderId, "S3_tmp", transientConfig, keyStrategy);
                if (caching) {
                    transientStore = new CachingBlobStore(blobProviderId, "Cache_tmp", transientStore,
                            config.cachingConfiguration);
                }
            }
            // transactional store
            store = new TransactionalBlobStore(blobProviderId, store, transientStore);
        }
        return store;
    }

    protected S3BlobStoreConfiguration getConfiguration(Map<String, String> properties) throws IOException {
        return new S3BlobStoreConfiguration(properties);
    }

    @Override
    public TransferManager getTransferManager() {
        return config.transferManager;
    }

    @Override
    public void close() {
        config.close();
    }

    @Override
    protected String getDigestAlgorithm() {
        return config.digestConfiguration.digestAlgorithm;
    }

    /** Checks if the bucket exists (used in health check probes). */
    public boolean canAccessBucket() {
        return config.amazonS3.doesBucketExistV2(config.bucketName);
    }

    @Override
    public URI getURI(ManagedBlob blob, BlobManager.UsageHint hint, HttpServletRequest servletRequest)
            throws IOException {
        if (hint != BlobManager.UsageHint.DOWNLOAD || !config.directDownload) {
            return null;
        }
        String bucketKey = config.bucketPrefix + stripBlobKeyPrefix(blob.getKey());
        Date expiration = new Date(System.currentTimeMillis() + config.directDownloadExpire * 1000);
        try {
            if (config.cloudFront.enabled) {
                return getURICloudFront(bucketKey, blob, expiration);
            } else {
                return getURIS3(bucketKey, blob, expiration);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    protected URI getURICloudFront(String bucketKey, ManagedBlob blob, Date expiration)
            throws URISyntaxException {
        String[] parts = bucketKey.split(String.valueOf(VER_SEP));
        bucketKey = parts[0];
        CloudFrontConfiguration cloudFront = config.cloudFront;
        Protocol protocol = cloudFront.protocol;
        String baseURI = protocol == Protocol.http || protocol == Protocol.https
                ? protocol + "://" + cloudFront.distributionDomain + "/" + bucketKey
                : bucketKey;
        URIBuilder uriBuilder = new URIBuilder(baseURI);
        if (parts.length > 1) {
            uriBuilder.addParameter("versionId", parts[1]);
        }
        uriBuilder.addParameter("response-content-type", getContentTypeHeader(blob));
        uriBuilder.addParameter("response-content-disposition", getContentDispositionHeader(blob));
        if (cloudFront.fixEncoding) {
            // remove spaces in the values, as they're not encoded correctly due to a bug somewhere
            // this happens in particular for the Content-Disposition header
            for (NameValuePair p : uriBuilder.getQueryParams()) {
                String value = p.getValue();
                if (value != null && value.contains(" ")) {
                    uriBuilder.setParameter(p.getName(), value.replace(" ", ""));
                }
            }
        }
        URI uri = uriBuilder.build();
        if (cloudFront.privateKey == null) {
            return uri;
        } else {
            String signedURL = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(uri.toString(), cloudFront.keyPairId,
                    cloudFront.privateKey, expiration);
            return new URI(signedURL);
        }
    }

    protected URI getURIS3(String bucketKey, ManagedBlob blob, Date expiration) throws URISyntaxException {
        // split version id if part of file key
        String[] parts = bucketKey.split(String.valueOf(VER_SEP));
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(config.bucketName, parts[0],
                HttpMethod.GET);
        if (parts.length > 1) {
            request.setVersionId(parts[1]);
        }
        request.addRequestParameter("response-content-type", getContentTypeHeader(blob));
        request.addRequestParameter("response-content-disposition", getContentDispositionHeader(blob));
        request.setExpiration(expiration);
        URL url = config.amazonS3.generatePresignedUrl(request);
        return url.toURI();
    }

    protected String getContentTypeHeader(Blob blob) {
        return DownloadHelper.getContentTypeHeader(blob);
    }

    protected String getContentDispositionHeader(Blob blob) {
        return RFC2231.encodeContentDisposition(blob.getFilename(), false, null);
    }

    @Override
    public BlobStatus getStatus(ManagedBlob blob) throws IOException {
        String key = stripBlobKeyPrefix(blob.getKey());
        String objectKey;
        String versionId;
        int seppos = key.indexOf(VER_SEP);
        if (seppos < 0) {
            objectKey = key;
            versionId = null;
        } else {
            objectKey = key.substring(0, seppos);
            versionId = key.substring(seppos + 1);
        }
        String bucketKey = config.bucketPrefix + objectKey;
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(config.bucketName, bucketKey, versionId);
        ObjectMetadata metadata;
        try {
            metadata = config.amazonS3.getObjectMetadata(request);
        } catch (AmazonServiceException e) {
            if (S3BlobStore.isMissingKey(e)) {
                // don't crash for a missing blob, even though it means the storage is corrupted
                log.error("Failed to get information on blob: {}", key, e);
                return new BlobStatus().withDownloadable(false);
            }
            throw new IOException(e);
        }
        // storage class is null for STANDARD
        String storageClass = metadata.getStorageClass();
        if (StorageClass.Standard.toString().equals(storageClass)) {
            storageClass = null;
        }

        // the object storage class can be Standard or Glacier.
        // the Glacier Storage class can have one of these 3 states:
        // x-amz-restore absent
        // x-amz-restore: ongoing-request="true"
        // x-amz-restore: ongoing-request="false", expiry-date="Fri, 23 Dec 2012 00:00:00 GMT"
        Date date = metadata.getRestoreExpirationTime();
        Instant downloadableUntil = date == null ? null : date.toInstant();
        boolean downloadable = storageClass == null || downloadableUntil != null;
        boolean ongoingRestore = BooleanUtils.isTrue(metadata.getOngoingRestore());
        return new BlobStatus().withStorageClass(storageClass)
                               .withDownloadable(downloadable)
                               .withDownloadableUntil(downloadableUntil)
                               .withOngoingRestore(ongoingRestore);
    }

}
