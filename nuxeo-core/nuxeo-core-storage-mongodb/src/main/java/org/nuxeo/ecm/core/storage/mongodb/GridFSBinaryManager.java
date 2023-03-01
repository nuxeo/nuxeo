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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.NAMESPACE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.TRANSIENT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.AbstractBinaryManager;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerRootDescriptor;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.gridfs.GridFS;

/**
 * Implements the {@link BinaryManager} and {@link BlobProvider} interface using MongoDB GridFS.
 * <p>
 * This implementation does not use local caching.
 * <p>
 * This implementation may not always be ideal regarding streaming because of the usage of {@link Binary} interface that
 * exposes a {@link File}.
 *
 * @since 7.10
 */
public class GridFSBinaryManager extends AbstractBinaryManager implements BlobProvider {

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p />
     * The connection id will be {@code blobProvider/[BLOB_PROVIDER_ID]}.
     */
    public static final String BLOB_PROVIDER_CONNECTION_PREFIX = "blobProvider/";

    /**
     * @deprecated since 9.3 use {@link MongoDBConnectionService} to provide access instead
     */
    @Deprecated
    public static final String SERVER_PROPERTY = "server";

    /**
     * @deprecated since 9.3 use {@link MongoDBConnectionService} to provide access instead
     */
    @Deprecated
    public static final String DBNAME_PROPERTY = "dbname";

    public static final String BUCKET_PROPERTY = "bucket";

    private static final String METADATA_PROPERTY_FILENAME = "filename";

    private static final String METADATA_PROPERTY_METADATA = "metadata";

    private static final String METADATA_PROPERTY_LENGTH = "length";

    @Deprecated
    protected GridFS gridFS;

    protected GridFSBucket gridFSBucket;

    protected MongoCollection<Document> filesColl;

    protected String bucket;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        if (StringUtils.isNotBlank(properties.get(SERVER_PROPERTY))
                || StringUtils.isNotBlank(properties.get(DBNAME_PROPERTY))) {
            throw new NuxeoException("Unable to initialize GridFS Binary Manager, properties " + SERVER_PROPERTY
                    + " and " + DBNAME_PROPERTY + " has been removed. Please configure a connection!");
        }
        BinaryManagerRootDescriptor descriptor = new BinaryManagerRootDescriptor();
        descriptor.digest = getDefaultDigestAlgorithm();
        setDescriptor(descriptor);

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

