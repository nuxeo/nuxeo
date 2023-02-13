/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.mongodb.transientstore;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Projections.include;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;

/**
 * Transient Store optimized for MongoDB, using a blob provider for binaries. It can replace the
 * KeyValueBlobTransientStore implementation, it uses the same configuration.
 * <p>
 * A MongoDB collection is created per store, the name is prefixed by "transient.". If not provided the blob provider is
 * created using a default configuration with "transient_" prefix that gives the required transient property.
 * <p>
 * The storage format is the following:
 *
 * <pre>
 * { "_id" : $KEY, "completed" : false, "ttl" : ISODate("2022-11-17T12:10:16.568Z"),
 *   "params" : { "chunked" : "false" } },
 *   "blobs" : [ { "key" : ..., "mimetype" : ..., "encoding" : ..., "filename" : ..., "length" : NumberLong(131984), "digest" : ... } ],
 *   "blobSize" : NumberLong(131984), "blobCount" : 1 }
 * </pre>
 *
 * The total size of the storage is computed with an aggregation on the blobSize property.
 *
 * @since 2021.30
 */
public class MongoDBTransientStore implements TransientStoreProvider {

    private static final Logger log = LogManager.getLogger(MongoDBTransientStore.class);

    public static final String COLLECTION_NAME_PROPERTY = "nuxeo.mongodb.transient.collection.name";

    public static final String DEFAULT_COLLECTION_NAME = "transient";

    public static final String TRANSIENT_DATABASE_ID = "transient";

    public static final String CONFIG_BLOB_PROVIDER = "blobProvider";

    public static final String CONFIG_DEFAULT_BLOB_PROVIDER = "defaultBlobProvider";

    public static final String CONFIG_DEFAULT_BLOB_PROVIDER_DEFAULT = "default";

    public static final String KEY = "key";

    public static final String MIMETYPE = "mimetype";

    public static final String ENCODING = "encoding";

    public static final String FILENAME = "filename";

    public static final String LENGTH = "length";

    public static final String DIGEST = "digest";

    public static final String ID_KEY = "_id";

    public static final String TTL_KEY = "ttl";

    public static final String COMPLETED_KEY = "completed";

    public static final String BLOBS_KEY = "blobs";

    public static final String BLOB_SIZE_KEY = "blobSize";

    public static final String BLOB_COUNT_KEY = "blobCount";

    public static final Double ONE = 1.0;

    protected static final long WARN_DURATION_MS_THRESHOLD = 300_000L; // 5min

    private static final String PARAMS_KEY = "params";

    protected MongoCollection<Document> coll;

    protected TransientStoreConfig config;

    protected int firstLevelTTL;

    protected int secondLevelTTL;

    protected String blobProviderId;

    protected String defaultBlobProviderId;

    protected long absoluteMaxSize;

    protected long targetMaxSize;

    // ---------------------------------------------------------
    // TransientStoreProvider

    @Override
    public void init(TransientStoreConfig config) {
        this.config = config;
        firstLevelTTL = config.getFirstLevelTTL() * 60;
        secondLevelTTL = config.getSecondLevelTTL() * 60;
        targetMaxSize = config.getTargetMaxSizeMB() * 1024L * 1024;
        absoluteMaxSize = config.getAbsoluteMaxSizeMB() * 1024L * 1024;
        Map<String, String> properties = config.getProperties();
        if (properties == null) {
            properties = Collections.emptyMap();
        }
        String defaultName = config.getName();
        if (!defaultName.startsWith(BlobManagerComponent.TRANSIENT_ID_PREFIX)) {
            defaultName = BlobManagerComponent.TRANSIENT_ID_PREFIX + "_" + defaultName;
        }
        blobProviderId = defaultIfBlank(properties.get(CONFIG_BLOB_PROVIDER), defaultName);
        defaultBlobProviderId = defaultIfBlank(properties.get(CONFIG_DEFAULT_BLOB_PROVIDER),
                CONFIG_DEFAULT_BLOB_PROVIDER_DEFAULT);
        getBlobProvider(); // force explicit registration of the blob provider
        // the collection is lazy initialized because the transient service is used before mongo
    }

