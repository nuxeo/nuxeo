/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.mongodb.ts;

import static com.mongodb.client.model.Filters.empty;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static org.nuxeo.ecm.core.mongodb.MongoDBConstants.ID_KEY;
import static org.nuxeo.ecm.core.mongodb.MongoDBConstants.SET;
import static org.nuxeo.ecm.core.mongodb.MongoDBConstants.SET_ON_INSERT;
import static org.nuxeo.ecm.core.mongodb.MongoDBConstants.UNSET;
import static org.nuxeo.runtime.ts.TransientDataStoreDescriptor.TTLPolicy.CREATED;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.types.Binary;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.ts.AbstractTransientDataStoreProvider;
import org.nuxeo.runtime.ts.TransientDataStoreDescriptor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;

/**
 * @since 2025.8
 */
public class MongoDBTransientDataStore extends AbstractTransientDataStoreProvider {

    private static final Logger log = LogManager.getLogger(MongoDBTransientDataStore.class);

    protected static final String TRANSIENTDATA_DATABASE_ID = "transientdata";

    protected static final String COLLECTION_PROP = "collection";

    protected static final String COLLECTION_DEFAULT = "transientdata";

    protected static final String TTL_KEY = "ttl";

    protected volatile MongoCollection<Document> coll;

    public MongoDBTransientDataStore(TransientDataStoreDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected <S extends Serializable> void doPut(String key, String parameter, S value) {
        Document document = prepareUpdateDocument(key);
        document.get(SET, Document.class).append(parameter, serializeValue(value));
        getColl().updateOne(eq(ID_KEY, key), document, new UpdateOptions().upsert(true));
    }

    @Override
    protected <S extends Serializable> void doPutAll(String key, Map<String, S> parameters) {
        Document document = prepareUpdateDocument(key);
        Document setDocument = document.get(SET, Document.class);
        parameters.entrySet()
                  .stream()
                  .filter(entry -> entry.getValue() != null)
                  .forEach(entry -> setDocument.append(entry.getKey(), serializeValue(entry.getValue())));
        getColl().updateOne(eq(ID_KEY, key), document, new UpdateOptions().upsert(true));
    }

    protected Document prepareUpdateDocument(String key) {
        Document setOnInsertDocument = new Document(ID_KEY, key);
        Document setDocument = new Document();
        Date ttlDate = new Date(System.currentTimeMillis() + descriptor.getTtl().toMillis());
        (descriptor.getTtlPolicy() == CREATED ? setOnInsertDocument : setDocument).put(TTL_KEY, ttlDate);
        return new Document(SET_ON_INSERT, setOnInsertDocument).append(SET, setDocument);
    }

    @Override
    protected boolean doExists(String key) {
        return getColl().find(eq(ID_KEY, key)).projection(include(ID_KEY)).first() != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> S doGet(String key, String parameter) {
        Document document = getColl().findOneAndUpdate(eq(ID_KEY, key), prepareUpdateDocument(key),
                new FindOneAndUpdateOptions().upsert(false).returnDocument(BEFORE).projection(include(parameter)));
        if (document == null || !document.containsKey(parameter)) {
            return null;
        }
        return (S) deserializeValue(document.get(parameter));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> Map<String, S> doGetAll(String key) {
        Document document = getColl().findOneAndUpdate(eq(ID_KEY, key), prepareUpdateDocument(key),
                new FindOneAndUpdateOptions().upsert(false)
                                             .returnDocument(BEFORE)
                                             .projection(exclude(ID_KEY, TTL_KEY)));
        if (document == null || document.isEmpty()) {
            return null;
        }
        return (Map<String, S>) deserializeValue(document);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> S doRemove(String key, String parameter) {
        Document previousDocument = getColl().findOneAndUpdate(eq(ID_KEY, key),
                new Document(UNSET, new Document(parameter, 1)),
                new FindOneAndUpdateOptions().returnDocument(BEFORE).projection(exclude(ID_KEY, TTL_KEY)));
        if (previousDocument == null || !previousDocument.containsKey(parameter)) {
            return null;
        }
        if (previousDocument.size() == 1) {
            getColl().deleteOne(eq(ID_KEY, key));
        }
        return (S) deserializeValue(previousDocument.get(parameter));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> Map<String, S> doRemoveAll(String key) {
        Document previousDocument = getColl().findOneAndDelete(eq(ID_KEY, key),
                new FindOneAndDeleteOptions().projection(exclude(ID_KEY, TTL_KEY)));
        if (previousDocument == null) {
            return null;
        }
        return (Map<String, S>) deserializeValue(previousDocument);
    }

    @Override
    public void clear() {
        getColl().deleteMany(empty());
    }

    protected MongoCollection<Document> getColl() {
        if (coll == null) {
            synchronized (this) {
                if (coll == null) {
                    String collectionName = descriptor.getProviderProperty(COLLECTION_PROP).orElse(COLLECTION_DEFAULT)
                            + "." + descriptor.getName();
                    MongoDatabase database = Framework.getService(MongoDBConnectionService.class)
                                                      .getDatabase(TRANSIENTDATA_DATABASE_ID);
                    coll = database.getCollection(collectionName);
                    log.trace("The store with name: {} using collection: {} is initializing", descriptor::getName,
                            coll::getNamespace);
                    MongoDBConnectionHelper.ensureCollectionExists(database, collectionName);
                    // make sure TTL works by creating the appropriate index
                    IndexOptions indexOptions = new IndexOptions().expireAfter(0L, TimeUnit.SECONDS);
                    coll.createIndex(new Document(TTL_KEY, 1), indexOptions);
                    log.trace("The store with name: {} is initialized", descriptor::getName);
                }
            }
        }
        return coll;
    }

    protected Object serializeValue(Object value) {
        return switch (value) {
            case Collection<?> collection ->
                collection.stream().filter(Objects::nonNull).map(this::serializeValue).toList();
            case Object[] array -> {
                if (!array.getClass().getComponentType().isPrimitive()) {
                    throw new IllegalArgumentException(
                            "Only array of primitive types is supported, value: " + Arrays.toString(array));
                }
                yield Stream.of(array).filter(Objects::nonNull).map(this::serializeValue).toList();
            }
            case Map<?, ?> map -> map.entrySet()
                                     .stream()
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toMap(entry -> entry.getKey().toString(),
                                             entry -> serializeValue(entry.getValue()), (e1, e2) -> {
                                                 // it should not happen has the keys are already unique
                                                 throw new IllegalStateException(String.format(
                                                         "Duplicate key (attempted merging values: %s and %s)", e1,
                                                         e2));
                                             }, Document::new));
            case Boolean bool -> bool;
            case Number number -> number;
            case String string -> string;
            case Serializable serializable -> {
                try {
                    byte[] bytes = SerializationUtils.serialize(serializable);
                    yield new Binary(BsonBinarySubType.USER_DEFINED, bytes);
                } catch (SerializationException e) {
                    throw new NuxeoException("Unable to serialize value: " + serializable, e);
                }
            }
            case null, default ->
                throw new IllegalArgumentException("Non Serializable object is not supported, value: " + value);
        };
    }

    protected Serializable deserializeValue(Object value) {
        return (Serializable) switch (value) {
            case Collection<?> collection -> collection.stream().map(this::deserializeValue).toList();
            case Object[] array -> array; // we only serialize primitive array types
            case Map<?, ?> map ->
                map.entrySet()
                   .stream()
                   .collect(Collectors.toMap(Map.Entry::getKey, entry -> deserializeValue(entry.getValue())));
            case Binary binary when BsonBinarySubType.USER_DEFINED.getValue() == binary.getType() -> {
                try {
                    byte[] bytes = binary.getData();
                    yield SerializationUtils.deserialize(bytes);
                } catch (SerializationException e) {
                    throw new NuxeoException("Unable to deserializable the binary object", e);
                }
            }
            default -> value; // Boolean, Number, String...@
        };
    }
}
