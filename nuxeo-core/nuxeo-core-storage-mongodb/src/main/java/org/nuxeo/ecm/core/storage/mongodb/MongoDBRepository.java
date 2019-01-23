/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mongodb;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_DATA;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.resource.spi.ConnectionManager;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

/**
 * MongoDB implementation of a {@link Repository}.
 *
 * @since 5.9.4
 */
public class MongoDBRepository extends DBSRepositoryBase {

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p />
     * The connection id will be {@code repository/[REPOSITORY_NAME]}.
     */
    public static final String REPOSITORY_CONNECTION_PREFIX = "repository/";

    public static final Long LONG_ZERO = Long.valueOf(0);

    public static final Double ZERO = Double.valueOf(0);

    public static final Double ONE = Double.valueOf(1);

    public static final String MONGODB_ID = "_id";

    public static final String MONGODB_INC = "$inc";

    public static final String MONGODB_SET = "$set";

    public static final String MONGODB_UNSET = "$unset";

    public static final String MONGODB_PUSH = "$push";

    public static final String MONGODB_EACH = "$each";

    public static final String MONGODB_META = "$meta";

    public static final String MONGODB_TEXT_SCORE = "textScore";

    public static final String FULLTEXT_INDEX_NAME = "fulltext";

    public static final String LANGUAGE_FIELD = "__language";

    public static final String COUNTER_NAME_UUID = "ecm:id";

    public static final String COUNTER_FIELD = "seq";

    /** The key to use to store the id in the database. */
    protected String idKey;

    /** Number of values still available in the in-memory sequence. */
    protected long sequenceLeft;

    /** Last value used from the in-memory sequence. */
    protected long sequenceLastValue;

    /** Sequence allocation block size. */
    protected long sequenceBlockSize;

    protected final MongoClient mongoClient;

    protected final MongoDBConverter converter;

    protected final MongoDBCursorService cursorService;

    protected final MongoCollection<Document> collection;

    protected final MongoCollection<Document> countersCollection;

    protected final boolean supportsSessions;

    protected final boolean supportsTransactions;

    public MongoDBRepository(ConnectionManager cm, MongoDBRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);

        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        String connectionId = REPOSITORY_CONNECTION_PREFIX + descriptor.name;
        mongoClient = mongoService.getClient(connectionId);
        String dbname = mongoService.getDatabaseName(connectionId);
        MongoDatabase database = mongoClient.getDatabase(dbname);
        collection = database.getCollection(descriptor.name);
        countersCollection = database.getCollection(descriptor.name + ".counters");

        if (Boolean.TRUE.equals(descriptor.nativeId)) {
            idKey = MONGODB_ID;
        } else {
            idKey = KEY_ID;
        }
        boolean useCustomId = KEY_ID.equals(idKey);
        if (idType == IdType.sequence || DEBUG_UUIDS) {
            Integer sbs = descriptor.sequenceBlockSize;
            sequenceBlockSize = sbs == null ? 1 : sbs.longValue();
            sequenceLeft = 0;
        }
        converter = new MongoDBConverter(useCustomId ? null : KEY_ID);
        cursorService = new MongoDBCursorService(converter);

        // check session and transaction support
        boolean hasSessions;
        boolean hasTransactions;
        try (ClientSession session = mongoClient.startSession()) {
            hasSessions = true;
            try {
                session.startTransaction();
                session.abortTransaction();
                hasTransactions = true;
            } catch (MongoClientException e) {
                hasTransactions = false;
            }
        } catch (MongoClientException ee) {
            // startSession may throw
            // "Sessions are not supported by the MongoDB cluster to which this client is connected"
            // startTransaction may throw
            hasSessions = false;
            hasTransactions = false;
        }
        supportsSessions = hasSessions;
        supportsTransactions = hasTransactions;

        // initialize the repository
        try (MongoDBConnection connection = getConnection()) {
            connection.initRepository();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cursorService.clear();
    }

    @Override
    public MongoDBConnection getConnection() {
        return new MongoDBConnection(this);
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Arrays.asList(IdType.varchar, IdType.sequence);
    }

    protected boolean supportsSessions() {
        return supportsSessions;
    }

    @Override
    public boolean supportsTransactions() {
        return supportsTransactions;
    }

    protected MongoClient getClient() {
        return mongoClient;
    }

    protected MongoDBCursorService getCursorService() {
        return cursorService;
    }

    protected MongoCollection<Document> getCollection() {
        return collection;
    }

    protected MongoCollection<Document> getCountersCollection() {
        return countersCollection;
    }

    protected String getIdKey() {
        return idKey;
    }

    protected MongoDBConverter getConverter() {
        return converter;
    }

    protected synchronized Long getNextSequenceId() {
        if (sequenceLeft == 0) {
            // allocate a new sequence block
            // the database contains the last value from the last block
            Bson filter = Filters.eq(MONGODB_ID, COUNTER_NAME_UUID);
            Bson update = Updates.inc(COUNTER_FIELD, Long.valueOf(sequenceBlockSize));
            // we do NOT want to use clientSession here because counters must be non-transactional
            Document idCounter = countersCollection.findOneAndUpdate(filter, update,
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (idCounter == null) {
                throw new NuxeoException("Repository id counter not initialized");
            }
            sequenceLeft = sequenceBlockSize;
            sequenceLastValue = ((Long) idCounter.get(COUNTER_FIELD)).longValue() - sequenceBlockSize;
        }
        sequenceLeft--;
        sequenceLastValue++;
        return Long.valueOf(sequenceLastValue);
    }

    /** Keys used for document projection when marking all binaries for GC. */
    protected Bson binaryKeys;

    @Override
    protected void initBlobsPaths() {
        MongoDBBlobFinder finder = new MongoDBBlobFinder();
        finder.visit();
        binaryKeys = Projections.fields(finder.binaryKeys);
    }

    protected static class MongoDBBlobFinder extends BlobFinder {
        protected List<Bson> binaryKeys = new ArrayList<>(Collections.singleton(Projections.excludeId()));

        @Override
        protected void recordBlobPath() {
            path.addLast(KEY_BLOB_DATA);
            binaryKeys.add(Projections.include(StringUtils.join(path, ".")));
            path.removeLast();
        }
    }

    @Override
    public void markReferencedBinaries() {
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        // TODO add a query to not scan all documents
        Block<Document> block = doc -> markReferencedBinaries(doc, blobManager);
        collection.find().projection(binaryKeys).forEach(block);
    }

    protected void markReferencedBinaries(Document ob, DocumentBlobManager blobManager) {
        for (String key : ob.keySet()) {
            Object value = ob.get(key);
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object v : list) {
                    if (v instanceof Document) {
                        markReferencedBinaries((Document) v, blobManager);
                    } else {
                        markReferencedBinary(v, blobManager);
                    }
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    markReferencedBinary(v, blobManager);
                }
            } else if (value instanceof Document) {
                markReferencedBinaries((Document) value, blobManager);
            } else {
                markReferencedBinary(value, blobManager);
            }
        }
    }

    protected void markReferencedBinary(Object value, DocumentBlobManager blobManager) {
        if (!(value instanceof String)) {
            return;
        }
        String key = (String) value;
        blobManager.markReferencedBinary(key, repositoryName);
    }

}
