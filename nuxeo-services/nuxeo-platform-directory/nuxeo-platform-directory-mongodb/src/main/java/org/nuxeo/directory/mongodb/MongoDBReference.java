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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.directory.AbstractReference;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.DirectoryCSVLoader;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import org.nuxeo.ecm.directory.ReferenceDescriptor;
import org.nuxeo.ecm.directory.Session;

/**
 * MongoDB implementation of a {@link Reference}
 *
 * @since 9.1
 */
public class MongoDBReference extends AbstractReference {

    protected String collection;

    protected String sourceField;

    protected String targetField;

    protected String dataFileName;

    private boolean initialized;

    /**
     * @since 9.2
     */
    public MongoDBReference(String field, String directory, String collection, String sourceField, String targetField,
            String dataFileName) {
        super(field, directory);
        this.collection = collection;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.dataFileName = dataFileName;
    }

    /**
     * @since 9.2
     */
    public MongoDBReference(MongoDBReferenceDescriptor descriptor) {
        this(descriptor.getFieldName(), descriptor.getTargetDirectoryName(), descriptor.getCollection(),
                descriptor.getSourceField(), descriptor.getTargetField(), descriptor.getDataFileName());
    }

    /**
     * @since 9.2
     */
    public MongoDBReference(ReferenceDescriptor descriptor) {
        this(descriptor.getFieldName(), descriptor.getDirectory(), descriptor.getReferenceName(),
                descriptor.getSource(), descriptor.getTarget(), descriptor.getDataFileName());
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds) {
        try (MongoDBSession session = getMongoDBSession()) {
            addLinks(sourceId, targetIds, session);
        }
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds, Session session) {
        MongoDBSession mongoSession = (MongoDBSession) session;
        if (!initialized) {
            if (dataFileName != null) {
                initializeSession(mongoSession);
            }
            initialized = true;
        }
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        try {
            MongoCollection<Document> coll = mongoSession.getCollection(collection);
            List<Document> newDocs = targetIds.stream()
                                              .map(targetId -> buildDoc(sourceId, targetId))
                                              .filter(doc -> coll.count(doc) == 0)
                                              .collect(Collectors.toList());
            if (!newDocs.isEmpty()) {
                coll.insertMany(newDocs);
            }
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId, Session session) {
        MongoDBSession mongodbSession = (MongoDBSession) session;
        MongoCollection<Document> coll = mongodbSession.getCollection(collection);
        List<Document> newDocs = sourceIds.stream()
                                          .map(sourceId -> buildDoc(sourceId, targetId))
                                          .filter(doc -> coll.count(doc) == 0)
                                          .collect(Collectors.toList());
        if (!newDocs.isEmpty()) {
            coll.insertMany(newDocs);
        }
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return;
        }
        try (MongoDBSession session = getMongoDBSession()) {
            addLinks(sourceIds, targetId, session);
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public void removeLinksForSource(String sourceId) {
        try (MongoDBSession session = getMongoDBSession()) {
            removeLinksForSource(sourceId, session);
        }
    }

    @Override
    public void removeLinksForSource(String sourceId, Session session) {
        removeLinksFor(sourceField, sourceId, (MongoDBSession) session);
    }

    @Override
    public void removeLinksForTarget(String targetId) {
        try (MongoDBSession session = getMongoDBSession()) {
            removeLinksFor(targetField, targetId, session);
        }
    }

    @Override
    public void removeLinksForTarget(String targetId, Session session) {
        removeLinksFor(targetField, targetId, (MongoDBSession) session);
    }

    private void removeLinksFor(String field, String value, MongoDBSession session) {
        try {
            DeleteResult result = session.getCollection(collection)
                                         .deleteMany(MongoDBSerializationHelper.fieldMapToBson(field, value));
            if (!result.wasAcknowledged()) {
                throw new DirectoryException(
                        "Error while deleting the entry, the request has not been acknowledged by the server");
            }
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public List<String> getTargetIdsForSource(String sourceId) {
        try (MongoDBSession session = getMongoDBSession()) {
            return getIdsFor(sourceField, sourceId, targetField, session);
        }
    }

    /**
     * Retrieves all target ids associated to the given source id
     *
     * @param sourceId the source id
     * @param session the mongoDB session
     * @return the list of target ids
     */
    public List<String> getTargetIdsForSource(String sourceId, MongoDBSession session) {
        return getIdsFor(sourceField, sourceId, targetField, session);
    }

    @Override
    public List<String> getSourceIdsForTarget(String targetId) {
        try (MongoDBSession session = getMongoDBSession()) {
            return getIdsFor(targetField, targetId, sourceField, session);
        }
    }

    private List<String> getIdsFor(String queryField, String value, String resultField, MongoDBSession session) {
        FindIterable<Document> docs = session.getCollection(collection)
                                             .find(MongoDBSerializationHelper.fieldMapToBson(queryField, value));
        return StreamSupport.stream(docs.spliterator(), false)
                            .map(doc -> doc.getString(resultField))
                            .collect(Collectors.toList());
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds) {
        try (MongoDBSession session = getMongoDBSession()) {
            setTargetIdsForSource(sourceId, targetIds, session);
        }
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds, Session session) {
        setIdsFor(sourceField, sourceId, targetField, targetIds, (MongoDBSession) session);
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds) {
        try (MongoDBSession session = getMongoDBSession()) {
            setIdsFor(targetField, targetId, sourceField, sourceIds, session);
        }
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds, Session session) {
        setIdsFor(targetField, targetId, sourceField, sourceIds, (MongoDBSession) session);
    }

    private void setIdsFor(String field, String value, String fieldToUpdate, List<String> ids, MongoDBSession session) {
        Set<String> idsToAdd = new HashSet<>();
        if (ids != null) {
            idsToAdd.addAll(ids);
        }
        List<String> idsToDelete = new ArrayList<>();

        List<String> existingIds = getIdsFor(field, value, fieldToUpdate, session);
        for (String id : existingIds) {
            if (!idsToAdd.remove(id)) {
                idsToDelete.add(id);
            }
        }

        if (!idsToDelete.isEmpty()) {
            BasicDBList list = new BasicDBList();
            if (sourceField.equals(field)) {
                list.addAll(idsToDelete.stream().map(id -> buildDoc(value, id)).collect(Collectors.toList()));
            } else {
                list.addAll(idsToDelete.stream().map(id -> buildDoc(id, value)).collect(Collectors.toList()));
            }
            Bson deleteDoc = new BasicDBObject("$or", list);
            session.getCollection(collection).deleteMany(deleteDoc);
        }

        if (!idsToAdd.isEmpty()) {
            List<Document> list;
            if (sourceField.equals(field)) {
                list = idsToAdd.stream().map(id -> buildDoc(value, id)).collect(Collectors.toList());
            } else {
                list = idsToAdd.stream().map(id -> buildDoc(id, value)).collect(Collectors.toList());
            }
            session.getCollection(collection).insertMany(list);
        }
    }

    private Document buildDoc(String sourceId, String targetId) {
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put(sourceField, sourceId);
        fieldMap.put(targetField, targetId);
        return MongoDBSerializationHelper.fieldMapToBson(fieldMap);
    }

    protected void initializeSession(MongoDBSession session) {
        // fake schema for DirectoryCSVLoader.loadData
        SchemaImpl schema = new SchemaImpl(collection, null);
        schema.addField(sourceField, StringType.INSTANCE, null, 0, Collections.emptySet());
        schema.addField(targetField, StringType.INSTANCE, null, 0, Collections.emptySet());

        Consumer<Map<String, Object>> loader = map -> {
            Document doc = MongoDBSerializationHelper.fieldMapToBson(map);
            MongoCollection<Document> coll = session.getCollection(collection);
            if (coll.count(doc) == 0) {
                coll.insertOne(doc);
            }
        };
        DirectoryCSVLoader.loadData(dataFileName, BaseDirectoryDescriptor.DEFAULT_DATA_FILE_CHARACTER_SEPARATOR, schema,
                loader);
    }

    protected MongoDBSession getMongoDBSession() {
        if (!initialized) {
            if (dataFileName != null) {
                try (MongoDBSession session = (MongoDBSession) getSourceDirectory().getSession()) {
                    initializeSession(session);
                }
            }
            initialized = true;
        }
        return (MongoDBSession) getSourceDirectory().getSession();
    }

}