        garbageCollector = new GridFSBinaryGarbageCollector(bucket);
    }

    @Override
    public void close() {
    }

    @Override
    public BinaryManager getBinaryManager() {
        return this;
    }

    protected GridFSBucket getGridFSBucket() {
       if (gridFSBucket == null) {
            MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
            MongoDatabase database = mongoService.getDatabase(BLOB_PROVIDER_CONNECTION_PREFIX + blobProviderId);
            gridFSBucket = GridFSBuckets.create(database, bucket);
        }
        return gridFSBucket;
    }

    protected MongoCollection<Document> getFilesColl() {
        if (filesColl == null) {
            MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
            MongoDatabase database = mongoService.getDatabase(BLOB_PROVIDER_CONNECTION_PREFIX + blobProviderId);
            filesColl = database.getCollection(bucket + ".files");
        }
        return filesColl;
    }

    /**
     * A binary backed by GridFS.
     */
    protected static class GridFSBinary extends Binary {

        private static final long serialVersionUID = 1L;

        // transient to be Serializable
        protected transient GridFSBinaryManager bm;

        protected GridFSBinary(String digest, String blobProviderId, GridFSBinaryManager bm) {
            super(digest, blobProviderId);
            this.bm = bm;
        }

        // because the class is Serializable, re-acquire the BinaryManager if needed
        protected GridFSBinaryManager getBinaryManager() {
            if (bm == null) {
                if (blobProviderId == null) {
                    throw new UnsupportedOperationException("Cannot find binary manager, no blob provider id");
                }
                BlobManager blobManager = Framework.getService(BlobManager.class);
                BlobProvider bp = blobManager.getBlobProvider(blobProviderId);
                bm = (GridFSBinaryManager) bp.getBinaryManager();
            }
            return bm;
        }

        @Override
        public InputStream getStream() {
            return getBinaryManager().getGridFSBucket().openDownloadStream(digest);
        }

        @Override
        protected File recomputeFile() {
            return null; // no file to recompute
        }
    }

    @Override
    public Binary getBinary(Blob blob) throws IOException {
        if (!(blob instanceof FileBlob)) {
            return super.getBinary(blob); // just open the stream and call getBinary(InputStream)
        }
        // we already have a file so can compute the length and digest efficiently
        File file = blob.getFile();
        String digest;
        try (InputStream in = new FileInputStream(file)) {
            digest = DigestUtils.md5Hex(in);
        }
        // if the digest is not already known then save to GridFS
        GridFSFile dbFile = getGridFSBucket().find(Filters.eq(METADATA_PROPERTY_FILENAME, digest)).first();
        if (dbFile == null) {
            try (InputStream in = new FileInputStream(file)) {
                getGridFSBucket().uploadFromStream(digest, in);
            }
        }
        return new GridFSBinary(digest, blobProviderId, this);
    }

    @Override
    protected Binary getBinary(InputStream in) throws IOException {
        try {
            // save the file to GridFS
            String inputName = "tmp-" + System.nanoTime();
            ObjectId id = getGridFSBucket().uploadFromStream(inputName, in);
            // now we know length and digest
            GridFSFile inputFile = getGridFSBucket().find(Filters.eq(METADATA_PROPERTY_FILENAME, inputName)).first();
            String digest = inputFile.getMD5();
            // if the digest is already known then reuse it instead
            GridFSFile dbFile = getGridFSBucket().find(Filters.eq(METADATA_PROPERTY_FILENAME, digest)).first();
            if (dbFile == null) {
                // no existing file, set its filename as the digest
                getGridFSBucket().rename(id, digest);
            } else {
                // file already existed, no need for the temporary one
                getGridFSBucket().delete(id);
            }
            return new GridFSBinary(digest, blobProviderId, this);
        } finally {
            in.close();
        }
    }

    @Override
    public Binary getBinary(String digest) {
        GridFSFile dbFile = getGridFSBucket().find(Filters.eq(METADATA_PROPERTY_FILENAME, digest)).first();
        if (dbFile != null) {
            return new GridFSBinary(digest, blobProviderId, this);
        }
        return null;
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).readBlob(blobInfo);
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).writeBlob(blob);
    }

    @Override
    public boolean supportsUserUpdate() {
        return !Boolean.parseBoolean(properties.get(PREVENT_USER_UPDATE));
    }

    @Override
    public boolean isTransient() {
        return Boolean.parseBoolean(properties.get(TRANSIENT));
    }

    public class GridFSBinaryGarbageCollector implements BinaryGarbageCollector {

        protected final String bucket;

        protected BinaryManagerStatus status;

        protected volatile long startTime;

        protected static final String MARK_KEY_PREFIX = "gc-mark-key-";

        protected String msKey;

        public GridFSBinaryGarbageCollector(String bucket) {
            this.bucket = bucket;
        }

        @Override
        public String getId() {
            return "gridfs:" + bucket;
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return status;
        }

        @Override
        public boolean isInProgress() {
            return startTime != 0;
        }

        @Override
        public void mark(String digest) {
            Document dbFile = getFilesColl().findOneAndUpdate(Filters.eq(METADATA_PROPERTY_FILENAME, digest),
                    Updates.set(String.format("%s.%s", METADATA_PROPERTY_METADATA, msKey), TRUE),
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (dbFile != null) {
                status.numBinaries += 1;
                status.sizeBinaries += dbFile.getLong(METADATA_PROPERTY_LENGTH);
            }
        }

        @Override
        public void start() {
            if (startTime != 0) {
                throw new NuxeoException("Already started");
            }
            startTime = System.currentTimeMillis();
            status = new BinaryManagerStatus();
            msKey = MARK_KEY_PREFIX + System.currentTimeMillis();
        }

        @Override
        public void stop(boolean delete) {
            getGridFSBucket().find(Filters.exists(String.format("%s.%s", METADATA_PROPERTY_METADATA, msKey), false)) //
                        .forEach((Block<GridFSFile>) file -> {
                            status.numBinariesGC += 1;
                            status.sizeBinariesGC += file.getLength();
                            if (delete) {
                                getGridFSBucket().delete(file.getId());
                            }
                        });
            startTime = 0;
        }

        @Override
        public void reset() {
            startTime = 0;
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

}