    protected MongoCollection<Document> getColl() {
        if (coll == null) {
            ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
            String collName = configurationService.getString(COLLECTION_NAME_PROPERTY, DEFAULT_COLLECTION_NAME) + "."
                    + config.getName();
            MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
            MongoDatabase database = mongoService.getDatabase(TRANSIENT_DATABASE_ID);
            coll = database.getCollection(collName);
            log.trace("TransientStore: {} is using collection: {}", config.getName(), coll.getNamespace());
            // make sure TTL works by creating the appropriate index
            IndexOptions indexOptions = new IndexOptions().expireAfter(0L, TimeUnit.SECONDS);
            coll.createIndex(new Document(TTL_KEY, ONE), indexOptions);
            coll.createIndex(new Document(BLOB_COUNT_KEY, ONE));
            log.trace("MongoDBTransientStore initialized: {}", config.getName());
        }
        return coll;
    }

    @Override
    public void shutdown() {
        if (coll != null) {
            coll = null;
            log.trace("MongoDBTransientStore shutdown: {}", config.getName());
        }
    }

    @Override
    public Stream<String> keyStream() {
        Bson filter = Filters.exists(ID_KEY);
        return StreamSupport.stream(getColl().find(filter).projection(include(ID_KEY)).spliterator(), false)
                            .map(doc -> doc.getString(ID_KEY));
    }

    @Override
    public long getStorageSize() {
        Document sum = getColl().aggregate(
                Collections.singletonList(group(null, sum(BLOB_SIZE_KEY, "$" + BLOB_SIZE_KEY)))).first();
        if (sum == null) {
            return 0;
        }
        Object obj = sum.get(BLOB_SIZE_KEY);
        if (obj instanceof Integer) {
            // aggregate comes as integer when there is no blob
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return (Long) obj;
        }
        throw new NuxeoException("Unknown aggregate type for storage size: " + sum.toJson());
    }

    @Override
    public void doGC() {
        BlobProvider bp = getBlobProvider();
        BinaryGarbageCollector gc = bp.getBinaryGarbageCollector();
        if (gc.isInProgress()) {
            log.info("GC {} on storage {} already in progress", gc.getId(), config.getName());
            return;
        }
        TransactionHelper.commitOrRollbackTransaction();
        boolean delete = false;
        log.debug("Starting GC on storage {}, listing blob on {}", config.getName(), gc.getId());
        try {
            gc.start();
            // if a concurrent user of the key/value store adds new keys after this point,
            // it's ok because the GC doesn't delete keys created after GC start
            log.debug("Marking blobs from Transient storage");
            Bson filter = gt(BLOB_COUNT_KEY, 0);
            StreamSupport.stream(getColl().find(filter).projection(include(BLOBS_KEY)).spliterator(), false)
                         .flatMap(d -> d.getList(BLOBS_KEY, Document.class)
                                        .stream()
                                        .map(b -> b.getString(KEY)))
                         .forEach(gc::mark);
            delete = true;
        } finally {
            // don't delete if there's an exception, but still stop the GC
            log.debug("GC delete={}", delete);
            if (gc.isInProgress()) {
                gc.stop(delete);
            } else {
                log.debug("No GC in progress");
            }
            TransactionHelper.startTransaction();
        }
        if (gc.getStatus().getGCDuration() > WARN_DURATION_MS_THRESHOLD) {
            log.warn("GC completed for {}: {}", config.getName(), gc.getStatus());
        } else {
            log.debug("GC completed for {}: {}", config.getName(), gc.getStatus());
        }
    }

    @Override
    public void removeAll() {
        log.debug("removeAll {}", config.getName());
        getColl().drop();
        coll = null;
        getColl();
        doGC();
        log.debug("removeAll {} completed", config.getName());
    }

