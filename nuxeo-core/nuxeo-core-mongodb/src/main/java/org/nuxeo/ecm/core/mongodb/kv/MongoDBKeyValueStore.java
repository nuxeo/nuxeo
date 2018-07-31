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
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.CharacterCodingException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.AbstractKeyValueStoreProvider;
import org.nuxeo.runtime.kv.KeyValueStoreDescriptor;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.Block;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReturnDocument;
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
public class MongoDBKeyValueStore extends AbstractKeyValueStoreProvider {

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

    @Override
    public void initialize(KeyValueStoreDescriptor descriptor) {
        name = descriptor.name;
        Map<String, String> properties = descriptor.properties;
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
    public Stream<String> keyStream() {
        return StreamSupport.stream(coll.find().projection(include(ID_KEY)).spliterator(), false)
                            .map(doc -> doc.getString(ID_KEY));
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
            return bytesToString(bytes);
        } catch (CharacterCodingException e) {
            // could not decode as UTF-8, use a binary
            return new Binary(bytes);
        }
    }

    protected byte[] toBytes(Object value) {
        if (value instanceof String) {
            return ((String) value).getBytes(UTF_8);
        } else if (value instanceof Long) {
            return ((Long) value).toString().getBytes(UTF_8);
        } else if (value instanceof Binary) {
            return ((Binary) value).getData();
        }
        return null;
    }

