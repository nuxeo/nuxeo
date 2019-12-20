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

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.blob.AbstractBlobGarbageCollector;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobContext;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobUpdateContext;
import org.nuxeo.ecm.core.blob.BlobWriteContext;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.EncryptedPutObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectLockLegalHold;
import com.amazonaws.services.s3.model.ObjectLockLegalHoldStatus;
import com.amazonaws.services.s3.model.ObjectLockRetention;
import com.amazonaws.services.s3.model.ObjectLockRetentionMode;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.model.SetObjectLegalHoldRequest;
import com.amazonaws.services.s3.model.SetObjectRetentionRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

/**
 * Blob storage in S3.
 *
 * @since 11.1
 */
public class S3BlobStore extends AbstractBlobStore {

    private static final Logger log = LogManager.getLogger(S3BlobStore.class);

    /** Separator between object key and version id in the returned key. */
    protected static final char VER_SEP = '@';

    // x-amz-meta-username header
    protected static final String USER_METADATA_USERNAME = "username";

    protected final S3BlobStoreConfiguration config;

    protected final AmazonS3 amazonS3;

    protected final String bucketName;

    protected final String bucketPrefix;

    // note, we may choose to not use versions even in a versioned bucket
    // if we want the bucket to record and keep old versions for us
    /** If true, include the object version in the key. */
    protected final boolean useVersion;

    // TODO configure
    protected ObjectLockRetentionMode objectLockRetentionMode = ObjectLockRetentionMode.GOVERNANCE;

    protected final BinaryGarbageCollector gc;

    public S3BlobStore(String name, S3BlobStoreConfiguration config, KeyStrategy keyStrategy) {
        super(name, keyStrategy);
        this.config = config;
        amazonS3 = config.amazonS3;
        bucketName = config.bucketName;
        bucketPrefix = config.bucketPrefix;
        useVersion = isBucketVersioningEnabled() && !keyStrategy.useDeDuplication();
        gc = new S3BlobGarbageCollector();
    }

    public S3BlobStore getS3BinaryManager() {
        return S3BlobStore.this;
    }

    protected static boolean isMissingKey(AmazonServiceException e) {
        return e.getStatusCode() == 404 || "NoSuchKey".equals(e.getErrorCode()) || "Not Found".equals(e.getMessage());
    }

    protected boolean isBucketVersioningEnabled() {
        BucketVersioningConfiguration v = amazonS3.getBucketVersioningConfiguration(bucketName);
        // if versioning is suspended, created objects won't have versions
        return v.getStatus().equals(BucketVersioningConfiguration.ENABLED);
    }

    @Override
    public boolean hasVersioning() {
        return useVersion;
    }

