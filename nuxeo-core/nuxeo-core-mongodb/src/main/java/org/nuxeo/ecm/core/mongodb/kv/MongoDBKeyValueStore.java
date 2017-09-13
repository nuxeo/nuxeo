/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.mongodb.kv;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.nuxeo.ecm.core.kv.KeyValueStoreDescriptor;
import org.nuxeo.ecm.core.kv.KeyValueStoreProvider;
import org.nuxeo.ecm.core.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.api.Framework;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * MongoDB implementation of a Key/Value Store Provider.
 * <p>
 * The following configuration properties are available:
 * <ul>
 * <li>collection: the MongoDB collection prefix to use, the default is "kv". This will be followed by the Store name.
 * </ul>
 *
 * @since 9.3
 */
public class MongoDBKeyValueStore implements KeyValueStoreProvider {

    private static final Log log = LogFactory.getLog(MongoDBKeyValueStore.class);

    public static final String KEYVALUE_CONNECTION_ID = "keyvalue";

    public static final String COLLECTION_PROP = "collection";

    public static final String COLLECTION_DEFAULT = "kv";

    public static final String ID_KEY = "_id";

    public static final String VALUE_KEY = "v";

    public static final String TTL_KEY = "ttl";

    public static final Double ONE = Double.valueOf(1);

    protected String name;

    protected MongoCollection<Document> coll;

    protected static ThreadLocal<CharsetDecoder> UTF_8_DECODERS = ThreadLocal.withInitial(
            () -> UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(
                    CodingErrorAction.REPORT));

    @Override
    public void initialize(KeyValueStoreDescriptor descriptor) {
        name = descriptor.name;
        Map<String, String> properties = descriptor.getProperties();
        // find which collection prefix to use
        String collectionName = properties.get(COLLECTION_PROP);
        if (StringUtils.isBlank(collectionName)) {
            collectionName = COLLECTION_DEFAULT;
        }
        collectionName += "." + name;
        // get a connection to MongoDB
        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        MongoDatabase database = mongoService.getDatabase(KEYVALUE_CONNECTION_ID);
        coll = database.getCollection(collectionName);
        // make sure TTL works by creating the appropriate index
        IndexOptions indexOptions = new IndexOptions().expireAfter(Long.valueOf(0), TimeUnit.SECONDS);
        coll.createIndex(new Document(TTL_KEY, ONE), indexOptions);
    }

    @Override
    public void close() {
        if (coll != null) {
            coll = null;
        }
    }

    @Override
    public void clear() {
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CLEAR");
        }
        coll.deleteMany(new Document());
    }

    // if possible, store the bytes as a UTF-8 string
    protected static Object toStorage(byte[] bytes) {
        try {
            return UTF_8_DECODERS.get().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            // could not decode as UTF-8, use a binary
            return new Binary(bytes);
        }
    }

    protected static byte[] fromStorage(Object value) {
        if (value instanceof Binary) {
            return ((Binary) value).getData();
        } else if (value instanceof String) {
            return ((String) value).getBytes(UTF_8);
        } else {
            throw new UnsupportedOperationException(value.getClass().getName());
        }
    }

    @Override
    public byte[] get(String key) {
        Bson filter = eq(ID_KEY, key);
        Document doc = coll.find(filter).first();
        if (doc == null) {
            if (log.isTraceEnabled()) {
                log.trace("MongoDB: GET " + key + " = null");
            }
            return null;
        }
        Object value = doc.get(VALUE_KEY);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: GET " + key + " = " + value);
        }
        return fromStorage(value);
    }

    protected Date getDateFromTTL(long ttl) {
        return new Date(System.currentTimeMillis() + ttl * 1000);
    }

    @Override
    public void put(String key, byte[] bytes, long ttl) {
        Bson filter = eq(ID_KEY, key);
        if (bytes == null) {
            if (log.isTraceEnabled()) {
                log.trace("MongoDB: DEL " + key);
            }
            coll.deleteOne(filter);
        } else {
            Object value = toStorage(bytes);
            Document doc = new Document(VALUE_KEY, value);
            if (ttl != 0) {
                doc.append(TTL_KEY, getDateFromTTL(ttl));
            }
            if (log.isTraceEnabled()) {
                log.trace("MongoDB: PUT " + key + " = " + value + (ttl == 0 ? "" : " (TTL " + ttl + ")"));
            }
            coll.replaceOne(filter, doc, new UpdateOptions().upsert(true));
        }
    }

    @Override
    public boolean setTTL(String key, long ttl) {
        Bson filter = eq(ID_KEY, key);
        Bson update;
        if (ttl == 0) {
            update = unset(TTL_KEY);
        } else {
            update = set(TTL_KEY, getDateFromTTL(ttl));
        }
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: SETTTL " + key + " = " + ttl);
        }
        UpdateResult res = coll.updateOne(filter, update);
        return res.getModifiedCount() == 1;
    }

    @Override
    public boolean compareAndSet(String key, byte[] expected, byte[] value) {
        Bson filter = eq(ID_KEY, key);
        if (expected == null && value == null) {
            // check that document doesn't exist
            Document doc = coll.find(filter).first();
            boolean set = doc == null;
            if (log.isTraceEnabled()) {
                if (set) {
                    log.trace("MongoDB: TEST " + key + " = null ? NOP");
                } else {
                    log.trace("MongoDB: TEST " + key + " = null ? FAILED");
                }
            }
            return set;
        } else if (expected == null) {
            // set value if no document already exists: regular insert
            Object storageValue = toStorage(value);
            Document doc = new Document(ID_KEY, key).append(VALUE_KEY, storageValue);
            boolean set;
            try {
                coll.insertOne(doc);
                set = true;
            } catch (MongoWriteException e) {
                if (ErrorCategory.fromErrorCode(e.getCode()) != ErrorCategory.DUPLICATE_KEY) {
                    throw e;
                }
                set = false;
            }
            if (log.isTraceEnabled()) {
                if (set) {
                    log.trace("MongoDB: TEST " + key + " = null ? SET " + storageValue);
                } else {
                    log.trace("MongoDB: TEST " + key + " = null ? FAILED");
                }
            }
            return set;
        } else if (value == null) {
            // delete if previous value exists
            filter = and(filter, eq(VALUE_KEY, toStorage(expected)));
            DeleteResult res = coll.deleteOne(filter);
            boolean set = res.getDeletedCount() == 1;
            if (log.isTraceEnabled()) {
                if (set) {
                    log.trace("MongoDB: TEST " + key + " = " + toStorage(expected) + " ? DEL");
                } else {
                    log.trace("MongoDB: TEST " + key + " = " + toStorage(expected) + " ? FAILED");
                }
            }
            return set;
        } else {
            // replace if previous value exists
            filter = and(filter, eq(VALUE_KEY, toStorage(expected)));
            Object storageValue = toStorage(value);
            Document doc = new Document(VALUE_KEY, storageValue);
            UpdateResult res = coll.replaceOne(filter, doc);
            boolean set = res.getModifiedCount() == 1;
            if (log.isTraceEnabled()) {
                if (set) {
                    log.trace("MongoDB: TEST " + key + " = " + toStorage(expected) + " ? SET " + storageValue);
                } else {
                    log.trace("MongoDB: TEST " + key + " = " + toStorage(expected) + " ? FAILED");
                }
            }
            return set;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }

}
