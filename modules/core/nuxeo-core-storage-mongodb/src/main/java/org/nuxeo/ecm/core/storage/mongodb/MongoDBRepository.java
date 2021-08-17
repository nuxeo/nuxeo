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

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_KEYS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

/**
 * MongoDB implementation of a {@link Repository}.
 *
 * @since 5.9.4
 */
public class MongoDBRepository extends DBSRepositoryBase {

    private static final Logger log = LogManager.getLogger(MongoDBRepository.class);

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p>
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

    /** @since 11.5 */
    public static final String MONGODB_PULLALL = "$pullAll";

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

    protected static final String SETTING_VALUE = "value";

    /**
     * Settings key to determine whether {@code ecm:blobKeys} is supported.
     * <p>
     * The value is {@code true} on new or migrated repositories.
     */
    protected static final String SETTING_DENORMALIZED_BLOB_KEYS = "denormalizedBlobKeys";

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

    protected final MongoCollection<Document> settingsColl;

    protected final boolean supportsSessions;

    protected final boolean supportsTransactions;

    /**
     * Maximum execution time for a query when outside of a transaction.
     *
     * @since 11.1
     */
    protected final long maxTimeMS;

    protected boolean supportsDenormalizedBlobKeys;

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
        settingsColl = database.getCollection(descriptor.name + ".settings");
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

    protected void initSettings() {
        supportsDenormalizedBlobKeys = true;
        Bson filter = Filters.eq(MONGODB_ID, SETTING_DENORMALIZED_BLOB_KEYS);
        Bson update = Updates.set(SETTING_VALUE, supportsDenormalizedBlobKeys);
        settingsColl.updateOne(filter, update, new UpdateOptions().upsert(true));
        initCapabilities();
    }

    protected void readSettings() {
        Document doc = settingsColl.find(Filters.eq(MONGODB_ID, SETTING_DENORMALIZED_BLOB_KEYS)).first();
        if (doc == null) {
            supportsDenormalizedBlobKeys = false;
        } else {
            supportsDenormalizedBlobKeys = doc.getBoolean(SETTING_VALUE, false);
        }
        initCapabilities();
    }

    protected void initCapabilities() {
        capabilities.put(CAPABILITY_QUERY_BLOB_KEYS, supportsDenormalizedBlobKeys);
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

    /**
     * Keys used for document projection when marking all binaries for GC.
     * <p>
     * Used when denormalized ecm:blobKeys is not available.
     */
    protected Bson binaryKeys;

    @Override
    protected void initBlobsPaths() {
        super.initBlobsPaths();
        // compute projections for when ecm:blobKeys is not available
        List<Bson>projections = new ArrayList<>(blobKeysPaths.size() + 1);
        projections.add(Projections.excludeId());
        blobKeysPaths.forEach(path -> projections.add(Projections.include(String.join(".", path))));
        binaryKeys = Projections.fields(projections);
    }

    @Override
    public void markReferencedBlobs(BiConsumer<String, String> markerCallback) {
        Bson filter;
        Bson projection;
        Consumer<Document> markReferencedBlobs;
        if (supportsDenormalizedBlobKeys) {
            filter = Filters.exists(KEY_BLOB_KEYS, true);
            projection = Projections.fields(Projections.excludeId(), Projections.include(KEY_BLOB_KEYS));
            markReferencedBlobs = doc -> markReferencedBlobsDenormalized(doc, markerCallback);
        } else {
            filter = new Document();
            projection = binaryKeys;
            markReferencedBlobs = doc -> markReferencedBlobs(doc, markerCallback);
        }
        log.trace("MongoDB: QUERY {} KEYS {}", filter, projection);
        coll.find(filter).projection(projection).forEach(markReferencedBlobs);
    }

    protected void markReferencedBlobsDenormalized(Document ob, BiConsumer<String, String> markReferencedBlob) {
        Object blobKeys = ob.get(KEY_BLOB_KEYS);
        if (blobKeys instanceof List) {
            for (Object v : (List<?>) blobKeys) {
                if (v instanceof String) {
                    markReferencedBlob.accept((String) v, repositoryName);
                }
            }
        }
    }

    protected void markReferencedBlobs(Document ob, BiConsumer<String, String> markerCallback) {
        for (var value : ob.values()) {
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object v : list) {
                    if (v instanceof Document) {
                        markReferencedBlobs((Document) v, markerCallback);
                    } else {
                        markReferencedBlob(v, markerCallback);
                    }
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    markReferencedBlob(v, markerCallback);
                }
            } else if (value instanceof Document) {
                markReferencedBlobs((Document) value, markerCallback);
            } else {
                markReferencedBlob(value, markerCallback);
            }
        }
    }

    protected void markReferencedBlob(Object value, BiConsumer<String, String> markerCallback) {
        if (!(value instanceof String)) {
            return;
        }
        String key = (String) value;
        markerCallback.accept(key, repositoryName);
    }

}