    // ---------------------------------------------------------
    // TransientStore

    @Override
    public boolean exists(String key) {
        Bson filter = eq(ID_KEY, key);
        boolean ret = getColl().find(filter).projection(include(ID_KEY)).first() != null;
        log.trace("{} exists({}) -> {}", config::getName, () -> key, () -> ret);
        return ret;
    }

    @Override
    public void putParameter(String key, String parameter, Serializable value) {
        Document entry;
        Bson filter = eq(ID_KEY, key);
        if (!exists(key)) {
            entry = createEntry(key);
            entry.put(PARAMS_KEY, BasicDBObjectBuilder.start(parameter, serializeValue(value)).get());
            getColl().replaceOne(filter, entry, new ReplaceOptions().upsert(true));
            log.trace("{} putParameter({}, {}) -> create {}", config::getName, () -> key, () -> parameter, () -> entry);
        } else {
            entry = new Document();
            Document update = new Document();
            update.put(PARAMS_KEY + "." + parameter, serializeValue(value));
            update.put(COMPLETED_KEY, false);
            setTTL(update, firstLevelTTL);
            entry.put("$set", update);
            FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions();
            opts.upsert(true).projection(include(ID_KEY));
            if (log.isTraceEnabled()) {
                opts.returnDocument(ReturnDocument.AFTER).projection(include(PARAMS_KEY));
            }
            Document ret = getColl().findOneAndUpdate(filter, entry, opts);
            log.trace("{} putParameter({}, {}) -> update {}", config::getName, () -> key, () -> parameter, () -> ret);
        }
    }

    @Override
    public Serializable getParameter(String key, String parameter) {
        Bson filter = eq(ID_KEY, key);
        String param = PARAMS_KEY + "." + parameter;
        Document doc = getColl().find(filter).projection(include(param)).first();
        log.trace("{} getParameter({}, {}) -> {}", config::getName, () -> key, () -> parameter, () -> doc);
        if (doc == null || !doc.containsKey(PARAMS_KEY)) {
            return null;
        }
        return deSerializeValue(doc.get(PARAMS_KEY, Document.class).get(parameter));
    }