    @Override
    public String writeBlob(BlobWriteContext blobWriteContext) throws IOException {

        // detect copy from another S3 blob provider, to use direct S3-level copy
        BlobContext blobContext = blobWriteContext.blobContext;
        Blob blob = blobContext.blob;
        String copiedKey = copyBlob(blob);
        if (copiedKey != null) {
            return copiedKey;
        }

        Path file;
        String fileTraceSource;
        Path tmp = null;
        try {
            Path blobWriteContextFile = blobWriteContext.getFile();
            if (blobWriteContextFile != null) {
                // we have a file, assume that the caller already observed the write
                file = blobWriteContextFile;
                fileTraceSource = "Nuxeo";
            } else {
                // no transfer to a file was done yet (no caching)
                // we may be able to use the blob's underlying file, if not pure streaming
                File blobFile = blob.getFile();
                if (blobFile != null) {
                    // otherwise use blob file directly
                    if (blobWriteContext.writeObserver != null) {
                        // but we must still run the writes through the write observer
                        transfer(blobWriteContext, NULL_OUTPUT_STREAM);
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
        String bucketKey = bucketPrefix + key;
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("Writing s3://" + bucketName + "/" + bucketKey);
        }

        if (getKeyStrategy().useDeDuplication() && exists(bucketKey)) {
            return null; // no key version used with deduplication
        }

        PutObjectRequest putObjectRequest;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        if (config.useClientSideEncryption) {
            // client-side encryption
            putObjectRequest = new EncryptedPutObjectRequest(bucketName, bucketKey, file.toFile());
        } else {
            // server-side encryption
            putObjectRequest = new PutObjectRequest(bucketName, bucketKey, file.toFile());
            if (config.useServerSideEncryption) {
                if (isNotBlank(config.serverSideKMSKeyID)) {
                    // SSE-KMS
                    SSEAwsKeyManagementParams params = new SSEAwsKeyManagementParams(config.serverSideKMSKeyID);
                    putObjectRequest.setSSEAwsKeyManagementParams(params);
                } else {
                    // SSE-S3
                    objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                }
                // TODO SSE-C
            }
        }
        setMetadata(objectMetadata, blobContext);
        putObjectRequest.setMetadata(objectMetadata);
        logTrace(fileTraceSource, "->", null, "write " + Files.size(file) + " bytes");
        logTrace("hnote right: " + bucketKey);
        Upload upload = config.transferManager.upload(putObjectRequest);
        try {
            UploadResult uploadResult = upload.waitForUploadResult();
            // if we don't want to use versions, ignore them even though the bucket may be versioned
            String versionId = useVersion ? uploadResult.getVersionId() : null;
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("Wrote s3://" + bucketName + "/" + bucketKey + " in " + dtms + "ms");
            }
            if (versionId != null) {
                logTrace("<--", "v=" + versionId);
            }
            return versionId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected void setMetadata(ObjectMetadata objectMetadata, BlobContext blobContext) {
        if (blobContext != null) {
            Blob blob = blobContext.blob;
            String filename = blob.getFilename();
            if (filename != null) {
                String contentDisposition = RFC2231.encodeContentDisposition(filename, false, null);
                objectMetadata.setContentDisposition(contentDisposition);
            }
            String contentType = DownloadHelper.getContentTypeHeader(blob);
            objectMetadata.setContentType(contentType);
        }
        if (config.metadataAddUsername) {
            NuxeoPrincipal principal = ClientLoginModule.getCurrentPrincipal();
            if (principal != null && !(principal instanceof SystemPrincipal)) {
                String username = principal.getActingUser();
                if (username != null) {
                    Map<String, String> userMetadata = Collections.singletonMap(USER_METADATA_USERNAME, username);
                    objectMetadata.setUserMetadata(userMetadata);
                }
            }
        }
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.unknown();
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        return OptionalOrUnknown.unknown();
    }

    protected boolean exists(String bucketKey) {
        try {
            amazonS3.getObjectMetadata(bucketName, bucketKey);
            if (log.isDebugEnabled()) {
                log.debug("Blob s3://" + bucketName + "/" + bucketKey + " already exists");
            }
            logTrace("<--", "exists");
            logTrace("hnote right: " + bucketKey);
            return true;
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
                logTrace("hnote right: " + bucketKey);
                return false;
            }
            throw e;
        }
    }

    // used for tests
    protected void clearBucket() {
        logTrace("group ClearBucket");
        ObjectListing list = null;
        long n = 0;
        do {
            if (list == null) {
                logTrace("->", "listObjects");
                list = amazonS3.listObjects(bucketName);
            } else {
                list = amazonS3.listNextBatchOfObjects(list);
            }
            for (S3ObjectSummary summary : list.getObjectSummaries()) {
                amazonS3.deleteObject(bucketName, summary.getKey());
                n++;
            }
        } while (list.isTruncated());
        if (n > 0) {
            logTrace("loop " + n + " objects");
            logTrace("->", "deleteObject");
            logTrace("end");
        }
        VersionListing vlist = null;
        long vn = 0;
        do {
            if (vlist == null) {
                logTrace("->", "listVersions");
                vlist = amazonS3.listVersions(new ListVersionsRequest().withBucketName(bucketName));
            } else {
                vlist = amazonS3.listNextBatchOfVersions(vlist);

            }
            for (S3VersionSummary vsummary : vlist.getVersionSummaries()) {
                amazonS3.deleteVersion(bucketName, vsummary.getKey(), vsummary.getVersionId());
                vn++;
            }
        } while (vlist.isTruncated());
        if (vn > 0) {
            logTrace("loop " + vn + " versions");
            logTrace("->", "deleteVersion");
            logTrace("end");
        }
        logTrace("end");
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
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
        String bucketKey = bucketPrefix + objectKey;
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("Reading s3://" + bucketName + "/" + bucketKey);
        }
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, bucketKey, versionId);
            Download download = config.transferManager.download(getObjectRequest, dest.toFile());
            download.waitForCompletion();
            logTrace("<-", "read " + Files.size(dest) + " bytes");
            logTrace("hnote right: " + bucketKey + (versionId == null ? "" : " v=" + versionId));
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("Read s3://" + bucketName + "/" + bucketKey + " in " + dtms + "ms");
            }
            if (config.useClientSideEncryption) {
                // can't efficiently check the decrypted digest
                return true;
            }
            String expectedDigest = getKeyStrategy().getDigestFromKey(objectKey);
            if (expectedDigest != null) {
                checkDigest(expectedDigest, download, dest);
            }
            // else nothing to compare to, key is not digest-based
            return true;
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
                logTrace("hnote right: " + bucketKey + (versionId == null ? "" : " v=" + versionId));
                if (log.isDebugEnabled()) {
                    log.debug("Blob s3://" + bucketName + "/" + bucketKey + " does not exist");
                }
                return false;
            }
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected void checkDigest(String expectedDigest, Download download, Path file) throws IOException {
        if (!expectedDigest.equals(download.getObjectMetadata().getETag())) {
            // if our digest algorithm is not MD5 (so the ETag can never match),
            // or in case of a multipart upload (where the ETag may not be the MD5),
            // check manually the object integrity
            // TODO this is costly and it should possible to deactivate it
            String digest = new DigestUtils(config.digestConfiguration.digestAlgorithm).digestAsHex(file.toFile());
            if (!digest.equals(expectedDigest)) {
                String msg = "Invalid S3 object digest, expected=" + expectedDigest + " actual=" + digest;
                log.warn(msg);
                throw new IOException(msg);
            }
        }
    }

