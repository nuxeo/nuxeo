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

import static software.amazon.awssdk.services.s3.model.StorageClass.STANDARD;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.blob.CloudBlobProvider;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobStatus;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.CachingBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.TransactionalBlobStore;
import org.nuxeo.ecm.core.io.download.DownloadHelper;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Blob provider that stores files in S3.
 * <p>
 * This blob provider supports transactional record mode.
 *
 * @since 11.1
 */
public class S3BlobProvider extends CloudBlobProvider<S3BlobStoreConfiguration> implements S3ManagedTransfer {

    private static final Logger log = LogManager.getLogger(S3BlobProvider.class);

    /**
     * @since 2023
     */
    public static final String STORE_SCROLL_NAME = "s3BlobScroll";

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        log.info("Registering S3 blob provider {}", blobProviderId);
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

    /**
     * @since 2023.12
     */
    @Override
    public boolean allowDirectDownload() {
        var allow = super.allowDirectDownload();
        if (allow && config.useClientSideEncryption) {
            log.warn("Cannot allow s3 direct download with client side encryption enabled.");
            return false;
        }
        return allow;
    }

    @Deprecated(since = "2025.11", forRemoval = true)
    protected S3BlobStoreConfiguration getConfiguration(Map<String, String> properties) throws IOException {
        return new S3BlobStoreConfiguration(properties);
    }

    @Override
    public S3TransferManager getTransferManager() {
        return config.transferManager;
    }

    @Override
    public void close() {
        config.close();
    }

