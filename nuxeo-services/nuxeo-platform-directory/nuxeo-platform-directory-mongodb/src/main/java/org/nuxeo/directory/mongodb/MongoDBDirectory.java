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

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryCSVLoader;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;

/**
 * MongoDB implementation of a {@link Directory}
 *
 * @since 9.1
 */
public class MongoDBDirectory extends AbstractDirectory {

    protected String countersCollectionName;

    // used in double-checked locking for lazy init
    protected volatile boolean initialized;

    public MongoDBDirectory(MongoDBDirectoryDescriptor descriptor) {
        super(descriptor, MongoDBReference.class);

        // Add specific references
        addMongoDBReferences(descriptor.getMongoDBReferences());

        // cache fallback
        fallbackOnDefaultCache();

        countersCollectionName = getName() + ".counters";
    }

    @Override
    public MongoDBDirectoryDescriptor getDescriptor() {
        return (MongoDBDirectoryDescriptor) descriptor;
    }

    @Override
    public Session getSession() throws DirectoryException {
        MongoDBSession session = new MongoDBSession(this);
        addSession(session);
        initializeIfNeeded(session);
        return session;
    }

    @Override
    public boolean isMultiTenant() {
        return schemaFieldMap.containsKey(TENANT_ID_FIELD);
    }

    protected void initializeIfNeeded(MongoDBSession session) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initialize(session);
                    initialized = true;
                }
            }
        }
    }

    protected void initialize(MongoDBSession session) {
        initSchemaFieldMap();

        // Initialize counters collection if autoincrement enabled
        if (descriptor.isAutoincrementIdField() && !session.hasCollection(countersCollectionName)) {
            Map<String, Object> seq = new HashMap<>();
            seq.put(MONGODB_ID, getName());
            seq.put(MONGODB_SEQ, 0L);
            session.getCollection(countersCollectionName).insertOne(MongoDBSerializationHelper.fieldMapToBson(seq));
        }

        String policy = descriptor.getCreateTablePolicy();
        MongoCollection collection = session.getCollection(getName());
        boolean dropCollection = false;
        boolean loadData = false;

        switch (policy) {
        case CREATE_TABLE_POLICY_ALWAYS:
            dropCollection = true;
            loadData = true;
            break;
        case CREATE_TABLE_POLICY_ON_MISSING_COLUMNS:
            // As MongoDB does not have the notion of columns, only load data if collection doesn't exist
            if (!session.hasCollection(getName())) {
                loadData = true;
            }
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
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            Schema schema = schemaManager.getSchema(getSchema());
            loadData(schema, session);
        }
    }

    protected void loadData(Schema schema, Session session) {
        if (descriptor.getDataFileName() != null) {
            Framework.doPrivileged(() -> DirectoryCSVLoader.loadData(descriptor.getDataFileName(),
                    descriptor.getDataFileCharacterSeparator(), schema, session::createEntry));
        }
    }

    protected void addMongoDBReferences(MongoDBReferenceDescriptor[] mongodbReferences) {
        if (mongodbReferences != null) {
            Arrays.stream(mongodbReferences).map(MongoDBReference::new).forEach(this::addReference);
        }
    }

    public String getCountersCollectionName() {
        return countersCollectionName;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

}
