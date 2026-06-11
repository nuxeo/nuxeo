/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.mongodb;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_PATH;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.audit.service.AuditBackendFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.client.model.Indexes;

/**
 * @since 2025.0
 */
public class MongoDBAuditBackendFactory extends DefaultComponent implements AuditBackendFactory<MongoDBAuditBackend> {

    protected static final String XP_BACKEND = "backend";

    protected final Map<String, MongoDBAuditBackend> backends = new HashMap<>();

    @Override
    public void start(ComponentContext context) {
        this.<MongoDBAuditBackendDescriptor> getDescriptors(XP_BACKEND)
            .stream()
            .filter(MongoDBAuditBackendDescriptor::iEnabled)
            .map(this::instantiateBackend)
            .forEach(b -> backends.put(b.name(), b.backend));
    }

    protected MongoDBAuditBackendWithName instantiateBackend(MongoDBAuditBackendDescriptor descriptor) {
        // Get a connection to MongoDB
        var mongoService = Framework.getService(MongoDBConnectionService.class);
        var database = mongoService.getDatabase(descriptor.getConnectionId());
        var collectionName = descriptor.getCollectionName();
        MongoDBConnectionHelper.ensureCollectionExists(database, collectionName);
        var collection = database.getCollection(collectionName);
        collection.createIndex(Indexes.ascending(LOG_DOC_UUID)); // query by doc id
        collection.createIndex(Indexes.ascending(LOG_EVENT_DATE)); // query by date range
        collection.createIndex(Indexes.ascending(LOG_EVENT_ID)); // query by type of event
        collection.createIndex(Indexes.ascending(LOG_DOC_PATH)); // query by path
        collection.createIndex(Indexes.descending(LOG_ID)); // query by log id - sort
        collection.createIndex(Indexes.compoundIndex( //
                Indexes.ascending(LOG_REPOSITORY_ID), Indexes.descending(LOG_EVENT_DATE))); // query by drive - sort
        return new MongoDBAuditBackendWithName(descriptor.getName(), new MongoDBAuditBackend(collection));
    }

    @Override
    public void stop(ComponentContext context) {
        backends.clear();
    }

    @Override
    public MongoDBAuditBackend getAuditBackend(String name) {
        var backend = backends.get(name);
        if (backend == null) {
            throw new IllegalStateException("The audit backend with name: " + name + " does not exist");
        }
        return backend;
    }

    protected record MongoDBAuditBackendWithName(String name, MongoDBAuditBackend backend) {
    }
}
