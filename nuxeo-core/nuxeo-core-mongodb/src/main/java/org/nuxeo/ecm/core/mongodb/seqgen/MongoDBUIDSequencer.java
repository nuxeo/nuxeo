/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.mongodb.seqgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.uidgen.AbstractUIDSequencer;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

/**
 * MongoDB implementation of {@link UIDSequencer}.
 * <p>
 * We use MongoDB upsert feature to provide a sequencer.
 *
 * @since 9.1
 */
public class MongoDBUIDSequencer extends AbstractUIDSequencer {

    private static final Log log = LogFactory.getLog(MongoDBUIDSequencer.class);

    public static final String SEQUENCE_DATABASE_ID = "sequence";

    public static final String COLLECTION_NAME_PROPERTY = "nuxeo.mongodb.seqgen.collection.name";

    public static final String DEFAULT_COLLECTION_NAME = "sequence";

    public static final Long ONE = Long.valueOf(1L);

    public static final String SEQUENCE_VALUE_FIELD = "sequence";

    protected MongoCollection<Document> coll;

    @Override
    public void init() {
        getCollection();
    }

    protected MongoCollection<Document> getCollection() {
        if (coll == null) {
            // Get collection name
            ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
            String collName = configurationService.getProperty(COLLECTION_NAME_PROPERTY, DEFAULT_COLLECTION_NAME);
            // Get a connection to MongoDB
            MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
            // Get database
            MongoDatabase database = mongoService.getDatabase(SEQUENCE_DATABASE_ID);
            // Get collection
            coll = database.getCollection(collName);
        }
        return coll;
    }

    @Override
    public int getNext(String key) {
        return (int) getNextLong(key);
    }

    @Override
    public long getNextLong(String key) {
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
        Bson filter = Filters.eq(MongoDBSerializationHelper.MONGODB_ID, key);
        Bson update = Updates.inc(SEQUENCE_VALUE_FIELD, ONE);
        Document sequence = getCollection().findOneAndUpdate(filter, update, options);
        // If sequence is null, we need to create it
        if (sequence == null) {
            try {
                sequence = new Document();
                sequence.put(MongoDBSerializationHelper.MONGODB_ID, key);
                sequence.put(SEQUENCE_VALUE_FIELD, ONE);
                getCollection().insertOne(sequence);
            } catch (MongoWriteException e) {
                // There was a race condition - just re-run getNextLong
                if (log.isTraceEnabled()) {
                    log.trace("There was a race condition during '" + key + "' sequence insertion", e);
                }
                return getNextLong(key);
            }
        }
        return ((Long) MongoDBSerializationHelper.bsonToFieldMap(sequence).get(SEQUENCE_VALUE_FIELD)).longValue();
    }

    @Override
    public void dispose() {
        if (coll != null) {
            coll = null;
        }
    }

}
