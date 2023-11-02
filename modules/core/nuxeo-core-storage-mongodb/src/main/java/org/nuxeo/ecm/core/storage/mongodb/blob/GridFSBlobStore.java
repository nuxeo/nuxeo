/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.storage.mongodb.blob;

import static java.lang.Boolean.TRUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.NAMESPACE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.AbstractBlobGarbageCollector;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobContext;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobWriteContext;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

/**
 * Blob provider that stores files in MongoDB GridFS.
 *
 * @since 2023.5
 */
public class GridFSBlobStore extends AbstractBlobStore {

    protected static final Logger log = LogManager.getLogger(GridFSBlobStore.class);

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p>
     * The connection id will be {@code blobProvider/[BLOB_PROVIDER_ID]}.
     */
    protected static final String BLOB_PROVIDER_CONNECTION_PREFIX = "blobProvider/";

    protected static final String BUCKET_PROPERTY = "bucket";

    protected static final String METADATA_PROPERTY_FILENAME = "filename";

    protected static final String METADATA_PROPERTY_METADATA = "metadata";

    protected static final String METADATA_PROPERTY_LENGTH = "length";

    protected GridFSBucket gridFSBucket;

    protected String bucket;

    protected MongoCollection<Document> filesColl;

    public GridFSBlobStore(String blobProviderId, String name, Map<String, String> properties,
            KeyStrategy keyStrategy) {
        super(blobProviderId, name, keyStrategy);
        String namespace = properties.get(NAMESPACE);
        bucket = properties.get(BUCKET_PROPERTY);
        if (StringUtils.isBlank(bucket)) {
            if (StringUtils.isNotBlank(namespace)) {
                bucket = blobProviderId + "." + namespace.trim();
            } else {
                bucket = blobProviderId;
            }
            bucket = bucket + ".fs";
        } else if (StringUtils.isNotBlank(namespace)) {
            bucket = bucket + "." + namespace.trim();
        }
    }

