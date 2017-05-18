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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor.SubstringMatchType;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;

import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * MongoDB implementation of a {@link Session}
 *
 * @since 9.1
 */
public class MongoDBSession extends BaseSession {

    protected MongoClient client;

    protected String dbName;

    protected String countersCollectionName;

    public MongoDBSession(MongoDBDirectory directory) {
        super(directory, MongoDBReference.class);
        MongoDBDirectoryDescriptor desc = directory.getDescriptor();
        client = MongoDBConnectionHelper.newMongoClient(desc.getServerUrl());
        dbName = desc.getDatabaseName();
        countersCollectionName = directory.getCountersCollectionName();
    }

    @Override
    public MongoDBDirectory getDirectory() {
        return (MongoDBDirectory) directory;
    }

    @Override
    protected DocumentModel createEntryWithoutReferences(Map<String, Object> fieldMap) {
        // Filter out reference fields for creation as we keep it in a different collection
        Map<String, Object> newDocMap = fieldMap.entrySet()
                                                .stream()
                                                .filter(entry -> getDirectory().getReferences(entry.getKey()) == null)
                                                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()),
                                                        HashMap::putAll);

        String idFieldName = schemaFieldMap.get(getIdField()).getName().getPrefixedName();
        String id;
        if (autoincrementId) {
            Document filter = MongoDBSerializationHelper.fieldMapToBson(MONGODB_ID, directoryName);
            Bson update = Updates.inc(MONGODB_SEQ, 1L);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
            Long longId = getCollection(countersCollectionName).findOneAndUpdate(filter, update, options)
                                                               .getLong(MONGODB_SEQ);
            fieldMap.put(idFieldName, longId);
            newDocMap.put(idFieldName, longId);
            id = String.valueOf(longId);
        } else {
            id = String.valueOf(fieldMap.get(idFieldName));
            if (hasEntry(id)) {
                throw new DirectoryException(String.format("Entry with id %s already exists", id));
            }
        }
        try {
            Document bson = MongoDBSerializationHelper.fieldMapToBson(newDocMap);
            String password = (String) newDocMap.get(getPasswordField());
            if (password != null && !PasswordHelper.isHashed(password)) {
                password = PasswordHelper.hashPassword(password, passwordHashAlgorithm);
                bson.append(getPasswordField(), password);
            }
            getCollection().insertOne(bson);
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
        return createEntryModel(null, schemaName, id, fieldMap, isReadOnly());
    }

    @Override
    protected List<String> updateEntryWithoutReferences(DocumentModel docModel) throws DirectoryException {
        Map<String, Object> fieldMap = new HashMap<>();
        List<String> referenceFieldList = new LinkedList<>();

        for (String fieldName : schemaFieldMap.keySet()) {
            if (fieldName.equals(getIdField())) {
                continue;
            }
            Property prop = docModel.getPropertyObject(schemaName, fieldName);
            if (fieldName.equals(getPasswordField()) && StringUtils.isEmpty((String) prop.getValue())) {
                continue;
            }
            if (prop != null && prop.isDirty()) {
                Serializable value = prop.getValue();
                if (fieldName.equals(getPasswordField())) {
                    value = PasswordHelper.hashPassword((String) value, passwordHashAlgorithm);
                }
                if (value instanceof Calendar) {
                    value = ((Calendar) value).getTime();
                }
                fieldMap.put(prop.getName(), value);
            }
            if (getDirectory().isReference(fieldName)) {
                referenceFieldList.add(fieldName);
            }
        }

        String idFieldName = schemaFieldMap.get(getIdField()).getName().getPrefixedName();
        String id = docModel.getId();
        Document bson = MongoDBSerializationHelper.fieldMapToBson(idFieldName, autoincrementId ? Long.valueOf(id) : id);

        List<Bson> updates = fieldMap.entrySet().stream().map(e -> Updates.set(e.getKey(), e.getValue())).collect(
                Collectors.toList());

        try {
            UpdateResult result = getCollection().updateOne(bson, Updates.combine(updates));
            // Throw an error if no document matched the update
            if (!result.wasAcknowledged()) {
                throw new DirectoryException(
                        "Error while updating the entry, the request has not been acknowledged by the server");
            }
            if (result.getMatchedCount() == 0) {
                throw new DirectoryException(
                        String.format("Error while updating the entry, no document was found with the id %s", id));
            }
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
        return referenceFieldList;
    }

    @Override
    public void deleteEntryWithoutReferences(String id) throws DirectoryException {
        try {
            String idFieldName = schemaFieldMap.get(getIdField()).getName().getPrefixedName();
            DeleteResult result = getCollection().deleteOne(
                    MongoDBSerializationHelper.fieldMapToBson(idFieldName, autoincrementId ? Long.valueOf(id) : id));
            if (!result.wasAcknowledged()) {
                throw new DirectoryException(
                        "Error while deleting the entry, the request has not been acknowledged by the server");
            }
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws DirectoryException {
        return query(filter, fulltext, orderBy, fetchReferences, -1, 0);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException {

        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }

        // Remove password as it is not possible to do queries with it
        filter.remove(getPasswordField());
        Document bson = buildQuery(filter, fulltext);

        DocumentModelList entries = new DocumentModelListImpl();

        FindIterable<Document> results = getCollection().find(bson).skip(offset);
        if (limit > 0) {
            results.limit(limit);
        }
        for (Document resultDoc : results) {

            // Cast object to document model
            Map<String, Object> fieldMap = MongoDBSerializationHelper.bsonToFieldMap(resultDoc);
            // Remove password from results
            if (!readAllColumns) {
                fieldMap.remove(getPasswordField());
            }
            DocumentModel doc = fieldMapToDocumentModel(fieldMap);

            if (fetchReferences) {
                Map<String, List<String>> targetIdsMap = new HashMap<>();
                for (Reference reference : directory.getReferences()) {
                    List<String> targetIds;
                    if (reference instanceof MongoDBReference) {
                        MongoDBReference mongoReference = (MongoDBReference) reference;
                        targetIds = mongoReference.getTargetIdsForSource(doc.getId(), this);
                    } else {
                        targetIds = reference.getTargetIdsForSource(doc.getId());
                    }
                    targetIds = new ArrayList<>(targetIds);
                    Collections.sort(targetIds);
                    String fieldName = reference.getFieldName();
                    targetIdsMap.computeIfAbsent(fieldName, key -> new ArrayList<>()).addAll(targetIds);
                }
                for (Map.Entry<String, List<String>> entry : targetIdsMap.entrySet()) {
                    String fieldName = entry.getKey();
                    List<String> targetIds = entry.getValue();
                    try {
                        doc.setProperty(schemaName, fieldName, targetIds);
                    } catch (PropertyException e) {
                        throw new DirectoryException(e);
                    }
                }
            }
            entries.add(doc);
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            getDirectory().orderEntries(entries, orderBy);
        }

        return entries;
    }

    protected Document buildQuery(Map<String, Serializable> fieldMap, Set<String> fulltext) {

        Document bson = new Document();
        for (Map.Entry<String, Serializable> entry : fieldMap.entrySet()) {
            Field field = schemaFieldMap.entrySet()
                                        .stream()
                                        .filter(e -> e.getValue().getName().getPrefixedName().equals(entry.getKey()))
                                        .map(Map.Entry::getValue)
                                        .findFirst()
                                        .orElse(null);
            Object value = null;
            if (field != null) {
                Type type = field.getType();
                if (entry.getValue() instanceof String) {
                    String originalValue = (String) entry.getValue();
                    if (type instanceof IntegerType) {
                        value = Integer.valueOf(originalValue);
                    } else if (type instanceof LongType) {
                        value = Long.valueOf(originalValue);
                    } else {
                        value = MongoDBSerializationHelper.valueToBson(entry.getValue());
                    }
                }
            } else {
                value = MongoDBSerializationHelper.valueToBson(entry.getValue());
            }
            String key = entry.getKey();
            if (fulltext != null && fulltext.contains(key)) {
                String val = String.valueOf(value);
                switch (substringMatchType) {
                case subany:
                    addField(bson, key, Pattern.compile(val, Pattern.CASE_INSENSITIVE));
                    break;
                case subinitial:
                    addField(bson, key, Pattern.compile('^' + val, Pattern.CASE_INSENSITIVE));
                    break;
                case subfinal:
                    addField(bson, key, Pattern.compile(val + '$', Pattern.CASE_INSENSITIVE));
                    break;
                }
            } else {
                addField(bson, key, value);
            }
        }
        return bson;
    }

    protected void addField(Document bson, String key, Object value) {
        String keyFieldName = key;
        Field field = schemaFieldMap.get(key);
        if (field != null) {
            keyFieldName = field.getName().getPrefixedName();
        }
        bson.put(keyFieldName, value);
    }

    @Override
    public void close() throws DirectoryException {
        client.close();
        getDirectory().removeSession(this);
    }

    @Override
    public boolean authenticate(String username, String password) throws DirectoryException {
        Document user = getCollection().find(MongoDBSerializationHelper.fieldMapToBson(getIdField(), username)).first();
        if (user == null) {
            return false;
        }
        String storedPassword = user.getString(getPasswordField());
        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean isAuthenticating() {
        return schemaFieldMap.containsKey(getPasswordField());
    }

    @Override
    public boolean hasEntry(String id) {
        return getCollection().count(MongoDBSerializationHelper.fieldMapToBson(getIdField(), id)) > 0;
    }

    /**
     * Retrieve a collection
     *
     * @param collection the collection name
     * @return the MongoDB collection
     */
    public MongoCollection<Document> getCollection(String collection) {
        return MongoDBConnectionHelper.getCollection(client, dbName, collection);
    }

    /**
     * Retrieve the collection associated to this directory
     *
     * @return the MongoDB collection
     */
    public MongoCollection<Document> getCollection() {
        return getCollection(directoryName);
    }

    /**
     * Check if the MongoDB server has the collection
     *
     * @param collection the collection name
     * @return true if the server has the collection, false otherwise
     */
    public boolean hasCollection(String collection) {
        return MongoDBConnectionHelper.hasCollection(client, dbName, collection);
    }

    protected DocumentModel fieldMapToDocumentModel(Map<String, Object> fieldMap) {
        String idFieldName = schemaFieldMap.get(getIdField()).getName().getPrefixedName();
        if (!fieldMap.containsKey(idFieldName)) {
            idFieldName = getIdField();
        }
        String id = String.valueOf(fieldMap.get(idFieldName));
        return createEntryModel(null, schemaName, id, fieldMap, isReadOnly());
    }

}
