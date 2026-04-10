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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.DELIMITER;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.SUPPORTED_STORAGE_CLASS;
import static org.nuxeo.ecm.blob.s3.S3Utils.sanitizeETag;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;
import static org.nuxeo.ecm.core.blob.KeyStrategy.VER_SEP;
import static software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus.OFF;
import static software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus.ON;
import static software.amazon.awssdk.services.s3.model.StorageClass.STANDARD;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.blob.AbstractBlobGarbageCollector;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobContext;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobUpdateContext;
import org.nuxeo.ecm.core.blob.BlobWriteContext;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.PathStrategy;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.encryption.s3.S3EncryptionClientException;

/**
 * Blob storage in S3.
 *
 * @since 11.1
 */
public class S3BlobStore extends AbstractBlobStore {

    private static final Logger log = LogManager.getLogger(S3BlobStore.class);

    private static final Logger logs3dl = LogManager.getLogger("S3_Download");

    // x-amz-meta-username header
    protected static final String USER_METADATA_USERNAME = "username";

    protected final S3BlobStoreConfiguration config;

    protected final S3Client amazonS3;

    protected final String bucketName;

    protected final String bucketPrefix;

    protected final PathStrategy pathStrategy;

    protected final boolean pathSeparatorIsBackslash;

    protected final boolean allowByteRange;

    protected volatile Boolean useAsyncDigest;

    protected final BinaryGarbageCollector gc;

    /** @since 11.5 */
    public S3BlobStore(String blobProviderId, String name, S3BlobStoreConfiguration config, KeyStrategy keyStrategy) {
        super(blobProviderId, name, keyStrategy);
        this.config = config;
        amazonS3 = config.amazonS3;
        bucketName = config.bucketName;
        bucketPrefix = config.bucketPrefix;
        pathStrategy = config.pathStrategy;
        pathSeparatorIsBackslash = config.pathSeparatorIsBackslash;
        allowByteRange = config.getBooleanProperty(ALLOW_BYTE_RANGE);
        gc = new S3BlobGarbageCollector();
    }

    protected static boolean isMissingKey(SdkException e) {
        return (e instanceof SdkServiceException sse && sse.statusCode() == 404)
                || (e instanceof S3EncryptionClientException
                        && e.getCause() instanceof SdkServiceException sdkServiceException
                        && sdkServiceException.statusCode() == 404);
    }

    @Override
    public boolean hasVersioning() {
        return config.useVersion();
    }

    @Override
    public boolean useAsyncDigest() {
        if (useAsyncDigest == null) {
            synchronized (this) {
                if (useAsyncDigest == null) {
                    useAsyncDigest = config.digestConfiguration.digestAsync && supportsAsyncDigest();
                }
            }
        }
        return useAsyncDigest;
    }

    /** Checks that all repositories support queries on blob keys. */
    protected boolean supportsAsyncDigest() {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        return repositoryService.getRepositoryNames()
                                .stream()
                                .map(repositoryService::getRepository)
                                .allMatch(this::supportsAsyncDigest);
    }

    protected boolean supportsAsyncDigest(Repository repository) {
        return repository.hasCapability(Repository.CAPABILITY_QUERY_BLOB_KEYS);
    }

    protected String bucketKey(String key) {
        return config.bucketKey(key);
    }

