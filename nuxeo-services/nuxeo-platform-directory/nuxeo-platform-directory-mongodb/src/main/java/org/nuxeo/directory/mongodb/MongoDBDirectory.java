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

import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_ALWAYS;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_ON_MISSING_COLUMNS;
import static org.nuxeo.directory.mongodb.MongoDBSerializationHelper.MONGODB_ID;
import static org.nuxeo.directory.mongodb.MongoDBSerializationHelper.MONGODB_SEQ;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryCSVLoader;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

/**
 * MongoDB implementation of a {@link Directory}
 * 
 * @since 9.1
 */
public class MongoDBDirectory extends AbstractDirectory {

    protected String countersCollectionName;

    protected boolean initialized;

    public MongoDBDirectory(MongoDBDirectoryDescriptor descriptor) {
        super(descriptor);

        // register the references to other directories
        addReferences(descriptor.getInverseReferences());
        addReferences(descriptor.getMongoDBReferences());

        // cache parameterization
        String cacheEntryName = descriptor.cacheEntryName;
        String cacheEntryNameWithoutReferencesName = descriptor.cacheEntryWithoutReferencesName;

        // cache fallback
        CacheService cacheService = Framework.getService(CacheService.class);
        if (cacheService != null) {
            if (cacheEntryName == null && descriptor.getCacheMaxSize() != 0) {
                cache.setEntryCacheName("cache-" + getName());
                cacheService.registerCache("cache-" + getName(), descriptor.getCacheMaxSize(),
                        descriptor.getCacheTimeout() / 60);
            }
            if (cacheEntryNameWithoutReferencesName == null && descriptor.getCacheMaxSize() != 0) {
                cache.setEntryCacheWithoutReferencesName("cacheWithoutReference-" + getName());
                cacheService.registerCache("cacheWithoutReference-" + getName(), descriptor.getCacheMaxSize(),
                        descriptor.getCacheTimeout() / 60);
            }
        }

        countersCollectionName = getName() + ".counters";

    }

    @Override
    public MongoDBDirectoryDescriptor getDescriptor() {
        return (MongoDBDirectoryDescriptor) descriptor;
    }

    @Override
    public Session getSession() throws DirectoryException {

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(getSchema());
        if (schema == null) {
            throw new DirectoryException(getSchema() + " is not a registered schema");
        }
        schemaFieldMap = new LinkedHashMap<>();
        schema.getFields().forEach(f -> schemaFieldMap.put(f.getName().getLocalName(), f));

        MongoDBSession session = new MongoDBSession(this);
        addSession(session);

        // Initialize counters collection if autoincrement enabled
        if (descriptor.isAutoincrementIdField() && !session.hasCollection(countersCollectionName)) {
            Map<String, Object> seq = new HashMap<>();
            seq.put(MONGODB_ID, getName());
            seq.put(MONGODB_SEQ, 0L);
            session.getCollection(countersCollectionName).insertOne(MongoDBSerializationHelper.fieldMapToBson(seq));
        }

        if (!initialized) {
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
                if (session.hasCollection(getName())) {
                    long totalEntries = collection.count();
                    boolean missingColumns = schema.getFields().stream().map(f -> f.getName().getLocalName()).anyMatch(
                            fname -> collection.count(Filters.exists(fname, false)) == totalEntries);
                    if (missingColumns) {
                        dropCollection = true;
                        loadData = true;
                    }
                } else {
                    loadData = true;
                }
                break;
            default:
                if (!session.hasCollection(getName())) {
                    loadData = true;
                }
                break;
            }
            if (dropCollection) {
                collection.drop();
            }
            if (loadData) {
                loadData(schema, session);
            }
            initialized = true;
        }
        return session;
    }

    protected void loadData(Schema schema, Session session) {
        if (descriptor.getDataFileName() != null) {
            DirectoryCSVLoader.loadData(descriptor.getDataFileName(), descriptor.getDataFileCharacterSeparator(),
                    schema, session::createEntry);
        }
    }

    public String getCountersCollectionName() {
        return countersCollectionName;
    }
}
