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
 *     Funsho David
 *
 */

package org.nuxeo.directory.mongodb;

import static org.nuxeo.directory.mongodb.MongoDBSerializationHelper.MONGODB_ID;
import static org.nuxeo.directory.mongodb.MongoDBSerializationHelper.MONGODB_SEQ;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_ALWAYS;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_ON_MISSING_COLUMNS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

/**
 * MongoDB implementation of a {@link Directory}
 *
 * @since 9.1
 */
public class MongoDBDirectory extends AbstractDirectory {

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p>
     * The connection id will be {@code directory/[DIRECTORY_NAME]}.
     *
     * @since 10.10
     */
    public static final String DIRECTORY_CONNECTION_PREFIX = "directory/";

    /**
     * @since 10.10-HF16
     */
    protected String databaseID;

    protected MongoDatabase database;

    protected MongoCollection<Document> collection;

    protected MongoCollection<Document> countersCollection;

    public MongoDBDirectory(MongoDBDirectoryDescriptor descriptor) {
        super(descriptor, MongoDBReference.class);

        // cache fallback
        fallbackOnDefaultCache();
    }

    @Override
    public MongoDBDirectoryDescriptor getDescriptor() {
        return (MongoDBDirectoryDescriptor) descriptor;
    }

    @Override
    protected void addReferences() {
        super.addReferences();
        // add backward compat MongoDB references
        MongoDBReferenceDescriptor[] descs = getDescriptor().getMongoDBReferences();
        if (descs != null) {
            Arrays.stream(descs).map(MongoDBReference::new).forEach(this::addReference);
        }
    }

    @Override
    public MongoDBSession getSession() {
        MongoDBSession session = new MongoDBSession(this);
        addSession(session);
        return session;
    }

    @Override
    public boolean isMultiTenant() {
        return schemaFieldMap.containsKey(TENANT_ID_FIELD);
    }

    @Override
    public void initialize() {
        super.initialize();

        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        databaseID = DIRECTORY_CONNECTION_PREFIX + getName();
        database = mongoService.getDatabase(databaseID);
        collection = database.getCollection(getName());
        String countersCollectionName = getName() + ".counters";
        countersCollection = database.getCollection(countersCollectionName);

        // Initialize counters collection if autoincrement enabled
        if (descriptor.isAutoincrementIdField() && !hasCollection(countersCollectionName)) {
            Map<String, Object> seq = new HashMap<>();
            seq.put(MONGODB_ID, getName());
            seq.put(MONGODB_SEQ, 0L);
            getCountersCollection().insertOne(MongoDBSerializationHelper.fieldMapToBson(seq));
        }

        String policy = descriptor.getCreateTablePolicy();
        boolean dropCollection = false;
        boolean loadData = false;

        switch (policy) {
        case CREATE_TABLE_POLICY_ALWAYS:
            dropCollection = true;
            loadData = true;
            break;
        case CREATE_TABLE_POLICY_ON_MISSING_COLUMNS:
            // As MongoDB does not have the notion of columns, only load data if collection doesn't exist
            if (!hasCollection(getName())) {
                loadData = true;
            }
            break;
        default:
            break;
        }
        if (dropCollection) {
            collection.drop();
        }
        if (isMultiTenant()) {
            collection.createIndex(Indexes.hashed(TENANT_ID_FIELD));
        }
        if (loadData) {
            loadData();
        }
    }

    @Override
    public void initializeReferences() {
        try (MongoDBSession session = getSession()) {
            for (Reference reference : getReferences()) {
                if (reference instanceof MongoDBReference) {
                    ((MongoDBReference) reference).initialize(session);
                }
            }
        }
    }

    /**
     * Checks if the MongoDB server has the collection.
     *
     * @param collection the collection name
     * @return true if the server has the collection, false otherwise
     */
    protected boolean hasCollection(String collection) {
        return MongoDBConnectionHelper.hasCollection(database, collection);
    }

    /**
     * Retrieves the collection associated to this directory.
     *
     * @return the MongoDB collection
     */
    protected MongoCollection<Document> getCollection() {
        return collection;
    }

    /**
     * Retrieves the counters collection associated to this directory.
     *
     * @return the MongoDB counters collection
     */
    protected MongoCollection<Document> getCountersCollection() {
        return countersCollection;
    }

}