    /**
     * Copies the blob as a direct S3 operation, if possible.
     *
     * @return the key, or {@code null} if no copy was done
     */
    protected String copyBlob(Blob blob) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            // not a managed blob
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = Framework.getService(BlobManager.class)
                                             .getBlobProvider(managedBlob.getProviderId());
        if (!getKeyStrategy().useDeDuplication()) {
            // key is not digest-based, so we don't know the destination key
            return null;
        }
        if (!(blobProvider instanceof S3BlobProvider)) {
            // not an S3 blob
            return null;
        }
        S3BlobStore sourceStore = (S3BlobStore) ((S3BlobProvider) blobProvider).store.unwrap();
        if (!sourceStore.getKeyStrategy().equals(getKeyStrategy())) {
            // not the same digest
            return null;
        }
        // use S3-level copy as the source blob provider is also S3
        String sourceKey = stripBlobKeyPrefix(managedBlob.getKey());
        String key = sourceKey; // because same key strategy
        boolean found = copyBlob(key, sourceStore, sourceKey, false);
        if (!found) {
            throw new IOException("Cannot find source blob: " + sourceKey);
        }
        return key;
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return sourceStore instanceof S3BlobStore;
    }

    @Override
    public boolean copyBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        BlobStore unwrappedSourceStore = sourceStore.unwrap();
        if (unwrappedSourceStore instanceof S3BlobStore) {
            // attempt direct S3-level copy
            S3BlobStore sourceS3BlobStore = (S3BlobStore) unwrappedSourceStore;
            try {
                boolean copied = copyBlob(key, sourceS3BlobStore, sourceKey, atomicMove);
                if (copied) {
                    return true;
                }
            } catch (AmazonServiceException e) {
                if (isMissingKey(e)) {
                    logTrace("<--", "missing");
                    // source not found
                    return false;
                }
                throw new IOException(e);
            }
            // fall through if not copied
        }
        return copyBlobGeneric(key, sourceStore, sourceKey, atomicMove);
    }

    /**
     * @return {@code false} if generic copy is needed
     * @throws AmazonServiceException if the source is missing
     */
    protected boolean copyBlob(String key, S3BlobStore sourceBlobStore, String sourceKey, boolean move)
            throws AmazonServiceException { // NOSONAR
        String sourceBucketName = sourceBlobStore.bucketName;
        String sourceBucketKey = sourceBlobStore.bucketPrefix + sourceKey;
        String bucketKey = bucketPrefix + key;

        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("Copying s3://" + sourceBucketName + "/" + sourceBucketKey + " to s3://" + bucketName + "/"
                    + bucketKey);
        }

        if (getKeyStrategy().useDeDuplication() && exists(bucketKey)) {
            return true;
        }

        // copy the blob
        logTrace("->", "getObjectMetadata");
        ObjectMetadata sourceMetadata = amazonS3.getObjectMetadata(sourceBucketName, sourceBucketKey);
        // don't catch AmazonServiceException if missing, caller will do it
        long length = sourceMetadata.getContentLength();
        logTrace("<-", length + " bytes");
        try {

            copyBlob(sourceBlobStore.config, sourceBucketKey, config, bucketKey, move);

            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("Copied s3://" + sourceBucketName + "/" + sourceBucketKey + " to s3://" + bucketName + "/"
                        + bucketKey + " in " + dtms + "ms");
            }
            return true;
        } catch (AmazonServiceException e) {
            logTrace("<--", "ERROR");
            String message = "Direct copy failed from s3://" + sourceBucketName + "/" + sourceBucketKey + " to s3://"
                    + bucketName + "/" + bucketKey + " (" + length + " bytes)";
            log.warn(message + ", falling back to slow copy: " + e.getMessage());
            log.debug(message, e);
            return false;
        }
    }

    protected void copyBlob(S3BlobStoreConfiguration sourceConfig, String sourceKey,
            S3BlobStoreConfiguration destinationConfig, String destinationKey, boolean move) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceConfig.bucketName, sourceKey,
                destinationConfig.bucketName, destinationKey);
        if (destinationConfig.useServerSideEncryption) {
            // server-side encryption
            if (isNotBlank(destinationConfig.serverSideKMSKeyID)) {
                // SSE-KMS
                SSEAwsKeyManagementParams params = new SSEAwsKeyManagementParams(destinationConfig.serverSideKMSKeyID);
                copyObjectRequest.setSSEAwsKeyManagementParams(params);
            } else {
                // SSE-S3
                ObjectMetadata newObjectMetadata = new ObjectMetadata();
                newObjectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                // CopyCallable.populateMetadataWithEncryptionParams will get the rest from the source
                copyObjectRequest.setNewObjectMetadata(newObjectMetadata);
            }
            // TODO SSE-C
        }
        logTrace("->", "copyObject");
        logTrace("hnote right: " + sourceKey + " to " + destinationKey);
        Copy copy = destinationConfig.transferManager.copy(copyObjectRequest, sourceConfig.amazonS3, null);
        try {
            copy.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
        logTrace("<--", "copied");
        if (move) {
            logTrace("->", "deleteObject");
            logTrace("hnote right: " + sourceKey);
            amazonS3.deleteObject(sourceConfig.bucketName, sourceKey);
        }
    }

    protected boolean copyBlobGeneric(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
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
                    return false;
                }
                file = tmp;
                fileTraceSource = "tmp";
            }
            String versionId = writeFile(key, file, null, fileTraceSource); // always atomic
            if (versionId != null) {
                throw new NuxeoException("Cannot copy blob if store has versioning");
            }
            if (atomicMove) {
                sourceStore.deleteBlob(sourceKey);
            }
            return true;
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
        String bucketKey = bucketPrefix + objectKey;
        try {
            if (blobUpdateContext.updateRetainUntil != null) {
                if (versionId == null) {
                    throw new IOException("Cannot set retention on non-versioned blob");
                }
                Calendar retainUntil = blobUpdateContext.updateRetainUntil.retainUntil;
                Date retainUntilDate = retainUntil == null ? null : retainUntil.getTime();
                ObjectLockRetention retention = new ObjectLockRetention();
                retention.withMode(objectLockRetentionMode) //
                         .withRetainUntilDate(retainUntilDate);
                SetObjectRetentionRequest request = new SetObjectRetentionRequest();
                request.withBucketName(bucketName) //
                       .withKey(bucketKey)
                       .withVersionId(versionId)
                       .withRetention(retention);
                logTrace("->", "setObjectRetention");
                logTrace("hnote right: " + bucketKey + "v=" + versionId);
                logTrace("rnote right: " + (retainUntil == null ? "null" : retainUntil.toInstant().toString()));
                amazonS3.setObjectRetention(request);
            }
            if (blobUpdateContext.updateLegalHold != null) {
                if (versionId == null) {
                    throw new IOException("Cannot set legal hold on non-versioned blob");
                }
                boolean hold = blobUpdateContext.updateLegalHold.hold;
                ObjectLockLegalHoldStatus status = hold ? ObjectLockLegalHoldStatus.ON : ObjectLockLegalHoldStatus.OFF;
                ObjectLockLegalHold legalHold = new ObjectLockLegalHold().withStatus(status);
                SetObjectLegalHoldRequest request = new SetObjectLegalHoldRequest();
                request.withBucketName(bucketName) //
                       .withKey(bucketKey)
                       .withVersionId(versionId)
                       .withLegalHold(legalHold);
                logTrace("->", "setObjectLegalHold");
                logTrace("hnote right: " + bucketKey + "v=" + versionId);
                logTrace("rnote right: " + status.toString());
                amazonS3.setObjectLegalHold(request);
            }
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
                if (log.isDebugEnabled()) {
                    log.debug("Blob s3://" + bucketName + "/" + bucketKey + " does not exist");
                }
            }
            throw new IOException(e);
        }
    }

    @Override
    public void deleteBlob(String key) {
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
        String bucketKey = bucketPrefix + objectKey;
        try {
            if (versionId == null) {
                logTrace("->", "deleteObject");
                logTrace("hnote right: " + bucketKey);
                amazonS3.deleteObject(bucketName, bucketKey);
            } else {
                logTrace("->", "deleteVersion");
                logTrace("hnote right: " + bucketKey + " v=" + versionId);
                amazonS3.deleteVersion(bucketName, bucketKey, versionId);
            }
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
            } else {
                log.warn(e, e);
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

        @Override
        public String getId() {
            return "s3:" + bucketName + "/" + bucketPrefix;
        }

        @Override
        public Set<String> getUnmarkedBlobsAndUpdateStatus() {
            // list S3 objects in the bucket
            // record those not marked
            boolean useDeDuplication = keyStrategy.useDeDuplication();
            Set<String> unmarked = new HashSet<>();
            ObjectListing list = null;
            int prefixLength = bucketPrefix.length();
            logTrace("->", "listObjects");
            do {
                if (list == null) {
                    // use delimiter to avoid useless listing of objects in "subdirectories"
                    ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName, bucketPrefix, null,
                            S3BlobStoreConfiguration.DELIMITER, null);
                    list = amazonS3.listObjects(listObjectsRequest);
                } else {
                    list = amazonS3.listNextBatchOfObjects(list);
                }
                for (S3ObjectSummary summary : list.getObjectSummaries()) {
                    String key = summary.getKey().substring(prefixLength);
                    if (useDeDuplication) {
                        if (!config.digestConfiguration.isValidDigest(key)) {
                            // ignore files that cannot be digests, for safety
                            continue;
                        }
                    }
                    long length = summary.getSize();
                    if (marked.contains(key)) {
                        status.numBinaries++;
                        status.sizeBinaries += length;
                    } else {
                        status.numBinariesGC++;
                        status.sizeBinariesGC += length;
                        // record file to delete
                        unmarked.add(key);
                    }
                }
            } while (list.isTruncated());
            logTrace("<--", (status.numBinaries + status.numBinariesGC) + " objects");
            return unmarked;
        }

        @Override
        public void removeBlobs(Set<String> keys) {
            keys.forEach(S3BlobStore.this::deleteBlob);
        }
    }

}
