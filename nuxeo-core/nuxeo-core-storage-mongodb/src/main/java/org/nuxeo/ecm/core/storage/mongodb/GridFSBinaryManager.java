/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.AbstractBinaryManager;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

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

    public static final String SERVER_PROPERTY = "server";

    public static final String DBNAME_PROPERTY = "dbname";

    public static final String BUCKET_PROPERTY = "bucket";

    protected Map<String, String> properties;

    protected MongoClient client;

    protected GridFS gridFS;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        this.properties = properties;
        String server = properties.get(SERVER_PROPERTY);
        if (StringUtils.isBlank(server)) {
            throw new NuxeoException("Missing server property in GridFS Binary Manager descriptor: " + blobProviderId);
        }
        String dbname = properties.get(DBNAME_PROPERTY);
        if (StringUtils.isBlank(dbname)) {
            throw new NuxeoException("Missing dbname property in GridFS Binary Manager descriptor: " + blobProviderId);
        }
        String bucket = properties.get(BUCKET_PROPERTY);
        if (StringUtils.isBlank(bucket)) {
            bucket = blobProviderId + ".fs";
        }
        if (server.startsWith("mongodb://")) {
            client = new MongoClient(new MongoClientURI(server));
        } else {
            client = new MongoClient(new ServerAddress(server));
        }
        gridFS = new GridFS(client.getDB(dbname), bucket);
        garbageCollector = new GridFSBinaryGarbageCollector();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public BinaryManager getBinaryManager() {
        return this;
    }

    public GridFS getGridFS() {
        return gridFS;
    }

    /**
     * A binary backed by GridFS.
     */
    protected class GridFSBinary extends Binary {

        private static final long serialVersionUID = 1L;

        protected GridFSBinary(String digest, String blobProviderId) {
            super(digest, blobProviderId);
        }

        @Override
        public InputStream getStream() {
            GridFSDBFile dbFile = gridFS.findOne(digest);
            return dbFile == null ? null : dbFile.getInputStream();
        }
    }

    @Override
    public Binary getBinary(Blob blob) throws IOException {
        if (!(blob instanceof FileBlob)) {
            return super.getBinary(blob); // just open the stream and call getBinary(InputStream)
        }
        // we already have a file so can compute the length and digest efficiently
        File file = ((FileBlob) blob).getFile();
        String digest;
        try (InputStream in = new FileInputStream(file)) {
            digest = DigestUtils.md5Hex(in);
        }
        // if the digest is not already known then save to GridFS
        GridFSDBFile dbFile = gridFS.findOne(digest);
        if (dbFile == null) {
            try (InputStream in = new FileInputStream(file)) {
                GridFSInputFile inputFile = gridFS.createFile(in, digest);
                inputFile.save();
            }
        }
        return new GridFSBinary(digest, blobProviderId);
    }

    @Override
    protected Binary getBinary(InputStream in) throws IOException {
        // save the file to GridFS
        GridFSInputFile inputFile = gridFS.createFile(in, true);
        inputFile.save();
        // now we know length and digest
        String digest = inputFile.getMD5();
        // if the digest is already known then reuse it instead
        GridFSDBFile dbFile = gridFS.findOne(digest);
        if (dbFile == null) {
            // no existing file, set its filename as the digest
            inputFile.setFilename(digest);
            inputFile.save();
        } else {
            // file already existed, no need for the temporary one
            gridFS.remove(inputFile);
        }
        return new GridFSBinary(digest, blobProviderId);
    }

    @Override
    public Binary getBinary(String digest) {
        GridFSDBFile dbFile = gridFS.findOne(digest);
        if (dbFile != null) {
            return new GridFSBinary(digest, blobProviderId);
        }
        return null;
    }

    @Override
    public Blob readBlob(BlobManager.BlobInfo blobInfo) throws IOException {
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

    public class GridFSBinaryGarbageCollector implements BinaryGarbageCollector {

        protected BinaryManagerStatus status;

        protected volatile long startTime;

        protected static final String MARK_KEY_PREFIX = "gc-mark-key-";

        protected String msKey;

        @Override
        public String getId() {
            return "gridfs:" + getGridFS().getBucketName();
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
            GridFSDBFile dbFile = gridFS.findOne(digest);
            if (dbFile != null) {
                dbFile.setMetaData(new BasicDBObject(msKey, TRUE));
                dbFile.save();
                status.numBinaries += 1;
                status.sizeBinaries += dbFile.getLength();
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
            DBObject query = new BasicDBObject("metadata." + msKey, new BasicDBObject("$exists", FALSE));
            List<GridFSDBFile> files = gridFS.find(query);
            for (GridFSDBFile file : files) {
                status.numBinariesGC += 1;
                status.sizeBinariesGC += file.getLength();
                if (delete) {
                    gridFS.remove(file);
                }
            }
            startTime = 0;
        }
    }

}
