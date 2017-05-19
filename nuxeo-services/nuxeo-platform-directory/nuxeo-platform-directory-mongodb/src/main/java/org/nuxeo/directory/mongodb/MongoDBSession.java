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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mongodb.client.model.Updates;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.EntrySource;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor.SubstringMatchType;

import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * MongoDB implementation of a {@link Session}
 *
 * @since 9.1
 */
public class MongoDBSession extends BaseSession implements EntrySource {

    private static final Log log = LogFactory.getLog(MongoDBSession.class);

    protected MongoClient client;

    protected String dbName;

    protected String schemaName;

    protected String directoryName;

    protected SubstringMatchType substringMatchType;

    protected String countersCollectionName;

    protected final Map<String, Field> schemaFieldMap;

    protected final String passwordHashAlgorithm;

    protected final boolean autoincrementId;

    public MongoDBSession(MongoDBDirectory directory) {
        super(directory);
        MongoDBDirectoryDescriptor desc = directory.getDescriptor();
        client = MongoDBConnectionHelper.newMongoClient(desc.getServerUrl());
        dbName = desc.getDatabaseName();
        directoryName = directory.getName();
        countersCollectionName = directory.getCountersCollectionName();
        schemaName = directory.getSchema();
        substringMatchType = desc.getSubstringMatchType();
        schemaFieldMap = directory.getSchemaFieldMap();
        autoincrementId = desc.isAutoincrementIdField();
        passwordHashAlgorithm = desc.passwordHashAlgorithm;
    }

    @Override
    public MongoDBDirectory getDirectory() {
        return (MongoDBDirectory) directory;
    }

