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
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.DELIMITER;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;
import static org.nuxeo.ecm.core.blob.KeyStrategy.VER_SEP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableObject;
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
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobUpdateContext;
import org.nuxeo.ecm.core.blob.BlobWriteContext;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.KeyStrategyDocId;
import org.nuxeo.ecm.core.blob.PathStrategy;
import org.nuxeo.ecm.core.blob.PathStrategyFlat;
import org.nuxeo.ecm.core.blob.PathStrategySubDirs;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkBaseException;
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
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.model.SetObjectLegalHoldRequest;
import com.amazonaws.services.s3.model.SetObjectRetentionRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.CopyResult;
import com.amazonaws.services.s3.transfer.model.UploadResult;

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

    protected final AmazonS3 amazonS3;

    protected final String bucketName;

    protected final String bucketPrefix;

    protected final PathStrategy pathStrategy;

    protected final boolean pathSeparatorIsBackslash;

    protected final boolean allowByteRange;

    // note, we may choose to not use versions even in a versioned bucket
    // if we want the bucket to record and keep old versions for us
    /** If true, include the object version in the key. */
    protected final boolean useVersion;

    protected volatile Boolean useAsyncDigest;

    protected final BinaryGarbageCollector gc;

    /** @deprecated since 11.5 */
    @Deprecated
    public S3BlobStore(String name, S3BlobStoreConfiguration config, KeyStrategy keyStrategy) {
        this(null, name, config, keyStrategy);
    }

    /** @since 11.5 */
    public S3BlobStore(String blobProviderId, String name, S3BlobStoreConfiguration config, KeyStrategy keyStrategy) {
        super(blobProviderId, name, keyStrategy);
        this.config = config;
        amazonS3 = config.amazonS3;
        bucketName = config.bucketName;
        bucketPrefix = config.bucketPrefix;
        Path p = Paths.get(bucketPrefix);
        int subDirsDepth = config.getSubDirsDepth();
        if (subDirsDepth == 0) {
            // pathStrategy is not used when subDirsDepth=0 because a bucketPrefix could be in the key - NXP-30632
            pathStrategy = new PathStrategyFlat(p);
        } else {
            pathStrategy = new PathStrategySubDirs(p, subDirsDepth);
        }
        pathSeparatorIsBackslash = FileSystems.getDefault().getSeparator().equals("\\");
        allowByteRange = config.getBooleanProperty(ALLOW_BYTE_RANGE);
        // don't use versions if we use deduplication (including managed case)
        useVersion = isBucketVersioningEnabled() && keyStrategy instanceof KeyStrategyDocId;
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
        // this allows to retrieve blobs created with a bucketPrefix in the key - NXP-30632
        // this is a workaround for incorrectly written keys
        if (config.getSubDirsDepth() == 0) {
            return bucketPrefix + key;
        }
        String path = pathStrategy.getPathForKey(key).toString();
        if (pathSeparatorIsBackslash) {
            // correct for our abuse of Path under Windows
            path = path.replace("\\", DELIMITER);
        }
        return path;
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
        String bucketKey = bucketKey(key);
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
        } catch (SdkBaseException e) {
            // catch SdkBaseException and not just AmazonServiceException
            throw new NuxeoException("Failed to write blob: " + key, e);
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
            logTrace("-->", "getObjectMetadata");
            logTrace("hnote right: " + bucketKey);
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName, bucketKey);
            if (log.isDebugEnabled()) {
                log.debug("Blob s3://" + bucketName + "/" + bucketKey + " already exists");
            }
            logTrace("<--", "exists (" + metadata.getContentLength() + " bytes)");
            return true;
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
                return false;
            }
            throw e;
        }
    }

    /** @return object length, or -1 if missing */
    protected long lengthOfBlob(String key) {
        String bucketKey = bucketKey(key);
        try {
            logTrace("-->", "getObjectMetadata");
            logTrace("hnote right: " + bucketKey);
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName, bucketKey);
            long length = metadata.getContentLength();
            logTrace("<--", "exists (" + length + " bytes)");
            return length;
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
            } else  {
                log.warn("Cannot get length of: s3://" + bucketName + "/" + bucketKey, e);
            }
            return -1;
        }
    }

    @Override
    public void clear() {
        logTrace("group ClearBucket");
        ObjectListing list = null;
        long n = 0;
        do {
            if (list == null) {
                logTrace("->", "listObjects");
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
                                                                                .withPrefix(bucketPrefix);
                if (config.getSubDirsDepth() == 0) {
                    // use delimiter to avoid useless listing of objects in "subdirectories"
                    listObjectsRequest.setDelimiter(DELIMITER);
                }
                list = amazonS3.listObjects(listObjectsRequest);
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
                ListVersionsRequest listVersionsRequest = new ListVersionsRequest().withBucketName(bucketName)
                                                                                   .withPrefix(bucketPrefix);
                if (config.getSubDirsDepth() == 0) {
                    listVersionsRequest.setDelimiter(DELIMITER);
                }
                vlist = amazonS3.listVersions(listVersionsRequest);
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
        ByteRange byteRange;
        if (allowByteRange) {
            MutableObject<String> keyHolder = new MutableObject<>(key);
            byteRange = getByteRangeFromKey(keyHolder);
            key = keyHolder.getValue();
        } else {
            byteRange = null;
        }
        key = getBlobKeyReplacement(key);
        String objectKey;
        String versionId;
        int seppos;
        if (useVersion && (seppos = key.indexOf(VER_SEP)) > 0) {
            objectKey = key.substring(0, seppos);
            versionId = key.substring(seppos + 1);
        } else {
            objectKey = key;
            versionId = null;
        }
        String bucketKey = bucketKey(objectKey);
        String debugKey = bucketKey + (versionId == null ? "" : "@" + versionId);
        String debugObject = "s3://" + bucketName + "/" + debugKey;
        try {
            log.debug("Reading {}", debugObject);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, bucketKey, versionId);
            if (byteRange != null) {
                getObjectRequest.setRange(byteRange.getStart(), byteRange.getEnd());
            }
            long t0 = System.currentTimeMillis();
            Download download = config.transferManager.download(getObjectRequest, dest.toFile());
            download.waitForCompletion();
            long dtms = System.currentTimeMillis() - t0;

            logTrace("<-", "read " + Files.size(dest) + " bytes");
            logTrace("hnote right: " + debugKey);
            log.debug("Read {} in {} ms", debugObject, dtms);
            if (logs3dl.isDebugEnabled()) {
                String message = String.format("Read %s (%d bytes) in %.3f s", debugObject, Files.size(dest),
                        dtms / 1000.0);
                logs3dl.debug(message, new Exception("DEBUGGING STACK TRACE"));
            }
            return true;
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
                logTrace("hnote right: " + debugKey);
                log.debug("Blob {} does not exist", debugObject);
                return false;
            }
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return sourceStore.unwrap() instanceof S3BlobStore;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        BlobStore unwrappedSourceStore = sourceStore.unwrap();
        if (unwrappedSourceStore instanceof S3BlobStore) {
            // attempt direct S3-level copy
            S3BlobStore sourceS3BlobStore = (S3BlobStore) unwrappedSourceStore;
            try {
                String returnedKey = copyOrMoveBlob(key, sourceS3BlobStore, sourceKey, atomicMove);
                if (returnedKey != null) {
                    return returnedKey;
                }
            } catch (AmazonServiceException e) {
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

    /** @deprecated since 11.5, use {@link #copyOrMoveBlob(String, S3BlobStore, String, boolean)} instead */
    @Deprecated
    protected boolean copyBlob(String key, S3BlobStore sourceBlobStore, String sourceKey, boolean move)
            throws AmazonServiceException { // NOSONAR
        return copyOrMoveBlob(key, sourceBlobStore, sourceKey, move) != null;
    }

    /**
     * @return {@code null} if generic copy is needed
     * @throws AmazonServiceException if the source is missing
     */
    protected String copyOrMoveBlob(String key, S3BlobStore sourceBlobStore, String sourceKey, boolean move)
            throws AmazonServiceException { // NOSONAR
        sourceKey = getBlobKeyReplacement(sourceKey);
        String sourceObjectKey;
        String sourceVersionId;
        int seppos = sourceKey.indexOf(VER_SEP);
        if (seppos < 0) {
            sourceObjectKey = sourceKey;
            sourceVersionId = null;
        } else {
            sourceObjectKey = sourceKey.substring(0, seppos);
            sourceVersionId = sourceKey.substring(seppos + 1);
        }
        String sourceBucketName = sourceBlobStore.bucketName;
        String sourceBucketKey = sourceBlobStore.bucketKey(sourceObjectKey);

        if (key == null) {
            // fast digest compute or trigger async digest computation
            String digest;
            if (keyStrategy instanceof KeyStrategyDigest
                    && ((KeyStrategyDigest) keyStrategy).digestAlgorithm.equals("MD5") //
                    && (digest = sourceBlobStore.getMD5DigestFromETag(sourceBucketKey)) != null) {
                // we have a usable MD5 digest
                key = digest;
            } else {
                // async: use a random key for now; and do async computation of real digest
                key = randomString();
                notifyAsyncDigest(key);
            }
        }

        String bucketKey = bucketKey(key);

        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("Copying s3://" + sourceBucketName + "/" + sourceBucketKey + " to s3://" + bucketName + "/"
                    + bucketKey);
        }

        if (getKeyStrategy().useDeDuplication() && exists(bucketKey)) {
            return key;
        }

        // copy the blob
        try {
            String versionId = copyOrMoveBlob(sourceBlobStore.config, sourceBucketKey, sourceVersionId, config, bucketKey, move);
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("Copied s3://" + sourceBucketName + "/" + sourceBucketKey + " to s3://" + bucketName + "/"
                        + bucketKey + " in " + dtms + "ms");
            }
            return versionId == null ? key : key + VER_SEP + versionId;
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                throw e; // dealt with by the caller
            }
            logTrace("<--", "ERROR");
            String message = "Direct copy failed from s3://" + sourceBucketName + "/" + sourceBucketKey + " to s3://"
                    + bucketName + "/" + bucketKey;
            log.warn(message + ", falling back to slow copy: " + e.getMessage());
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
        ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName, bucketKey);
        String eTag = metadata.getETag();
        // with multipart uploaded the ETag is not a digest
        if (eTag.contains("-")) {
            return null;
        }
        // with SSE-KMS the ETag is not the MD5 of the object data
        if (SSEAlgorithm.KMS.getAlgorithm().equals(metadata.getSSEAlgorithm())) {
            return null;
        }
        // ok the ETag is an MD5 digest
        return eTag;
    }

    /**
     * @deprecated since 11.5, use
     *             {@link #copyOrMoveBlob(S3BlobStoreConfiguration, String, String, S3BlobStoreConfiguration, String, boolean)}
     *             instead
     */
    @Deprecated
    protected void copyBlob(S3BlobStoreConfiguration sourceConfig, String sourceKey,
            S3BlobStoreConfiguration destinationConfig, String destinationKey, boolean move) {
        copyOrMoveBlob(sourceConfig, sourceKey, null, destinationConfig, destinationKey, move);
    }

    /** Returns the version id, or {@code null}. */
    protected String copyOrMoveBlob(S3BlobStoreConfiguration sourceConfig, String sourceKey, String sourceVersionId,
            S3BlobStoreConfiguration destinationConfig, String destinationKey, boolean move) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceConfig.bucketName, sourceKey, sourceVersionId,
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
        logTrace("hnote right: " + sourceKey + (sourceVersionId == null ? "" : "@" + sourceVersionId) + " to "
                + destinationKey);
        Copy copy = destinationConfig.transferManager.copy(copyObjectRequest, sourceConfig.amazonS3, null);
        CopyResult copyResult;
        try {
            copyResult = copy.waitForCopyResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
        // if we don't want to use versions, ignore them even though the bucket may be versioned
        String versionId = useVersion ? copyResult.getVersionId() : null;
        logTrace("<--", "copied");
        if (versionId != null) {
            logTrace("hnote right: v=" + versionId);
        }
        if (move) {
            logTrace("->", "deleteObject");
            logTrace("hnote right: " + sourceKey);
            amazonS3.deleteObject(sourceConfig.bucketName, sourceKey);
        }
        return versionId;
    }

    /** @deprecated since 11.5, use {@link #copyOrMoveBlobGeneric(String, BlobStore, String, boolean)} instead */
    @Deprecated
    protected boolean copyBlobGeneric(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        return copyOrMoveBlobGeneric(key, sourceStore, sourceKey, atomicMove) != null;
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
        String bucketKey = bucketKey(objectKey);
        try {
            if (config.s3RetentionEnabled) {
                if (blobUpdateContext.updateRetainUntil != null) {
                    if (versionId == null) {
                        throw new IOException("Cannot set retention on non-versioned blob");
                    }
                    Calendar retainUntil = blobUpdateContext.updateRetainUntil.retainUntil;
                    Date retainUntilDate = retainUntil == null ? null : retainUntil.getTime();
                    ObjectLockRetention retention = new ObjectLockRetention();
                    retention.withMode(config.retentionMode) //
                             .withRetainUntilDate(retainUntilDate);
                    SetObjectRetentionRequest request = new SetObjectRetentionRequest();
                    request.withBucketName(bucketName) //
                           .withKey(bucketKey)
                           .withVersionId(versionId)
                           .withRetention(retention);
                    logTrace("->", "setObjectRetention");
                    logTrace("hnote right: " + bucketKey + "@" + versionId);
                    logTrace("rnote right: " + (retainUntil == null ? "null" : retainUntil.toInstant().toString()));
                    amazonS3.setObjectRetention(request);
                }
                if (blobUpdateContext.updateLegalHold != null) {
                    if (versionId == null) {
                        throw new IOException("Cannot set legal hold on non-versioned blob");
                    }
                    boolean hold = blobUpdateContext.updateLegalHold.hold;
                    ObjectLockLegalHoldStatus status = hold ? ObjectLockLegalHoldStatus.ON
                            : ObjectLockLegalHoldStatus.OFF;
                    ObjectLockLegalHold legalHold = new ObjectLockLegalHold().withStatus(status);
                    SetObjectLegalHoldRequest request = new SetObjectLegalHoldRequest();
                    request.withBucketName(bucketName) //
                           .withKey(bucketKey)
                           .withVersionId(versionId)
                           .withLegalHold(legalHold);
                    logTrace("->", "setObjectLegalHold");
                    logTrace("hnote right: " + bucketKey + "@" + versionId);
                    logTrace("rnote right: " + status.toString());
                    amazonS3.setObjectLegalHold(request);
                }
            }
            if (blobUpdateContext.coldStorageClass != null) {
                StorageClass storageClass = blobUpdateContext.coldStorageClass.inColdStorage ? StorageClass.Glacier
                        : StorageClass.Standard;
                CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, bucketKey, bucketName,
                        bucketKey).withSourceVersionId(versionId).withStorageClass(storageClass);
                logTrace("->", "updateStorageClass");
                logTrace("hnote right: " + bucketKey + "@" + versionId);
                logTrace("rnote right: " + storageClass);
                amazonS3.copyObject(copyObjectRequest);
            }
            if (blobUpdateContext.restoreForDuration != null) {
                Duration duration = blobUpdateContext.restoreForDuration.duration;
                // round up duration to days
                int days = (int) duration.plusDays(1).minusSeconds(1).toDays();
                RestoreObjectRequest request = new RestoreObjectRequest(bucketName, bucketKey, days).withVersionId(
                        versionId);
                amazonS3.restoreObjectV2(request);
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
        String bucketKey = bucketKey(objectKey);
        try {
            if (versionId == null) {
                logTrace("->", "deleteObject");
                logTrace("hnote right: " + bucketKey);
                amazonS3.deleteObject(bucketName, bucketKey);
            } else {
                logTrace("->", "deleteVersion");
                logTrace("hnote right: " + bucketKey + "@" + versionId);
                amazonS3.deleteVersion(bucketName, bucketKey, versionId);
            }
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                logTrace("<--", "missing");
            } else {
                log.warn("Cannot delete: s3://" + bucketName + "/" + bucketKey + "@" + versionId, e);
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
            ObjectListing list = null;
            int prefixLength = bucketPrefix.length();
            logTrace("->", "listObjects on " + getId());
            do {
                if (list == null) {
                    ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
                                                                                    .withPrefix(bucketPrefix);
                    if (config.getSubDirsDepth() == 0) {
                        // use delimiter to avoid useless listing of objects in "subdirectories"
                        listObjectsRequest.setDelimiter(DELIMITER);
                    }
                    list = amazonS3.listObjects(listObjectsRequest);
                } else {
                    list = amazonS3.listNextBatchOfObjects(list);
                }
                for (S3ObjectSummary summary : list.getObjectSummaries()) {
                    String path = summary.getKey().substring(prefixLength);
                    // do not use pathStrategy when subDirsDepth=0 - NXP-30632
                    String key = config.getSubDirsDepth() == 0 ? path : pathStrategy.getKeyForPath(path);
                    if (key == null) {
                        continue;
                    }
                    if (useDeDuplication) {
                        if (!((KeyStrategyDigest) keyStrategy).isValidDigest(key)) {
                            // ignore files that cannot be digests, for safety
                            continue;
                        }
                    }
                    long length = summary.getSize();
                    status.sizeBinaries += length;
                    status.numBinaries++;
                    toDelete.add(key);
                     if (toDelete.size() % WARN_OBJECTS_THRESHOLD == 0) {
                        log.warn("Listing {} in progress, {} objects ...", getId(), toDelete.size());
                    }
                }
            } while (list.isTruncated());
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