    @Override
    public void clear() {
        getGridFSBucket().drop();
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        // GridFS does not offer file entry duplication
        return false;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        Path tmp = null;
        try {
            OptionalOrUnknown<Path> fileOpt = sourceStore.getFile(sourceKey);
            Path file;
            if (fileOpt.isPresent()) {
                file = fileOpt.get();
            } else {
                // no local file available, read from source
                tmp = Files.createTempFile("bin_", ".tmp");
                boolean found = sourceStore.readBlob(sourceKey, tmp);
                if (!found) {
                    return null;
                }
                file = tmp;
            }
            // if the digest is not already known then save to GridFS
            GridFSFile dbFile = getGridFSFile(key);
            if (dbFile == null) {
                try (InputStream in = new FileInputStream(file.toFile())) {
                    getGridFSBucket().uploadFromStream(key, in);
                }
            }
            if (atomicMove) {
                sourceStore.deleteBlob(sourceKey);
            }
            return key;
        } finally {
            if (tmp != null) {
                try {
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }
    }

    @Override
    public void deleteBlob(String key) {
        GridFSFile dbFile = getGridFSFile(key);
        getGridFSBucket().delete(dbFile.getId());
    }

    @Override
    public boolean exists(String key) {
        GridFSFile dbFile = getGridFSFile(key);
        return dbFile != null;
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return new GridFSBlobGarbageCollector();
    }

    public class GridFSBlobGarbageCollector extends AbstractBlobGarbageCollector {

        protected static final int WARN_OBJECTS_THRESHOLD = 100_000;

        protected static final String MARK_KEY_PREFIX = "gc-mark-key-";

        protected static final String GC_INSTANCE_KEY_PREFIX = "gc-instance-key-";

        protected String msKey;

        @Override
        public void computeToDelete() {
            toDelete = new HashSet<>();
            getFilesColl().find().forEach(dbFile -> {
                status.sizeBinaries += dbFile.getLong(METADATA_PROPERTY_LENGTH);
                status.numBinaries++;
                toDelete.add((String) dbFile.get(METADATA_PROPERTY_FILENAME));
                if (toDelete.size() % WARN_OBJECTS_THRESHOLD == 0) {
                    log.warn("Listing {} in progress, {} objects ...", getId(), toDelete.size());
                }
            });
            if (toDelete.size() >= WARN_OBJECTS_THRESHOLD) {
                log.warn("Listing {} completed, {} objects.", getId(), toDelete.size());
            }
        }

        @Override
        public String getId() {
            return "gridfs:" + bucket;
        }

        @Override
        public void mark(String key) {
            toDelete.remove(key);
        }

        @Override
        public void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (String key : toDelete) {
                Document dbFile = getFilesColl().findOneAndUpdate(Filters.eq(METADATA_PROPERTY_FILENAME, key),
                        Updates.set(String.format("%s.%s", METADATA_PROPERTY_METADATA, msKey), TRUE),
                        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
                if (dbFile != null) {
                    long length = dbFile.getLong(METADATA_PROPERTY_LENGTH);
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

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.unknown();
    }

    protected MongoCollection<Document> getFilesColl() {
        if (filesColl == null) {
            MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
            MongoDatabase database = mongoService.getDatabase(BLOB_PROVIDER_CONNECTION_PREFIX + blobProviderId);
            filesColl = database.getCollection(bucket + ".files");
        }
        return filesColl;
    }

    protected GridFSBucket getGridFSBucket() {
        if (gridFSBucket == null) {
            MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
            MongoDatabase database = mongoService.getDatabase(BLOB_PROVIDER_CONNECTION_PREFIX + blobProviderId);
            gridFSBucket = GridFSBuckets.create(database, bucket);
        }
        return gridFSBucket;
    }

    protected GridFSFile getGridFSFile(String key) {
        return getGridFSBucket().find(Filters.eq(METADATA_PROPERTY_FILENAME, key)).first();
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        GridFSFile dbFile = getGridFSFile(key);
        return dbFile == null ? OptionalOrUnknown.missing()
                : OptionalOrUnknown.of(getGridFSBucket().openDownloadStream(key));
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        GridFSFile dbFile = getGridFSFile(key);
        if (dbFile == null) {
            return false;
        }
        try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(key)) {
            Files.copy(downloadStream, dest, REPLACE_EXISTING);
        }
        return true;
    }

    @Override
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        Path file;
        Path tmp = null;
        try {
            BlobContext blobContext = blobWriteContext.blobContext;
            Path blobWriteContextFile = blobWriteContext.getFile();
            if (blobWriteContextFile != null) { // we have a file, assume that the caller already observed the write
                file = blobWriteContextFile;
            } else {
                // no transfer to a file was done yet (no caching)
                // we may be able to use the blob's underlying file, if not pure streaming
                File blobFile = blobContext.blob.getFile();
                if (blobFile != null) { // otherwise use blob file directly
                    if (blobWriteContext.writeObserver != null) { // but we must still run the writes through the write
                                                                  // observer
                        transfer(blobWriteContext, NULL_OUTPUT_STREAM);
                    }
                    file = blobFile.toPath();
                } else {
                    // we must transfer the blob stream to a tmp file
                    tmp = Files.createTempFile("bin_", ".tmp");
                    transfer(blobWriteContext, tmp);
                    file = tmp;
                }
            }
            String key = blobWriteContext.getKey(); // may depend on write observer, for example for digests
            if (key == null) {
                // should never happen unless an invalid WriteObserver is used in new code
                throw new NuxeoException("Missing key");
            }
            // XXX do we try handle versioning ?
            // if the digest is not already known then save to GridFS
            GridFSFile dbFile = getGridFSFile(key);
            if (dbFile == null) {
                try (InputStream in = new FileInputStream(file.toFile())) {
                    getGridFSBucket().uploadFromStream(key, in);
                }
            }
            return key;
        } finally {
            if (tmp != null) {
                try {
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }

    }
}