    protected Serializable deSerializeValue(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        byte[] bytes = ((Binary) value).getData();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (Serializable) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new NuxeoException(e);
        }
    }

    protected Object serializeValue(Serializable value) {
        return (value == null || value instanceof String) ? value : SerializationUtils.serialize(value);
    }

    @Override
    public void putParameters(String key, Map<String, Serializable> parameters) {
        Document entry;
        Bson filter = eq(ID_KEY, key);
        BasicDBObjectBuilder paramsBuilder = BasicDBObjectBuilder.start();
        if (parameters != null) {
            parameters.forEach((k, v) -> paramsBuilder.append(k, serializeValue(v)));
        }
        DBObject params = paramsBuilder.get();
        if (!exists(key)) {
            entry = createEntry(key);
            entry.put(PARAMS_KEY, params);
            getColl().replaceOne(filter, entry, new ReplaceOptions().upsert(true));
            log.trace("{} putParameters({}, _) -> create {}", config::getName, () -> key, () -> entry);
        } else {
            entry = new Document();
            Document update = new Document();
            update.put(PARAMS_KEY, params);
            update.put(COMPLETED_KEY, false);
            setTTL(update, firstLevelTTL);
            entry.put("$set", update);
            FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions();
            opts.upsert(true).projection(include(ID_KEY));
            if (log.isTraceEnabled()) {
                opts.returnDocument(ReturnDocument.AFTER).projection(include(PARAMS_KEY));
            }
            Document ret = getColl().findOneAndUpdate(filter, entry, opts);
            log.trace("{} putParameters({}, _) -> update {}", config::getName, () -> key, () -> ret);
        }
    }

    @Override
    public Map<String, Serializable> getParameters(String key) {
        Bson filter = eq(ID_KEY, key);
        Document doc = getColl().find(filter).projection(include(PARAMS_KEY)).first();
        log.trace("{} getParameters({}) -> {}", config::getName, () -> key, () -> doc);
        if (doc == null) {
            return null;
        }
        Document params = (Document) doc.get(PARAMS_KEY);
        if (params == null) {
            return Collections.emptyMap();
        }
        HashMap<String, Serializable> ret = new HashMap<>(params.size());
        params.forEach((k, v) -> ret.put(k, deSerializeValue(v)));
        return ret;
    }

    @Override
    public void putBlobs(String key, List<Blob> blobs) {
        if (absoluteMaxSize >= 0 && getStorageSize() >= absoluteMaxSize) {
            // do the costly computation of the exact storage size if needed
            doGC();
            if (getStorageSize() >= absoluteMaxSize) {
                throw new MaximumTransientSpaceExceeded();
            }
        }
        BlobProvider bp = getBlobProvider();
        long totalSize = 0;
        BasicDBList docBlobs = new BasicDBList();
        for (Blob blob : blobs) {
            long size = blob.getLength();
            if (size >= 0) {
                totalSize += size;
            }
            // store blob
            String blobKey;
            try {
                blobKey = bp.writeBlob(blob);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
            // compute blob data
            Document blobMap = new Document();
            blobMap.put(KEY, blobKey);
            blobMap.put(MIMETYPE, blob.getMimeType());
            blobMap.put(ENCODING, blob.getEncoding());
            blobMap.put(FILENAME, blob.getFilename());
            blobMap.put(LENGTH, size);
            blobMap.put(DIGEST, blob.getDigest());
            docBlobs.add(blobMap);
        }
        Document entry;
        Bson filter = eq(ID_KEY, key);
        if (!exists(key)) {
            entry = createEntry(key);
            entry.put(BLOBS_KEY, docBlobs);
            entry.put(BLOB_SIZE_KEY, totalSize);
            entry.put(BLOB_COUNT_KEY, blobs.size());
            getColl().replaceOne(filter, entry, new ReplaceOptions().upsert(true));
            log.trace("{} putBlobs({}, _) -> create {}", config::getName, () -> key, () -> entry);
        } else {
            entry = new Document();
            Document update = new Document();
            update.put(BLOBS_KEY, docBlobs);
            update.put(BLOB_SIZE_KEY, totalSize);
            update.put(BLOB_COUNT_KEY, blobs.size());
            update.put(COMPLETED_KEY, false);
            setTTL(update, firstLevelTTL);
            entry.put("$set", update);
            FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions();
            opts.upsert(true).projection(include(ID_KEY));
            if (log.isTraceEnabled()) {
                opts.returnDocument(ReturnDocument.AFTER).projection(include(BLOBS_KEY, BLOB_SIZE_KEY));
            }
            Document ret = getColl().findOneAndUpdate(filter, entry, opts);
            log.trace("{} putBlobs({}, _) -> update {}", config::getName, () -> key, () -> ret);
        }
    }

    @Override
    public List<Blob> getBlobs(String key) {
        Bson filter = eq(ID_KEY, key);
        Document doc = getColl().find(filter).projection(include(BLOBS_KEY)).first();
        log.trace("{} getBlobs({}) -> {}", config::getName, () -> key, () -> doc);
        if (doc == null) {
            return null;
        }
        List<Document> blobsInfo = doc.getList(BLOBS_KEY, Document.class);
        if (blobsInfo == null) {
            return Collections.emptyList();
        }
        BlobProvider bp = getBlobProvider();
        List<Blob> blobs = new ArrayList<>(blobsInfo.size());
        for (Document info : blobsInfo) {
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = info.getString(KEY);
            blobInfo.digest = info.getString(DIGEST);
            blobInfo.filename = info.getString(FILENAME);
            blobInfo.length = info.getLong(LENGTH);
            blobInfo.encoding = info.getString(ENCODING);
            blobInfo.mimeType = info.getString(MIMETYPE);
            try {
                Blob blob = bp.readBlob(blobInfo);
                blobs.add(blob);
            } catch (IOException e) {
                // ignore, the blob was removed from the blob provider
                // maybe by a concurrent GC from this transient store
                // or from the blob provider itself (if it's incorrectly shared)
                log.debug("Failed to read blob: {} in blob provider: {}  for transient store: {}", blobInfo.digest,
                        blobProviderId, config.getName());

            }
        }
        return blobs;
    }

    @Override
    public long getSize(String key) {
        Bson filter = eq(ID_KEY, key);
        Document doc = getColl().find(filter).projection(include(BLOB_SIZE_KEY)).first();
        log.trace("{} getSize({}) -> {}", config::getName, () -> key, () -> doc);
        return doc == null ? -1L : doc.getLong(BLOB_SIZE_KEY);
    }

    @Override
    public boolean isCompleted(String key) {
        Bson filter = and(eq(ID_KEY, key), eq(COMPLETED_KEY, true));
        boolean ret = getColl().find(filter).projection(include(ID_KEY)).first() != null;
        log.trace("{} isCompleted({}) -> {}", config::getName, () -> key, () -> ret);
        return ret;
    }

    @Override
    public void setCompleted(String key, boolean completed) {
        Bson filter = eq(ID_KEY, key);
        Document update = new Document();
        update.put(COMPLETED_KEY, completed);
        setTTL(update, completed ? firstLevelTTL : secondLevelTTL);
        Document entry = new Document();
        entry.put("$set", update);
        FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions();
        opts.upsert(false).returnDocument(ReturnDocument.AFTER).projection(include(COMPLETED_KEY));
        Document ret = getColl().findOneAndUpdate(filter, entry, opts);
        log.trace("{} setCompleted({}, {}) -> {}", config::getName, () -> key, () -> completed, () -> ret);
    }

    protected void setTTL(Document entry, int ttlSeconds) {
        Date ttl = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);
        entry.put(TTL_KEY, ttl);
    }

    protected Document createEntry(String key) {
        Document entry = new Document();
        entry.put(ID_KEY, key);
        entry.put(COMPLETED_KEY, false);
        setTTL(entry, firstLevelTTL);
        return entry;
    }

    @Override
    public void remove(String key) {
        Bson filter = eq(ID_KEY, key);
        DeleteResult ret = getColl().deleteOne(filter);
        log.trace("{} remove({}) -> {}", config::getName, () -> key, () -> ret);
    }

    @Override
    public void release(String key) {
        if (!exists(key)) {
            log.trace("{} release({}) key doesn't exist", config::getName, () -> key);
            return;
        }
        if (targetMaxSize >= 0 && getStorageSize() > targetMaxSize) {
            doGC();
            if (getStorageSize() > targetMaxSize) {
                remove(key);
                return;
            }
        }
        Document update = new Document();
        setTTL(update, secondLevelTTL);
        update.put(COMPLETED_KEY, true);
        Document entry = new Document();
        entry.put("$set", update);
        Bson filter = eq(ID_KEY, key);
        FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions().upsert(false).projection(include(ID_KEY));
        getColl().findOneAndUpdate(filter, entry, opts);
        log.trace("{} release({})", config::getName, () -> key);
    }

    public BlobProvider getBlobProvider() {
        BlobProvider blobProvider = Framework.getService(BlobManager.class)
                                             .getBlobProviderWithNamespace(blobProviderId, defaultBlobProviderId);
        if (blobProvider == null) {
            throw new NuxeoException("No blob provider with id: " + blobProviderId);
        }
        if (!blobProvider.isTransient()) {
            throw new NuxeoException("Blob provider: " + blobProviderId + " used for MongoDBTransientStore store: "
                    + config.getName() + " must be configured as transient");
        }
        return blobProvider;
    }

}