    protected String toString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Long) {
            return ((Long) value).toString();
        } else if (value instanceof Binary) {
            byte[] bytes = ((Binary) value).getData();
            try {
                return bytesToString(bytes);
            } catch (CharacterCodingException e) {
                return null;
            }
        }
        return null;
    }

    protected Long toLong(Object value) throws NumberFormatException { // NOSONAR
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            return Long.valueOf((String) value);
        } else if (value instanceof Binary) {
            byte[] bytes = ((Binary) value).getData();
            return bytesToLong(bytes);
        }
        return null;
    }

    @Override
    public byte[] get(String key) {
        Object value = getObject(key);
        if (value == null) {
            return null;
        }
        byte[] bytes = toBytes(value);
        if (bytes != null) {
            return bytes;
        }
        throw new UnsupportedOperationException(value.getClass().getName());
    }

    @Override
    public String getString(String key) {
        Object value = getObject(key);
        if (value == null) {
            return null;
        }
        String stringValue = toString(value);
        if (stringValue != null) {
            return stringValue;
        }
        throw new IllegalArgumentException("Value is not a String for key: " + key);
    }

    @Override
    public Long getLong(String key) throws NumberFormatException { // NOSONAR
        Object value = getObject(key);
        if (value == null) {
            return null;
        }
        Long longValue = toLong(value);
        if (longValue != null) {
            return longValue;
        }
        throw new NumberFormatException("Value is not a Long for key: " + key);
    }

    protected Object getObject(String key) {
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
        return value;
    }

    @Override
    public Map<String, byte[]> get(Collection<String> keys) {
        Map<String, byte[]> map = new HashMap<>(keys.size());
        Block<Document> block = doc -> {
            String key = doc.getString(ID_KEY);
            Object value = doc.get(VALUE_KEY);
            if (value != null) {
                byte[] bytes = toBytes(value);
                if (bytes == null) {
                    throw new UnsupportedOperationException(String.format(
                            "Value of class %s is not supported for key: %s", value.getClass().getName(), key));
                }
                map.put(key, bytes);
            }
        };
        findByKeys(keys, block);
        return map;
    }

    @Override
    public Map<String, String> getStrings(Collection<String> keys) {
        Map<String, String> map = new HashMap<>(keys.size());
        Block<Document> block = doc -> {
            String key = doc.getString(ID_KEY);
            Object value = doc.get(VALUE_KEY);
            if (value != null) {
                String strValue = toString(value);
                if (strValue == null) {
                    throw new IllegalArgumentException("Value is not a String for key: " + key);
                }
                map.put(key, strValue);
            }
        };
        findByKeys(keys, block);
        return map;
    }

    @Override
    public Map<String, Long> getLongs(Collection<String> keys) throws NumberFormatException { // NOSONAR
        Map<String, Long> map = new HashMap<>(keys.size());
        Block<Document> block = doc -> {
            String key = doc.getString(ID_KEY);
            Object value = doc.get(VALUE_KEY);
            if (value != null) {
                Long longValue = toLong(value);
                if (longValue == null) {
                    throw new IllegalArgumentException("Value is not a Long for key: " + key);
                }
                map.put(key, longValue);
            }
        };
        findByKeys(keys, block);
        return map;
    }

    /**
     * @since 9.10
     */
    protected void findByKeys(Collection<String> keys, Block<Document> block) {
        coll.find(in(ID_KEY, keys)).projection(include(ID_KEY, VALUE_KEY)).forEach(block);
    }

    protected Date getDateFromTTL(long ttl) {
        return new Date(System.currentTimeMillis() + ttl * 1000);
    }

    @Override
    public void put(String key, byte[] bytes, long ttl) {
        put(key, toStorage(bytes), ttl);
    }

    @Override
    public void put(String key, String string) {
        put(key, (Object) string, 0);
    }

    @Override
    public void put(String key, String string, long ttl) {
        put(key, (Object) string, ttl);
    }

    @Override
    public void put(String key, Long value) {
        put(key, (Object) value, 0);
    }

    @Override
    public void put(String key, Long value, long ttl) {
        put(key, (Object) value, ttl);
    }

    protected void put(String key, Object value, long ttl) {
        Bson filter = eq(ID_KEY, key);
        if (value == null) {
            if (log.isTraceEnabled()) {
                log.trace("MongoDB: DEL " + key);
            }
            coll.deleteOne(filter);
        } else {
            Document doc = new Document(VALUE_KEY, value);
            addTTL(doc, ttl);
            if (log.isTraceEnabled()) {
                log.trace("MongoDB: PUT " + key + " = " + value + (ttl == 0 ? "" : " (TTL " + ttl + ")"));
            }
            coll.replaceOne(filter, doc, new UpdateOptions().upsert(true));
        }
    }

    protected void addTTL(Document doc, long ttl) {
        if (ttl != 0) {
            doc.append(TTL_KEY, getDateFromTTL(ttl));
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
    public boolean compareAndSet(String key, byte[] expected, byte[] value, long ttl) {
        return compareAndSet(key, toStorage(expected), toStorage(value), ttl);
    }

    @Override
    public boolean compareAndSet(String key, String expected, String value, long ttl) {
        return compareAndSet(key, (Object) expected, (Object) value, ttl);
    }

    protected boolean compareAndSet(String key, Object expected, Object value, long ttl) {
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
            Document doc = new Document(ID_KEY, key).append(VALUE_KEY, value);
            addTTL(doc, ttl);
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
                    log.trace("MongoDB: TEST " + key + " = null ? SET " + value);
                } else {
                    log.trace("MongoDB: TEST " + key + " = null ? FAILED");
                }
            }
            return set;
        } else if (value == null) {
            // delete if previous value exists
            filter = and(filter, eq(VALUE_KEY, expected));
            DeleteResult res = coll.deleteOne(filter);
            boolean set = res.getDeletedCount() == 1;
            if (log.isTraceEnabled()) {
                if (set) {
                    log.trace("MongoDB: TEST " + key + " = " + expected + " ? DEL");
                } else {
                    log.trace("MongoDB: TEST " + key + " = " + expected + " ? FAILED");
                }
            }
            return set;
        } else {
            // replace if previous value exists
            filter = and(filter, eq(VALUE_KEY, expected));
            Document doc = new Document(VALUE_KEY, value);
            addTTL(doc, ttl);
            UpdateResult res = coll.replaceOne(filter, doc);
            boolean set = res.getModifiedCount() == 1;
            if (log.isTraceEnabled()) {
                if (set) {
                    log.trace("MongoDB: TEST " + key + " = " + expected + " ? SET " + value);
                } else {
                    log.trace("MongoDB: TEST " + key + " = " + expected + " ? FAILED");
                }
            }
            return set;
        }
    }

    @Override
    public long addAndGet(String key, long delta) throws NumberFormatException { // NOSONAR
        Bson filter = eq(ID_KEY, key);
        Bson update = inc(VALUE_KEY, Long.valueOf(delta));
        Document result;
        try {
            result = coll.findOneAndUpdate(filter, update,
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        } catch (MongoCommandException e) {
            // Cannot apply $inc to a value of non-numeric type; code: 16837
            if (!e.getMessage().contains("Cannot apply $inc")) {
                throw new NuxeoException(e);
            }
            // for compatibility with other backends that don't have datatypes,
            // try to interpret the value as the string representation of an integer
            // (this keeps the underlying format as a String though)
            return addAndGetGeneric(key, delta);
        }
        if (result == null) {
            throw new NuxeoException("Unexpected null result, upsert failed for key: " + key);
        }
        return ((Long) result.get(VALUE_KEY)).longValue();
    }

    // works on any representation that can be converted to a Long
    protected long addAndGetGeneric(String key, long delta) throws NumberFormatException { // NOSONAR
        for (;;) {
            Object value = getObject(key);
            long result;
            if (value == null) {
                result = delta;
            } else {
                Long base = toLong(value);
                if (base == null) {
                    throw new NumberFormatException("Value is not a Long for key: " + key);
                }
                result = base.longValue() + delta;
            }
            Object newValue = Long.valueOf(result);
            if (compareAndSet(key, value, newValue, 0)) {
                return result;
            }
            // else loop to try again
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }

}