    @Override
    public DocumentModel getEntry(String id) throws DirectoryException {
        return getEntry(id, true);
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return null;
        }
        return directory.getCache().getEntry(id, this, fetchReferences);
    }

    @Override
    public DocumentModelList getEntries() throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }
        return query(Collections.emptyMap());
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap) throws DirectoryException {
        checkPermission(SecurityConstants.WRITE);
        String id;
        if (autoincrementId) {
            Document filter = MongoDBSerializationHelper.fieldMapToBson(MONGODB_ID, directoryName);
            Bson update = Updates.inc(MONGODB_SEQ, 1L);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
            Long longId = getCollection(countersCollectionName).findOneAndUpdate(filter, update, options)
                                                               .getLong(MONGODB_SEQ);
            fieldMap.put(getIdField(), longId);
            id = String.valueOf(longId);
        } else {
            id = String.valueOf(fieldMap.get(getIdField()));
            if (hasEntry(id)) {
                throw new DirectoryException(String.format("Entry with id %s already exists", id));
            }
        }
        if (fieldMap.get(getPasswordField()) != null) {
            String password = (String) fieldMap.get(getPasswordField());
            password = PasswordHelper.hashPassword(password, passwordHashAlgorithm);
            fieldMap.put(getPasswordField(), password);
        }
        try {
            Document bson = MongoDBSerializationHelper.fieldMapToBson(fieldMap);
            getCollection().insertOne(bson);

            DocumentModel docModel = BaseSession.createEntryModel(null, schemaName, id, fieldMap, isReadOnly());

            // Add references fields
            Field schemaIdField = schemaFieldMap.get(getIdField());
            String idFieldName = schemaIdField.getName().getPrefixedName();

            String sourceId = docModel.getId();
            for (Reference reference : getDirectory().getReferences()) {
                String referenceFieldName = schemaFieldMap.get(reference.getFieldName()).getName().getPrefixedName();
                if (getDirectory().getReferences(reference.getFieldName()).size() > 1) {
                    log.warn("Directory " + getDirectory().getName() + " cannot create field "
                            + reference.getFieldName() + " for entry " + fieldMap.get(idFieldName)
                            + ": this field is associated with more than one reference");
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<String> targetIds = (List<String>) fieldMap.get(referenceFieldName);
                if (reference instanceof MongoDBReference) {
                    MongoDBReference mongodbReference = (MongoDBReference) reference;
                    mongodbReference.addLinks(sourceId, targetIds, this);
                } else {
                    reference.addLinks(sourceId, targetIds);
                }
            }

            getDirectory().invalidateCaches();
            return docModel;
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws DirectoryException {
        checkPermission(SecurityConstants.WRITE);
        Map<String, Object> fieldMap = new HashMap<>();
        List<String> referenceFieldList = new LinkedList<>();

        for (String fieldName : schemaFieldMap.keySet()) {
            Property prop = docModel.getPropertyObject(schemaName, fieldName);
            if (fieldName.equals(getPasswordField()) && StringUtils.isEmpty((String) prop.getValue())) {
                continue;
            }
            if (prop != null && prop.isDirty()) {
                Serializable value = prop.getValue();
                if (fieldName.equals(getPasswordField())) {
                    value = PasswordHelper.hashPassword((String) value, passwordHashAlgorithm);
                }
                fieldMap.put(prop.getName(), value);
            }
            if (getDirectory().isReference(fieldName)) {
                referenceFieldList.add(fieldName);
            }
        }

        String id = docModel.getId();
        Document bson = MongoDBSerializationHelper.fieldMapToBson(getIdField(), id);

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

        // update reference fields
        for (String referenceFieldName : referenceFieldList) {
            List<Reference> references = directory.getReferences(referenceFieldName);
            if (references.size() > 1) {
                // not supported
                log.warn("Directory " + getDirectory().getName() + " cannot update field " + referenceFieldName
                        + " for entry " + docModel.getId() + ": this field is associated with more than one reference");
            } else {
                Reference reference = references.get(0);
                @SuppressWarnings("unchecked")
                List<String> targetIds = (List<String>) docModel.getProperty(schemaName, referenceFieldName);
                if (reference instanceof MongoDBReference) {
                    MongoDBReference mongoReference = (MongoDBReference) reference;
                    mongoReference.setTargetIdsForSource(docModel.getId(), targetIds, this);
                } else {
                    reference.setTargetIdsForSource(docModel.getId(), targetIds);
                }
            }
        }
        getDirectory().invalidateCaches();

    }

    @Override
    public void deleteEntry(DocumentModel docModel) throws DirectoryException {
        deleteEntry(docModel.getId());
    }

    @Override
    public void deleteEntry(String id) throws DirectoryException {
        checkPermission(SecurityConstants.WRITE);
        checkDeleteConstraints(id);

        for (Reference reference : getDirectory().getReferences()) {
            if (reference instanceof MongoDBReference) {
                MongoDBReference mongoDBReference = (MongoDBReference) reference;
                mongoDBReference.removeLinksForSource(id, this);
            } else {
                reference.removeLinksForSource(id);
            }
        }

        try {
            DeleteResult result = getCollection().deleteOne(
                    MongoDBSerializationHelper.fieldMapToBson(getIdField(), id));
            if (!result.wasAcknowledged()) {
                throw new DirectoryException(
                        "Error while deleting the entry, the request has not been acknowledged by the server");
            }
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
        getDirectory().invalidateCaches();
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map) throws DirectoryException {
        // TODO deprecate this as it's unused
        deleteEntry(id);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter) throws DirectoryException {
        return query(filter, Collections.emptySet());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext) throws DirectoryException {
        return query(filter, fulltext, new HashMap<>());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException {
        return query(filter, fulltext, orderBy, false);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws DirectoryException {
        return query(filter, fulltext, orderBy, fetchReferences, -1, 0);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException {

        Document bson = buildQuery(filter, fulltext);

        DocumentModelList entries = new DocumentModelListImpl();

        FindIterable<Document> results = getCollection().find(bson).skip(offset);
        if (limit > 0) {
            results.limit(limit);
        }
        for (Document resultDoc : results) {

            // Cast object to document model
            Map<String, Object> fieldMap = MongoDBSerializationHelper.bsonToFieldMap(resultDoc);
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
            Object value = MongoDBSerializationHelper.valueToBson(entry.getValue());
            if (value != null) {
                String key = entry.getKey();
                if (fulltext.contains(key)) {
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
        }
        return bson;
    }

    protected void addField(Document bson, String key, Object value) {
        bson.put(key, value);
    }

    @Override
    public void close() throws DirectoryException {
        client.close();
        getDirectory().removeSession(this);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, String columnName) throws DirectoryException {
        return getProjection(filter, Collections.emptySet(), columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName)
            throws DirectoryException {
        DocumentModelList docList = query(filter, fulltext);
        List<String> result = new ArrayList<>();
        for (DocumentModel docModel : docList) {
            Object obj = docModel.getProperty(schemaName, columnName);
            String propValue = String.valueOf(obj);
            result.add(propValue);
        }
        return result;
    }

    @Override
    public boolean authenticate(String username, String password) throws DirectoryException {
        Document user = getCollection().find(MongoDBSerializationHelper.fieldMapToBson(getIdField(), username)).first();
        String storedPassword = user.getString(getPasswordField());
        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean hasEntry(String id) {
        return getCollection().count(MongoDBSerializationHelper.fieldMapToBson(getIdField(), id)) > 0;
    }

    @Override
    public DocumentModel createEntry(DocumentModel documentModel) {
        return createEntry(documentModel.getProperties(schemaName));
    }

    @Override
    public DocumentModel getEntryFromSource(String id, boolean fetchReferences) throws DirectoryException {
        DocumentModelList result = query(Collections.singletonMap(getIdField(), id), Collections.emptySet(),
                Collections.emptyMap(), fetchReferences, 1, 0);
        return result.isEmpty() ? null : result.get(0);
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
        String id = String.valueOf(fieldMap.get(getIdField()));
        DocumentModel docModel = BaseSession.createEntryModel(null, schemaName, id, fieldMap, isReadOnly());
        return docModel;
    }

}