    @Override
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        Path file;
        String fileTraceSource;
        Path tmp = null;
        try {
            BlobContext blobContext = blobWriteContext.blobContext;
            Path blobWriteContextFile = blobWriteContext.getFile();
            if (blobWriteContextFile != null) {
                // we have a file, assume that the caller already observed the write
                file = blobWriteContextFile;
                fileTraceSource = "Nuxeo";
            } else {
                // no transfer to a file was done yet (no caching)
                // we may be able to use the blob's underlying file, if not pure streaming
                File blobFile = blobContext.blob.getFile();
                if (blobFile != null) {
                    // otherwise use blob file directly
                    if (blobWriteContext.writeObserver != null) {
                        // but we must still run the writes through the write observer
                        transfer(blobWriteContext, NullOutputStream.INSTANCE);
                    }
                    file = blobFile.toPath();
                    fileTraceSource = "Nuxeo";
                } else {
                    // we must transfer the blob stream to a tmp file
                    tmp = Files.createTempFile("bin_", ".tmp");
                    logTrace(null, "->", "tmp", "write");
                    logTrace("hnote right: " + tmp.getFileName());
                    transfer(blobWriteContext, tmp);
                    file = tmp;
                    fileTraceSource = "tmp";
                }
            }
            String key = blobWriteContext.getKey(); // may depend on write observer, for example for digests
            if (key == null) {
                // should never happen unless an invalid WriteObserver is used in new code
                throw new NuxeoException("Missing key");
            } else if (key.indexOf(VER_SEP) >= 0) {
                // should never happen unless AWS S3 changes their key format
                throw new NuxeoException(
                        "Invalid key '" + key + "', it contains the version separator '" + VER_SEP + "'");
            }
            String versionId = writeFile(key, file, blobContext, fileTraceSource);
            return versionId == null ? key : key + VER_SEP + versionId;
        } finally {
            if (tmp != null) {
                try {
                    logTrace("tmp", "-->", "tmp", "delete");
                    logTrace("hnote right: " + tmp.getFileName());
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }
    }

    /** Writes a file with the given key and returns its version id. */
    protected String writeFile(String key, Path file, BlobContext blobContext, String fileTraceSource)
            throws IOException {
        String bucketKey = bucketKey(key);
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("Writing s3://{}/{}", bucketName, bucketKey);
        }

        if (getKeyStrategy().useDeDuplication() && bucketKeyExists(bucketKey)) {
            if (bucketKeyHasDefaultStorageClass(bucketKey)) {
                return null; // no key version used with deduplication
            } else {
                log.warn("Restoring blob: s3://{}/{} by overwriting", bucketName, bucketKey);
            }
        }

        UploadFileRequest.Builder uploadFileRequestBuilder = UploadFileRequest.builder().putObjectRequest(b -> {
            b.bucket(bucketName).key(bucketKey);
            if (config.useServerSideEncryption) {
                if (isNotBlank(config.serverSideKMSKeyID)) {
                    // SSE-KMS
                    b.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(config.serverSideKMSKeyID);

                } else {
                    // SSE-S3
                    b.serverSideEncryption(ServerSideEncryption.AES256);
                }
            }
            setMetadata(b, blobContext);
            b.storageClass(config.storageClass);
        }).addTransferListener(LoggingTransferListener.create()).source(file);
        logTrace(fileTraceSource, "->", null, "write " + Files.size(file) + " bytes");
        logTrace("hnote right: " + bucketKey);
        try {
            FileUpload fileUpload = config.transferManager.uploadFile(uploadFileRequestBuilder.build());
            CompletedFileUpload uploadResult = fileUpload.completionFuture().join();
            // if we don't want to use versions, ignore them even though the bucket may be versioned
            String versionId = hasVersioning() ? uploadResult.response().versionId() : null;
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("Wrote s3://{}/{} in {}ms", bucketName, bucketKey, dtms);
            }
            if (versionId != null) {
                logTrace("<--", "v=" + versionId);
            }
            return versionId;
        } catch (CompletionException e) {
            throw new NuxeoException("Failed to write blob: " + key, e);
        }
    }

    protected void setMetadata(PutObjectRequest.Builder builder, BlobContext blobContext) {
        if (blobContext != null) {
            Blob blob = blobContext.blob;
            String filename = blob.getFilename();
            if (filename != null) {
                String contentDisposition = RFC2231.encodeContentDisposition(filename, false);
                builder.contentDisposition(contentDisposition);
            }
            String contentType = DownloadHelper.getContentTypeHeader(blob);
            builder.contentType(contentType);
        }
        if (config.metadataAddUsername) {
            NuxeoPrincipal principal = NuxeoPrincipal.getCurrent();
            if (principal != null && !(principal instanceof SystemPrincipal)) {
                String username = principal.getActingUser();
                if (username != null) {
                    builder.metadata(Collections.singletonMap(USER_METADATA_USERNAME, username));
                }
            }
        }
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.unknown();
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) {
        return OptionalOrUnknown.unknown();
    }

    @Override
    public boolean exists(String key) {
        return bucketKeyExists(bucketKey(key));
    }

    @Override
    public boolean hasDefaultStorageClass(String key) {
        return bucketKeyHasDefaultStorageClass(bucketKey(key));
    }

    protected boolean bucketKeyHasDefaultStorageClass(String bucketKey) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                                               .bucket(config.bucketName)
                                                               .key(bucketKey)
                                                               .build();
        try {
            var response = amazonS3.headObject(headObjectRequest);
            // storage class is null for STANDARD
            var storageClass = Objects.requireNonNullElse(response.storageClass(), STANDARD);
            return SUPPORTED_STORAGE_CLASS.contains(storageClass);
        } catch (SdkException e) {
            if (isMissingKey(e)) {
                return false;
            }
            throw e;
        }
    }

    protected boolean bucketKeyExists(String bucketKey) {
        logTrace("-->", "doesObjectExist");
        logTrace("hnote right: " + bucketKey);
        try {
            amazonS3.headObject(b -> b.bucket(config.bucketName).key(bucketKey));
            logTrace("<--", "exists");
            return true;
        } catch (SdkException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing"); // NOSONAR
                return false;
            }
            throw e;
        }
    }

    /** @return object length, or -1 if missing */
    protected long lengthOfBlob(String key) {
        String bucketKey = bucketKey(key);
        logTrace("-->", "getObjectMetadata");
        logTrace("hnote right: " + bucketKey);
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                                               .bucket(config.bucketName)
                                                               .key(bucketKey)
                                                               .build();
        try {
            return amazonS3.headObject(headObjectRequest).contentLength();
        } catch (SdkException e) {
            if (isMissingKey(e)) {
                log.debug("Failed to get information on blob: {}", key, e);
                // don't crash for a missing blob, even though it means the storage is corrupted
                return -1;
            }
            throw e;
        }
    }

    @Override
    public void clear() {
        logTrace("group ClearBucket");
        // No need to paginate, clear is used for tests purpose
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucketName).prefix(bucketPrefix);
        if (config.getSubDirsDepth() == 0) {
            // use delimiter to avoid useless listing of objects in "subdirectories"
            builder.delimiter(DELIMITER);
        }
        logTrace("->", "listObjects");
        ListObjectsV2Request listReq = builder.build();

        final List<ObjectIdentifier> keys = amazonS3.listObjectsV2Paginator(listReq)
                                                    .stream()
                                                    .flatMap(r -> r.contents().stream())
                                                    .map(c -> ObjectIdentifier.builder().key(c.key()).build())
                                                    .toList();

        if (!keys.isEmpty()) {
            DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
                                                                                .bucket(bucketName)
                                                                                .delete(d -> d.objects(keys))
                                                                                .build();
            amazonS3.deleteObjects(multiObjectDeleteRequest);
        }
        ListObjectVersionsRequest.Builder vbuilder = ListObjectVersionsRequest.builder()
                                                                              .bucket(bucketName)
                                                                              .prefix(bucketPrefix);
        if (config.getSubDirsDepth() == 0) {
            // use delimiter to avoid useless listing of objects in "subdirectories"
            builder.delimiter(DELIMITER);
        }
        ListObjectVersionsRequest listObjectVersionsRequest = vbuilder.build();
        logTrace("->", "listVersions");
        final List<ObjectIdentifier> vKeys = amazonS3.listObjectVersionsPaginator(listObjectVersionsRequest)
                                                     .stream()
                                                     .flatMap(r -> r.versions().stream())
                                                     .map(c -> ObjectIdentifier.builder().key(c.key()).build())
                                                     .toList();
        if (!vKeys.isEmpty()) {
            DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
                                                                                .bucket(bucketName)
                                                                                .delete(d -> d.objects(vKeys))
                                                                                .build();
            amazonS3.deleteObjects(multiObjectDeleteRequest);
        }
        logTrace("end");
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        ByteRange byteRange;
        if (allowByteRange) {
            MutableObject<String> keyHolder = new MutableObject<>(key);
            byteRange = getByteRangeFromKey(keyHolder);
            key = keyHolder.get();
        } else {
            byteRange = null;
        }
        key = getBlobKeyReplacement(key);
        var s3Key = new S3BlobKey(config, key);
        String debugObject = "s3://" + bucketName + "/" + s3Key;
        log.debug("Reading {}", debugObject);
        GetObjectRequest.Builder objectRequestBuilder = GetObjectRequest.builder()
                                                                        .bucket(bucketName)
                                                                        .key(s3Key.bucketKey())
                                                                        .versionId(s3Key.versionId());
        if (byteRange != null) {
            objectRequestBuilder.range(byteRange.toHtmlHeader());
        }
        long t0 = System.currentTimeMillis();
        try {
            FileDownload downloadFile = config.transferManager.downloadFile(
                    DownloadFileRequest.builder()
                                       .getObjectRequest(objectRequestBuilder.build())
                                       .destination(dest)
                                       .build());
            downloadFile.completionFuture().join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof SdkException sdkException) {
                if (isMissingKey(sdkException)) {
                    logTrace("<--", "missing");
                    logTrace("hnote right: " + s3Key);
                    log.debug("Blob {} does not exist", debugObject);
                    return false;
                }
                throw e;
            }
        }

        long dtms = System.currentTimeMillis() - t0;

        logTrace("<-", "read " + Files.size(dest) + " bytes");
        logTrace("hnote right: " + s3Key);
        log.debug("Read {} in {} ms", debugObject, dtms);
        if (logs3dl.isDebugEnabled()) {
            String message = String.format("Read %s (%d bytes) in %.3f s", debugObject, Files.size(dest),
                    dtms / 1000.0);
            logs3dl.debug(message, new Exception("DEBUGGING STACK TRACE"));
        }
        return true;
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return !config.useClientSideEncryption && sourceStore.unwrap() instanceof S3BlobStore s3SrcStore
                && !s3SrcStore.config.useClientSideEncryption;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        BlobStore unwrappedSourceStore = sourceStore.unwrap();
        if (unwrappedSourceStore instanceof S3BlobStore srcS3Store && !config.useClientSideEncryption
                && !srcS3Store.config.useClientSideEncryption) {
            // attempt direct S3-level copy
            try {
                String returnedKey = copyOrMoveBlob(key, srcS3Store, sourceKey, atomicMove);
                if (returnedKey != null) {
                    return returnedKey;
                }
            } catch (SdkException e) {
                if (isMissingKey(e)) {
                    logTrace("<--", "missing");
                    // source not found
                    return null;
                }
                throw new IOException(e);
            }
            // fall through if not copied
        }

        return copyOrMoveBlobGeneric(key, sourceStore, sourceKey, atomicMove);
    }

    /**
     * @return {@code null} if generic copy is needed
     * @throws AwsServiceException if the source is missing
     */
    protected String copyOrMoveBlob(String key, S3BlobStore sourceBlobStore, String sourceKey, boolean move)
            throws S3Exception { // NOSONAR
        sourceKey = getBlobKeyReplacement(sourceKey);
        var srcs3Key = new S3BlobKey(sourceBlobStore.config, sourceKey);
        if (key == null) {
            // fast digest compute or trigger async digest computation
            String digest;
            if (keyStrategy instanceof KeyStrategyDigest keyStrategyDigest
                    && keyStrategyDigest.digestAlgorithm.equals("MD5") //
                    && (digest = sourceBlobStore.getMD5DigestFromETag(srcs3Key.bucketKey())) != null) {
                // we have a usable MD5 digest
                key = digest;
            } else {
                // async: use a random key for now; and do async computation of real digest
                key = randomString();
                notifyAsyncDigest(key);
            }
        }

        var s3Key = new S3BlobKey(config, key);
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("Copying s3://{}/{} to s3://{}/{}", () -> sourceBlobStore.bucketName, srcs3Key::toString,
                    () -> bucketName, s3Key::bucketKey);
        }

        if (getKeyStrategy().useDeDuplication() && bucketKeyExists(s3Key.bucketKey())) {
            if (bucketKeyHasDefaultStorageClass(s3Key.bucketKey())) {
                return key;
            } else {
                log.warn("Restoring blob: s3://{}/{} by copy", () -> bucketName, s3Key::bucketKey);
            }
        }

        // copy the blob
        try {
            String versionId = copyOrMoveBlob(sourceBlobStore.config, srcs3Key, config, s3Key.bucketKey(), move);
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("Copied s3://{}/{} to s3://{}/{} in {}ms", () -> sourceBlobStore.bucketName,
                        srcs3Key::toString, () -> bucketName, s3Key::bucketKey, () -> dtms);
            }
            return versionId == null ? key : key + VER_SEP + versionId;
        } catch (CompletionException e) {
            logTrace("<--", "ERROR");
            String message = "Direct copy failed from s3://" + sourceBlobStore.bucketName + "/" + srcs3Key + " to s3://"
                    + bucketName + "/" + s3Key;
            log.warn("{}, falling back to slow copy: {}", message, e.getMessage());
            log.debug(message, e);
            return null;
        }
    }

    /**
     * Gets the MD5 of an object from its ETag, if possible.
     *
     * @since 11.5
     */
    protected String getMD5DigestFromETag(String bucketKey) {
        // check if source ETag is applicable
        HeadObjectResponse response;
        response = amazonS3.headObject(b -> b.key(bucketKey).bucket(config.bucketName));
        String eTag = sanitizeETag(response.eTag());
        // with multipart uploaded the ETag is not a digest
        if (eTag.contains("-")) {
            return null;
        }
        // with SSE-KMS the ETag is not the MD5 of the object data
        if (ServerSideEncryption.AWS_KMS == response.serverSideEncryption()) {
            return null;
        }
        // ok the ETag is an MD5 digest
        return eTag;
    }

    /** Returns the version id, or {@code null}. */
    protected String copyOrMoveBlob(S3BlobStoreConfiguration sourceConfig, S3BlobKey srcs3Key,
            S3BlobStoreConfiguration destinationConfig, String destinationKey, boolean move) {
        logTrace("->", "copyObject");
        logTrace("hnote right: " + srcs3Key + " to " + destinationKey);
        Copy copy = destinationConfig.transferManager.copy(cb -> cb.copyObjectRequest(b -> {
            b.sourceBucket(sourceConfig.bucketName)
             .sourceKey(srcs3Key.bucketKey())
             .sourceVersionId(srcs3Key.versionId())
             .destinationBucket(destinationConfig.bucketName)
             .destinationKey(destinationKey)
             .storageClass(destinationConfig.storageClass);
            if (destinationConfig.useServerSideEncryption) {
                // server-side encryption
                if (isNotBlank(destinationConfig.serverSideKMSKeyID)) {
                    // SSE-KMS
                    b.ssekmsKeyId(config.serverSideKMSKeyID);
                } else {
                    // SSE-S3
                    b.sseCustomerAlgorithm(ServerSideEncryption.AES256.toString());
                }
            }
        }));
        CompletedCopy completedCopy = copy.completionFuture().join();

        // if we don't want to use versions, ignore them even though the bucket may be versioned
        String versionId = hasVersioning() ? completedCopy.response().versionId() : null;
        logTrace("<--", "copied");
        if (versionId != null) {
            logTrace("hnote right: v=" + versionId);
        }
        if (move) {
            logTrace("->", "deleteObject");
            logTrace("hnote right: " + srcs3Key);
            amazonS3.deleteObject(b -> b.bucket(sourceConfig.bucketName).key(srcs3Key.bucketKey()));
        }
        return versionId;
    }

    protected String copyOrMoveBlobGeneric(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        Path tmp = null;
        try {
            OptionalOrUnknown<Path> fileOpt = sourceStore.getFile(sourceKey);
            Path file;
            String fileTraceSource;
            if (fileOpt.isPresent()) {
                file = fileOpt.get();
                fileTraceSource = sourceStore.getName();
            } else {
                // no local file available, read from source
                tmp = Files.createTempFile("bin_", ".tmp");
                logTrace(null, "->", "tmp", "write");
                logTrace("hnote right: " + tmp.getFileName());
                boolean found = sourceStore.readBlob(sourceKey, tmp);
                if (!found) {
                    return null;
                }
                file = tmp;
                fileTraceSource = "tmp";
            }
            String versionId = writeFile(key, file, null, fileTraceSource); // always atomic
            if (atomicMove) {
                sourceStore.deleteBlob(sourceKey);
            }
            return versionId == null ? key : key + VER_SEP + versionId;
        } finally {
            if (tmp != null) {
                try {
                    logTrace("tmp", "-->", "tmp", "delete");
                    logTrace("hnote right: " + tmp.getFileName());
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }
    }

    @Override
    public void writeBlobProperties(BlobUpdateContext blobUpdateContext) throws IOException {
        String key = blobUpdateContext.key;
        key = getBlobKeyReplacement(key);
        var s3Key = new S3BlobKey(config, key);
        try {
            if (config.retentionEnabled) {
                if (blobUpdateContext.updateLegalHold != null) {
                    if (!s3Key.isVersioned()) {
                        throw new IOException("Cannot set legal hold on non-versioned blob");
                    }
                    boolean hold = blobUpdateContext.updateLegalHold.hold;
                    ObjectLockLegalHoldStatus status = hold ? ON : OFF;
                    logTrace("->", "setObjectLegalHold");
                    logTrace("hnote right: " + s3Key);
                    logTrace("rnote right: " + status);
                    amazonS3.putObjectLegalHold(pb -> pb.bucket(bucketName)
                                                        .key(s3Key.bucketKey())
                                                        .versionId(s3Key.versionId())
                                                        .legalHold(b -> b.status(status)));
                }
                if (blobUpdateContext.updateRetainUntil != null) {
                    if (!s3Key.isVersioned()) {
                        throw new IOException("Cannot set retention on non-versioned blob");
                    }
                    Optional.ofNullable(blobUpdateContext.updateRetainUntil.retainUntil)
                            .map(Calendar::toInstant)
                            .filter(retainUntil -> retainUntil.isAfter(Instant.now()))
                            .ifPresentOrElse(retainUntil -> {
                                logTrace("->", "setObjectRetention");
                                logTrace("hnote right: " + s3Key);
                                logTrace("rnote right: " + retainUntil);
                                amazonS3.putObjectRetention(pb -> pb.bucket(bucketName)
                                                                    .key(s3Key.bucketKey())
                                                                    .versionId(s3Key.versionId())
                                                                    .retention(b -> b.mode(config.retentionMode)
                                                                                     .retainUntilDate(retainUntil)));

                            }, () -> log.debug("Skipping retention at S3 level for key: {}, retainUntil: {}",
                                    s3Key::toString, () -> blobUpdateContext.updateRetainUntil.retainUntil));
                }
            }
            if (blobUpdateContext.coldStorageClass != null) {
                StorageClass storageClass = blobUpdateContext.coldStorageClass.inColdStorage ? StorageClass.GLACIER
                        : config.storageClass;
                logTrace("->", "updateStorageClass");
                logTrace("hnote right: " + s3Key);
                logTrace("rnote right: " + storageClass);
                Copy copy = config.transferManager.copy(
                        cb -> cb.copyObjectRequest(b -> b.sourceBucket(bucketName)
                                                         .sourceKey(s3Key.bucketKey())
                                                         .destinationBucket(bucketName)
                                                         .destinationKey(s3Key.bucketKey())
                                                         .storageClass(storageClass)
                                                         .sourceVersionId(s3Key.versionId())));
                copy.completionFuture().join();
                // No need to waitForCopyResult when changing storage class
            }
            if (blobUpdateContext.restoreForDuration != null) {
                Duration duration = blobUpdateContext.restoreForDuration.duration;
                // round up duration to days
                int days = (int) duration.plusDays(1).minusSeconds(1).toDays();
                amazonS3.restoreObject(b -> b.bucket(bucketName)
                                             .key(s3Key.bucketKey())
                                             .versionId(s3Key.versionId())
                                             .restoreRequest(rrb -> rrb.days(days)));
            }
        } catch (SdkException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
                log.debug("Blob s3://{}/{} does not exist", bucketName, s3Key.bucketKey());
            }
            throw new IOException(e);
        }
    }

    @Override
    public void deleteBlob(String key) {
        var s3Key = new S3BlobKey(config, key);
        try {
            if (!s3Key.isVersioned()) {
                logTrace("->", "deleteObject");
                logTrace("hnote right: " + s3Key);
                amazonS3.deleteObject(b -> b.bucket(bucketName).key(s3Key.bucketKey()));
            } else {
                logTrace("->", "deleteVersion");
                logTrace("hnote right: " + s3Key);
                amazonS3.deleteObject(b -> b.bucket(bucketName).key(s3Key.bucketKey()).versionId(s3Key.versionId()));
            }
        } catch (SdkException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
            } else {
                log.warn("Cannot delete: s3://{}/{}", bucketName, s3Key, e);
            }
        }
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    /**
     * Garbage collector for S3 binaries that stores the marked (in use) binaries in memory.
     */
    public class S3BlobGarbageCollector extends AbstractBlobGarbageCollector {

        protected static final int WARN_OBJECTS_THRESHOLD = 100_000;

        @Override
        public String getId() {
            return "s3:" + bucketName + "/" + bucketPrefix;
        }

        @Override
        public void computeToDelete() {
            // list S3 objects in the bucket
            boolean useDeDuplication = keyStrategy.useDeDuplication();
            toDelete = new HashSet<>();
            int prefixLength = bucketPrefix.length();
            logTrace("->", "listObjects on " + getId());
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                                                                       .bucket(bucketName)
                                                                       .prefix(bucketPrefix);
            if (config.getSubDirsDepth() == 0) {
                // use delimiter to avoid useless listing of objects in "subdirectories"
                builder.delimiter(DELIMITER);
            }
            ListObjectsV2Request listReq = builder.build();

            amazonS3.listObjectsV2Paginator(listReq).stream().flatMap(r -> r.contents().stream()).forEach(content -> {
                String path = content.key().substring(prefixLength);
                String key = config.getSubDirsDepth() == 0 ? path : pathStrategy.getKeyForPath(path);
                if (key == null) {
                    return;
                }
                if (useDeDuplication && !((KeyStrategyDigest) keyStrategy).isValidDigest(key)) {
                    // ignore files that cannot be digests, for safety
                    return;
                }
                long length = content.size();
                status.sizeBinaries += length;
                status.numBinaries++;
                toDelete.add(key);
                if (toDelete.size() % WARN_OBJECTS_THRESHOLD == 0) {
                    log.warn("Listing {} in progress, {} objects ...", getId(), toDelete.size());
                }
            });
            logTrace("<--", status.numBinaries + " objects");
            if (toDelete.size() >= WARN_OBJECTS_THRESHOLD) {
                log.warn("Listing {} completed, {} objects.", getId(), toDelete.size());
            }
        }

        /**
         * @since 2021.13
         */
        @Override
        public void mark(String key) {
            int seppos = key.indexOf(VER_SEP);
            if (seppos > 0) {
                key = key.substring(0, seppos);
            }
            toDelete.remove(key);
        }

        @Override
        public void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (String key : toDelete) {
                long length = lengthOfBlob(key);
                if (length < 0) {
                    // shouldn't happen except if blob concurrently removed
                    continue;
                }
                status.sizeBinariesGC += length;
                status.numBinariesGC++;
                status.sizeBinaries -= length;
                status.numBinaries--;
                if (delete) {
                    deleteBlob(key);
                }
            }
        }
    }

}