    /** Checks if the bucket exists (used in health check probes). */
    public boolean canAccessBucket() {
        try {
            config.amazonS3.headBucket(b -> b.bucket(config.bucketName));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    /**
     * Gets the blob length from the underlying s3 bucket.
     *
     * @return the blob length or -1 if the blob does not exist in storage
     * @since 2023.9
     */
    public long lengthOfBlob(ManagedBlob blob) throws IOException {
        String key = stripBlobKeyPrefix(blob.getKey());
        var s3Key = new S3BlobKey(config, key);
        try {
            return config.amazonS3.headObject(
                    b -> b.bucket(config.bucketName).key(s3Key.bucketKey()).versionId(s3Key.versionId()))
                                  .contentLength();
        } catch (SdkException e) {
            if (S3BlobStore.isMissingKey(e)) {
                log.debug("Failed to get information on blob: {}", key, e);
                // don't crash for a missing blob, even though it means the storage is corrupted
                return -1;
            }
            throw new IOException(e);
        }
    }

    @Override
    public URI getURI(ManagedBlob blob, BlobManager.UsageHint hint, HttpServletRequest servletRequest)
            throws IOException {
        if (hint != BlobManager.UsageHint.DOWNLOAD || !config.directDownload) {
            return null;
        }
        var s3Key = new S3BlobKey(config, stripBlobKeyPrefix(blob.getKey()));
        long expiresMs = config.directDownloadExpire * 1000;
        try {
            if (config.cloudFront.enabled) {
                return getURICloudFront(s3Key, blob, Instant.ofEpochMilli(expiresMs), servletRequest);
            } else {
                return getURIS3(s3Key, blob, Duration.ofMillis(expiresMs), servletRequest);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    protected URI getURICloudFront(S3BlobKey s3Key, ManagedBlob blob, Instant expiration,
            HttpServletRequest servletRequest) throws URISyntaxException {
        CloudFrontConfiguration cloudFront = config.cloudFront;
        String protocol = cloudFront.protocol;
        String baseURI = "http".equals(protocol) || "https".equals(protocol)
                ? protocol + "://" + cloudFront.distributionDomain + "/" + s3Key.bucketKey()
                : s3Key.bucketKey();
        URIBuilder uriBuilder = new URIBuilder(baseURI);
        if (s3Key.isVersioned()) {
            uriBuilder.addParameter("versionId", s3Key.versionId());
        }
        uriBuilder.addParameter("response-content-type", getContentTypeHeader(blob));
        uriBuilder.addParameter("response-content-disposition", getContentDispositionHeader(blob, servletRequest));
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
        if (cloudFront.privateKeyPath == null) {
            return uri;
        } else {
            CannedSignerRequest cannedRequest;
            try {
                cannedRequest = CannedSignerRequest.builder()
                                                   .resourceUrl(uri.toString())
                                                   .privateKey(cloudFront.privateKeyPath)
                                                   .keyPairId(cloudFront.keyPairId)
                                                   .expirationDate(expiration)
                                                   .build();
            } catch (Exception e) {
                // why v2 sdk is throwing Exception :|
                throw new NuxeoException("Cannot generate cloud front url", e);
            }
            return new URI(CloudFrontUtilities.create().getSignedUrlWithCannedPolicy(cannedRequest).url());
        }
    }

    protected URI getURIS3(S3BlobKey s3Key, ManagedBlob blob, Duration expiration, HttpServletRequest servletRequest)
            throws URISyntaxException {
        // split version id if part of file key
        S3Presigner.Builder s3Presignerbuilder = S3Presigner.builder()
                                                            .credentialsProvider(config.awsCredentialsProvider)
                                                            .region(config.region);
        if (config.endpointOverride != null) {
            s3Presignerbuilder.endpointOverride(config.endpointOverride);
        }
        try (S3Presigner presigner = s3Presignerbuilder.build()) {
            if (servletRequest != null && "HEAD".equals(servletRequest.getMethod())) {
                return presigner.presignHeadObject(r -> r.signatureDuration(expiration).headObjectRequest(hor -> {
                    hor.bucket(config.bucketName)
                       .key(s3Key.bucketKey())
                       .responseContentDisposition(getContentDispositionHeader(blob, servletRequest))
                       .responseContentType(getContentTypeHeader(blob));
                    if (s3Key.isVersioned()) {
                        hor.versionId(s3Key.versionId());
                    }
                })).url().toURI();
            } else {
                return presigner.presignGetObject(r -> r.signatureDuration(expiration).getObjectRequest(gor -> {
                    gor.bucket(config.bucketName)
                       .key(s3Key.bucketKey())
                       .responseContentDisposition(getContentDispositionHeader(blob, servletRequest))
                       .responseContentType(getContentTypeHeader(blob));
                    if (s3Key.isVersioned()) {
                        gor.versionId(s3Key.versionId());
                    }
                })).url().toURI();
            }
        }
    }

    protected String getContentTypeHeader(Blob blob) {
        return DownloadHelper.getContentTypeHeader(blob);
    }

    protected String getContentDispositionHeader(Blob blob, HttpServletRequest servletRequest) {
        if (servletRequest != null) {
            return DownloadHelper.getRFC2231ContentDisposition(servletRequest, blob.getFilename());
        } else {
            return RFC2231.encodeContentDisposition(blob.getFilename(), false);
        }
    }

    @Override
    public BlobStatus getStatus(ManagedBlob blob) throws IOException {
        String key = stripBlobKeyPrefix(blob.getKey());
        var s3Key = new S3BlobKey(config, key);
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                                               .bucket(config.bucketName)
                                                               .key(s3Key.bucketKey())
                                                               .build();
        try {
            HeadObjectResponse response = config.amazonS3.headObject(headObjectRequest);
            BlobStatus blobStatus = new BlobStatus();
            // storage class is null for STANDARD
            StorageClass storageClass = Objects.requireNonNullElse(response.storageClass(), STANDARD);
            blobStatus.withStorageClass(storageClass.toString());
            // the object storage class can be Standard or Glacier.
            // the Glacier Storage class can have one of these 3 states:
            // x-amz-restore absent
            // x-amz-restore: ongoing-request="true"
            // x-amz-restore: ongoing-request="false", expiry-date="Fri, 23 Dec 2012 00:00:00 GMT"
            String restore = response.restore();
            boolean ongoingRestore = S3Utils.isOnGoingRestore(restore);
            Instant downloadableUntil = S3Utils.getRestoreExpiryDate(restore);
            boolean downloadable = S3BlobStoreConfiguration.SUPPORTED_STORAGE_CLASS.contains(storageClass)
                    || downloadableUntil != null;
            blobStatus.withDownloadable(downloadable)
                      .withDownloadableUntil(downloadableUntil)
                      .withOngoingRestore(ongoingRestore);
            return blobStatus;
        } catch (SdkException e) {
            if (S3BlobStore.isMissingKey(e)) {
                log.error("Failed to get information on blob: {}", key);
                return new BlobStatus().withDownloadable(false);
            }
            throw e;
        }
    }

    @Override
    public String getStoreScrollName() {
        return STORE_SCROLL_NAME;
    }

}
