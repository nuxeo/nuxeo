/*
 * (C) Copyright 2014-2020 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.MongoClientException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

/**
 * MongoDB implementation of a {@link Repository}.
 *
 * @since 5.9.4
 */
public class MongoDBRepository extends DBSRepositoryBase {

    private static final Logger log = LogManager.getLogger(MongoDBRepository.class);

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

    protected static final int SEQUENCE_RANDOMIZED_BLOCKSIZE_DEFAULT = 1000;

    public static final String COUNTER_NAME_UUID = "ecm:id";

    public static final String COUNTER_FIELD = "seq";

    /**
     * Default maximum execution time for a query.
     *
     * @since 11.1
     */
    protected static final Duration MAX_TIME_DEFAULT = Duration.ofHours(1);

    /** The key to use to store the id in the database. */
    protected String idKey;

    /** Sequence allocation block size. */
    protected long sequenceBlockSize;

    protected final MongoDBRepositoryDescriptor descriptor;

    protected final MongoClient mongoClient;

    protected final MongoDBConverter converter;

    protected final MongoDBCursorService cursorService;

    protected final MongoCollection<Document> coll;

    protected final MongoCollection<Document> countersColl;

    protected final boolean supportsSessions;

    protected final boolean supportsTransactions;

    /**
     * Maximum execution time for a query.
     *
     * @since 11.1
     */
    protected final long maxTimeMS;

    public MongoDBRepository(MongoDBRepositoryDescriptor descriptor) {
        super(descriptor.name, descriptor);
        this.descriptor = descriptor;

        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        String connectionId = REPOSITORY_CONNECTION_PREFIX + descriptor.name;
        mongoClient = mongoService.getClient(connectionId);
        String dbname = mongoService.getDatabaseName(connectionId);
        MongoDatabase database = mongoClient.getDatabase(dbname);
        coll = database.getCollection(descriptor.name);
        countersColl = database.getCollection(descriptor.name + ".counters");
        Duration maxTime = mongoService.getConfig(connectionId).maxTime;
        if (maxTime == null) {
            maxTime = MAX_TIME_DEFAULT;
        }
        maxTimeMS = maxTime.toMillis();

        if (Boolean.TRUE.equals(descriptor.nativeId)) {
            idKey = MONGODB_ID;
        } else {
            idKey = KEY_ID;
        }
        boolean useCustomId = KEY_ID.equals(idKey);
        if (idType == IdType.sequence || idType == IdType.sequenceHexRandomized || DEBUG_UUIDS) {
            Integer sbs = descriptor.sequenceBlockSize;
            if (sbs == null) {
                sequenceBlockSize = idType == IdType.sequenceHexRandomized ? SEQUENCE_RANDOMIZED_BLOCKSIZE_DEFAULT : 1;
            } else {
                sequenceBlockSize = sbs.longValue();
            }
        }
        Set<String> idValuesKeys;
        if (idType == IdType.sequenceHexRandomized) {
            // store these ids as longs
            idValuesKeys = DBSSession.ID_VALUES_KEYS;
        } else {
            idValuesKeys = Set.of();
        }
        converter = new MongoDBConverter(useCustomId ? null : KEY_ID, DBSSession.TRUE_OR_NULL_BOOLEAN_KEYS,
                idValuesKeys);
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
        // TODO: reactivate sessions/transactions when they can be better tested
        hasSessions = false;
        hasTransactions = false;
        supportsSessions = hasSessions;
        supportsTransactions = hasTransactions;
        initRepository();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cursorService.clear();
    }

    protected void initRepository() {
        // the first connection will init the repository
        getConnection().close();
    }

    @Override
    public MongoDBConnection getConnection() {
        return new MongoDBConnection(this);
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Arrays.asList(IdType.varchar, IdType.sequence, IdType.sequenceHexRandomized);
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
        return coll;
    }

    protected MongoCollection<Document> getCountersCollection() {
        return countersColl;
    }

    protected String getIdKey() {
        return idKey;
    }

    protected MongoDBConverter getConverter() {
        return converter;
    }

    /** Keys used for document projection when marking all binaries for GC. */
    protected Bson binaryKeys;

    @Override
    protected void initBlobsPaths() {
        MongoDBBlobFinder finder = new MongoDBBlobFinder();
        finder.visit();
        if (isFulltextStoredInBlob()) {
            finder.recordBlobKey(KEY_FULLTEXT_BINARY);
        }
        binaryKeys = Projections.fields(finder.binaryKeys);
    }

    protected static class MongoDBBlobFinder extends BlobFinder {
        protected List<Bson> binaryKeys = new ArrayList<>(Set.of(Projections.excludeId()));

        @Override
        protected void recordBlobPath() {
            path.addLast(KEY_BLOB_DATA);
            recordBlobKey(StringUtils.join(path, "."));
            path.removeLast();
        }

        protected void recordBlobKey(String key) {
            binaryKeys.add(Projections.include(key));
        }
    }

    @Override
    public void markReferencedBinaries() {
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        // TODO add a query to not scan all documents
        log.trace("MongoDB: QUERY {} KEYS {}", Document::new, () -> binaryKeys);
        coll.find().projection(binaryKeys).forEach(doc -> markReferencedBinaries(doc, blobManager));
    }

    protected void markReferencedBinaries(Document ob, DocumentBlobManager blobManager) {
        for (var value : ob.values()) {
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
