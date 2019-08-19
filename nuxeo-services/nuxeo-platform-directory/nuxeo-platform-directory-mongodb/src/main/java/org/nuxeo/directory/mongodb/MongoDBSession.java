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
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.OperationNotAllowedException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p />
     * The connection id will be {@code directory/[DIRECTORY_NAME]}.
     */
    public static final String DIRECTORY_CONNECTION_PREFIX = "directory/";

    protected final MongoDatabase database;

    protected String countersCollectionName;

    public MongoDBSession(MongoDBDirectory directory) {
        super(directory, MongoDBReference.class);
        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        database = mongoService.getDatabase(DIRECTORY_CONNECTION_PREFIX + directory.getDescriptor().name);
        countersCollectionName = directory.getCountersCollectionName();
    }

    @Override
    public MongoDBDirectory getDirectory() {
        return (MongoDBDirectory) directory;
    }

    @Override
    public DocumentModel getEntryFromSource(String id, boolean fetchReferences) throws DirectoryException {
        String idFieldName = getPrefixedIdField();
        DocumentModelList result = doQuery(Collections.singletonMap(idFieldName, id), Collections.emptySet(),
                Collections.emptyMap(), true, 1, 0, false);

        if (result.isEmpty()) {
            return null;
        }

        DocumentModel docModel = result.get(0);

        if (isMultiTenant()) {
            // check that the entry is from the current tenant, or no tenant
            // at all
            if (!checkEntryTenantId((String) docModel.getProperty(schemaName, TENANT_ID_FIELD))) {
                return null;
            }
        }
        return docModel;
    }

    @Override
    protected DocumentModel createEntryWithoutReferences(Map<String, Object> fieldMap) {
        // Make a copy of fieldMap to avoid modifying it
        fieldMap = new HashMap<>(fieldMap);

        // Filter out reference fields for creation as we keep it in a different collection
        Map<String, Object> newDocMap = fieldMap.entrySet()
                                                .stream()
                                                .filter(entry -> getDirectory().getReferences(entry.getKey()) == null)
                                                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()),
                                                        HashMap::putAll);
        Map<String, Field> schemaFieldMap = directory.getSchemaFieldMap();
        String idFieldName = getPrefixedIdField();
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
            Object idFieldValue = fieldMap.get(idFieldName);
            id = String.valueOf(idFieldValue);
            if (hasEntry0(idFieldValue)) {
                throw new DirectoryException(String.format("Entry with id %s already exists", id));
            }
        }

        if (isMultiTenant()) {
            String tenantId = getCurrentTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                fieldMap.put(TENANT_ID_FIELD, tenantId);
                newDocMap.put(TENANT_ID_FIELD, tenantId);
                if (computeMultiTenantId) {
                    String tenantDirectoryId = computeMultiTenantDirectoryId(tenantId, id);
                    fieldMap.put(idFieldName, tenantDirectoryId);
                    newDocMap.put(idFieldName, tenantDirectoryId);
                }
            }
        }

        try {

            for (Map.Entry<String, Field> entry : schemaFieldMap.entrySet()) {
                Field field = entry.getValue();
                if (field != null) {
                    String fieldName = field.getName().getPrefixedName();
                    Type type = field.getType();
                    newDocMap.computeIfPresent(fieldName, (k, v) -> convertToType(v, type));
                    // Load default values if defined and not present in the map
                    Object defaultValue = field.getDefaultValue();
                    if (defaultValue != null) {
                        newDocMap.putIfAbsent(fieldName, defaultValue);
                    }
                }
            }
            Document bson = MongoDBSerializationHelper.fieldMapToBson(newDocMap);
            String password = (String) newDocMap.get(getPrefixedPasswordField());
            if (password != null && !PasswordHelper.isHashed(password)) {
                password = PasswordHelper.hashPassword(password, passwordHashAlgorithm);
                bson.append(getPrefixedPasswordField(), password);
            }
            getCollection().insertOne(bson);
        } catch (MongoWriteException e) {
            throw new DirectoryException(e);
        }
        return createEntryModel(null, schemaName, String.valueOf(fieldMap.get(idFieldName)), fieldMap, isReadOnly());
    }

    protected Object convertToType(Object value, Type type) {
        Object result = value;
        if (value instanceof String) {
            if (type instanceof IntegerType) {
                result = Integer.valueOf((String) value);
            } else if (type instanceof LongType) {
                result = Long.valueOf((String) value);
            }
        } else if (value instanceof Number) {
            if (type instanceof LongType && value instanceof Integer) {
                result = Long.valueOf((Integer) value);
            } else if (type instanceof StringType) {
                result = value.toString();
            }
        }
        return result;
    }

    @Override
    protected List<String> updateEntryWithoutReferences(DocumentModel docModel) throws DirectoryException {
        Map<String, Object> fieldMap = new HashMap<>();
        List<String> referenceFieldList = new LinkedList<>();

        if (isMultiTenant()) {
            // can only update entry from the current tenant
            String tenantId = getCurrentTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                String entryTenantId = (String) docModel.getProperty(schemaName, TENANT_ID_FIELD);
                if (StringUtils.isBlank(entryTenantId) || !entryTenantId.equals(tenantId)) {
                    throw new OperationNotAllowedException("Operation not allowed in the current tenant context",
                            "label.directory.error.multi.tenant.operationNotAllowed", null);
                }
            }
        }

        List<String> fields = directory.getSchemaFieldMap()
                                       .values()
                                       .stream()
                                       .map(field -> field.getName().getPrefixedName())
                                       .collect(Collectors.toList());
        String idFieldName = getPrefixedIdField();
        String passwordFieldName = getPrefixedPasswordField();

        for (String fieldName : fields) {
            if (fieldName.equals(idFieldName)) {
                continue;
            }
            Property prop = docModel.getPropertyObject(schemaName, fieldName);
            if (prop == null || !prop.isDirty()
                    || (fieldName.equals(passwordFieldName) && StringUtils.isEmpty((String) prop.getValue()))) {
                continue;
            }
            if (getDirectory().isReference(fieldName)) {
                referenceFieldList.add(fieldName);
            } else {
                Serializable value = prop.getValue();
                if (fieldName.equals(passwordFieldName)) {
                    value = PasswordHelper.hashPassword((String) value, passwordHashAlgorithm);
                }
                if (value instanceof Calendar) {
                    value = ((Calendar) value).getTime();
                }
                fieldMap.put(prop.getName(), value);
            }
        }

        String id = docModel.getId();
        Object idFieldValue = convertToType(id, getIdFieldType());
        Document bson = MongoDBSerializationHelper.fieldMapToBson(idFieldName, idFieldValue);

        List<Bson> updates = fieldMap.entrySet().stream().map(e -> Updates.set(e.getKey(), e.getValue())).collect(
                Collectors.toList());

        if (!updates.isEmpty()) {
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
        }
        return referenceFieldList;
    }

    @Override
    public void deleteEntryWithoutReferences(String id) throws DirectoryException {
        try {
            String idFieldName = getPrefixedIdField();
            Object idFieldValue = convertToType(id, getIdFieldType());
            DeleteResult result = getCollection().deleteOne(
                    MongoDBSerializationHelper.fieldMapToBson(idFieldName, idFieldValue));
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
            boolean fetchReferences, int limit, int offset) throws DirectoryException {
        return doQuery(filter, fulltext, orderBy, fetchReferences, limit, offset, true);
    }

    protected DocumentModelList doQuery(Map<String, Serializable> filter, Set<String> fulltext,
            Map<String, String> orderBy, boolean fetchReferences, int limit, int offset, boolean checkTenantId)
            throws DirectoryException {

        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }

        Map<String, Serializable> filterMap = new HashMap<>(filter);

        if (checkTenantId && isMultiTenant()) {
            // filter entries on the tenantId field also
            String tenantId = getCurrentTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                filterMap.put(TENANT_ID_FIELD, tenantId);
            }
        }

        // Remove password as it is not possible to do queries with it
        String passwordFieldName = getPrefixedPasswordField();
        filterMap.remove(passwordFieldName);
        Document bson = buildQuery(filterMap, fulltext);

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
                fieldMap.remove(passwordFieldName);
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
        Map<String, Field> schemaFieldMap = directory.getSchemaFieldMap();
        Document bson = new Document();
        for (Map.Entry<String, Serializable> entry : fieldMap.entrySet()) {
            Field field = schemaFieldMap.entrySet()
                                        .stream()
                                        .filter(e -> e.getValue().getName().getPrefixedName().equals(entry.getKey()))
                                        .map(Map.Entry::getValue)
                                        .findFirst()
                                        .orElse(null);

            Serializable v = entry.getValue();
            Object value = (field != null) ? MongoDBSerializationHelper.valueToBson(v, field.getType())
                    : MongoDBSerializationHelper.valueToBson(v);
            String key = entry.getKey();
            if (fulltext != null && fulltext.contains(key)) {
                String val = String.valueOf(value);
                if (val != null) {
                    val = val.replaceAll("%+", ".*");
                }
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
        Field field = directory.getSchemaFieldMap().get(key);
        if (field != null) {
            keyFieldName = field.getName().getPrefixedName();
        }
        bson.put(keyFieldName, value);
    }

    @Override
    public void close() throws DirectoryException {
        getDirectory().removeSession(this);
    }

    @Override
    public boolean authenticate(String username, String password) throws DirectoryException {
        Document user = getCollection().find(MongoDBSerializationHelper.fieldMapToBson(getPrefixedIdField(), username))
                                       .first();
        if (user == null) {
            return false;
        }

        String storedPassword = user.getString(getPrefixedPasswordField());
        if (isMultiTenant()) {
            // check that the entry is from the current tenant, or no tenant at all
            if(!checkEntryTenantId(user.getString(TENANT_ID_FIELD))) {
                storedPassword = null;
            }
        }

        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean isAuthenticating() {
        return directory.getSchemaFieldMap().containsKey(getPasswordField());
    }

    @Override
    public boolean hasEntry(String id) {
        return hasEntry0(id);
    }

    protected boolean hasEntry0(Object id) {
        String idFieldName = getPrefixedIdField();
        Type idFieldType = getIdFieldType();
        Object idFieldValue = convertToType(id, idFieldType);
        return getCollection().count(MongoDBSerializationHelper.fieldMapToBson(idFieldName, idFieldValue)) > 0;
    }

    /**
     * Retrieve a collection
     *
     * @param collection the collection name
     * @return the MongoDB collection
     */
    public MongoCollection<Document> getCollection(String collection) {
        return database.getCollection(collection);
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
        return MongoDBConnectionHelper.hasCollection(database, collection);
    }

    protected DocumentModel fieldMapToDocumentModel(Map<String, Object> fieldMap) {
        String idFieldName = getPrefixedIdField();
        if (!fieldMap.containsKey(idFieldName)) {
            idFieldName = getIdField();
        }
        String id = String.valueOf(fieldMap.get(idFieldName));
        return createEntryModel(null, schemaName, id, fieldMap, isReadOnly());
    }

    protected boolean checkEntryTenantId(String entryTenantId) {
        // check that the entry is from the current tenant, or no tenant at all
        String tenantId = getCurrentTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            if (StringUtils.isNotBlank(entryTenantId) && !entryTenantId.equals(tenantId)) {
                return false;
            }
        }
        return true;
    }

    protected String getPrefixedIdField() {
        Field idField = directory.getSchemaFieldMap().get(getIdField());
        if (idField == null) {
            return null;
        }
        return idField.getName().getPrefixedName();
    }

    protected String getPrefixedPasswordField() {
        Field passwordField = directory.getSchemaFieldMap().get(getPasswordField());
        if (passwordField == null) {
            return null;
        }
        return passwordField.getName().getPrefixedName();
    }

    protected Type getIdFieldType() {
        Field idField = directory.getSchemaFieldMap().get(getIdField());
        if (idField == null) {
            return null;
        }
        return idField.getType();
    }

}
