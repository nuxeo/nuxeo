/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Implementation of a {@link org.nuxeo.ecm.directory.Session Session} for a {@link CoreDirectory}.
 * <p>
 * Directory entries are stored as children documents of the directory folder, itself a child of a single directories
 * root folder.
 * <p>
 * The directory folder is a folderish document whose name is the directory name. Its document type is specified in the
 * directory configuration.
 * <p>
 * A directory entry is a document with the schema specified in the directory configuration. The entry id is stored in
 * the document name.
 * <p>
 * A core schema can be specified for storage instead of the directory schema; this is used for schemas having an "id"
 * field which is forbidden for core VCS storage.
 *
 * @since 8.2
 */
public class CoreDirectorySession extends BaseSession {

    protected String docType;

    protected String idFieldPrefixed;

    protected String passwordFieldPrefixed;

    protected String dirPath;

    public CoreDirectorySession(CoreDirectory directory) {
        super(directory, null);
        CoreDirectoryDescriptor descriptor = directory.getDescriptor();
        docType = descriptor.docType;
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (!schemaManager.getDocumentType(docType).hasSchema(schemaName)) {
            throw new DirectoryException("Unknown schema: " + schemaName + " in doctype: " + docType
                    + " for directory: " + descriptor.name);
        }
        Field field = schemaManager.getSchema(schemaName).getField(getIdField());
        if (field == null) {
            throw new DirectoryException("Unknown field: " + getIdField() + " in schema: " + schemaName
                    + " for directory: " + descriptor.name);
        }
        idFieldPrefixed = field.getName().getPrefixedName();
        if (isAuthenticating()) {
            field = schemaManager.getSchema(schemaName).getField(getPasswordField());
            if (field != null) {
                passwordFieldPrefixed = field.getName().getPrefixedName();
            }
        }
    }

    @Override
    public CoreDirectory getDirectory() {
        return (CoreDirectory) directory;
    }

    protected DocumentRef getDocumentRef(String id) {
        return new PathRef(getDirectory().directoryPath + '/' + idToName(id));
    }

    protected static final char ESC = '=';

    protected static final char SLASH = '/';

    /**
     * Escapes reserved characters using the form =xx where xx is an hex char name.
     * <p>
     * Slash cannot be used in document names, so it must be escaped.
     */
    protected static String idToName(String id) {
        if (id.indexOf(ESC) >= 0 || id.indexOf(SLASH) >= 0) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < id.length(); i++) {
                char c = id.charAt(i);
                if (c == ESC || c == SLASH) {
                    buf.append(ESC);
                    buf.append(Character.forDigit((c >> 4) & 0xF, 16));
                    buf.append(Character.forDigit(c & 0xF, 16));
                } else {
                    buf.append(c);
                }
            }
            id = buf.toString();
        }
        return id;
    }

    /**
     * Unescapes reserved characters using the form =xx where xx is an hex char name.
     */
    protected static String nameToId(String name) {
        if (name.indexOf(ESC) >= 0) {
            StringBuilder buf = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == ESC) {
                    if (i < name.length() - 2) {
                        try {
                            int cc = Integer.parseInt(name.substring(i + 1, i + 3), 16);
                            i += 2; // NOSONAR
                            buf.append((char) cc);
                        } catch (IllegalArgumentException e) {
                            throw new NuxeoException("Illegal name, bad encoding: " + name);
                        }
                    } else {
                        throw new NuxeoException("Illegal name, bad encoding: " + name);
                    }
                } else {
                    buf.append(c);
                }
            }
            name = buf.toString();
        }
        return name;
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences) {
        if (!hasPermission(SecurityConstants.READ)) {
            return null;
        }
        return TransactionHelper.runInTransaction(
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    DocumentRef ref = getDocumentRef(id);
                    if (!session.exists(ref)) {
                        return null;
                    }
                    DocumentModel doc = session.getDocument(ref);
                    return docToEntry(id, doc);
                }));
    }

    /** Maps a core document to a directory entry document. */
    protected DocumentModel docToEntry(String id, DocumentModel doc) {
        Map<String, Object> properties = doc.getProperties(schemaName);
        properties.put(getIdField(), id);
        // TODO : deal with references
        DocumentModel entry = createEntryModel(null, schemaName, id, properties);
        if (isReadOnly()) {
            setReadOnlyEntry(entry);
        }
        return entry;
    }

    @Override
    public DocumentModelList getEntries() throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl(0);
        }
        return TransactionHelper.runInTransaction(
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    DocumentModelList docs = session.getChildren(new PathRef(getDirectory().directoryPath));
                    DocumentModelList entries = new DocumentModelListImpl(docs.size());
                    for (DocumentModel doc : docs) {
                        entries.add(docToEntry(nameToId(doc.getName()), doc));
                    }
                    return entries;
                }));
    }

    @Override
    public DocumentModel createEntryWithoutReferences(Map<String, Object> map) {
        String id = (String) map.get(idFieldPrefixed);
        if (id == null) {
            throw new DirectoryException("Missing id field for entry: " + map);
        }

        // TODO : deal with encrypted password
        Map<String, Object> properties = new HashMap<>();
        List<String> createdRefs = new ArrayList<>();
        for (Entry<String, Object> es : map.entrySet()) {
            String fieldId = es.getKey();
            if (idFieldPrefixed.equals(fieldId)) {
                continue;
            }
            Object value = es.getValue();
            if (getDirectory().isReference(fieldId)) {
                createdRefs.add(fieldId);
            }
            // TODO reference fields
            properties.put(fieldId, value);
        }
        return TransactionHelper.runInTransaction(
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    DocumentRef ref = getDocumentRef(id);
                    if (session.exists(ref)) {
                        throw new DirectoryException(String.format("Entry with id %s already exists", id));
                    }
                    DocumentModel doc = session.createDocumentModel(getDirectory().directoryPath, idToName(id),
                            docType);
                    doc.setProperties(schemaName, properties);
                    doc = session.createDocument(doc);
                    session.save();
                    return docToEntry(id, doc);
                }));
    }

    @Override
    protected List<String> updateEntryWithoutReferences(DocumentModel docModel) {
        // TODO once references are implemented
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEntry(DocumentModel update) {
        checkPermission(SecurityConstants.WRITE);
        String id = (String) update.getPropertyValue(idFieldPrefixed);
        if (id == null) {
            throw new DirectoryException("Cannot update entry with null id: " + update.getProperties(schemaName));
        }
        TransactionHelper.runInTransaction( //
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    DocumentRef ref = getDocumentRef(id);
                    if (!session.exists(ref)) {
                        throw new DirectoryException("Missing entry with id: " + id);
                    }
                    DocumentModel doc = session.getDocument(ref);
                    List<String> updatedRefs = new ArrayList<String>();
                    for (Entry<String, Object> es : update.getProperties(schemaName).entrySet()) {
                        // TODO reference
                        String key = es.getKey();
                        if (idFieldPrefixed.equals(key)) {
                            continue;
                        }
                        if (getDirectory().isReference(key)) {
                            updatedRefs.add(key);
                        } else {
                            doc.setPropertyValue(key, (Serializable) es.getValue());
                        }
                    }
                    // TODO update reference fields
                    session.saveDocument(doc);
                    session.save();
                }));
    }

    @Override
    protected void deleteEntryWithoutReferences(String id) {
        // TODO once references are implemented
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEntry(DocumentModel doc) {
        String id = (String) doc.getPropertyValue(idFieldPrefixed);
        deleteEntry(id);
    }

    @Override
    public void deleteEntry(String id) {
        checkPermission(SecurityConstants.WRITE);
        checkDeleteConstraints(id);
        if (id == null) {
            throw new DirectoryException("Cannot delete entry with null id");
        }
        TransactionHelper.runInTransaction( //
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    DocumentRef ref = getDocumentRef(id);
                    if (!session.exists(ref)) {
                        return;
                    }
                    // TODO first remove references to this entry
                    session.removeDocument(ref);
                    session.save();
                }));
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }
        // TODO deal with fetch ref
        // TODO descriptor's queryLimitSize
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(docType);
        query.append(" WHERE ");
        addClauses(query, filter, fulltext);
        return TransactionHelper.runInTransaction(
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    DocumentModelList docs = session.query(query.toString(), null, limit, offset, false);
                    DocumentModelList entries = new DocumentModelListImpl(docs.size());
                    for (DocumentModel doc : docs) {
                        entries.add(docToEntry(nameToId(doc.getName()), doc));
                    }
                    return entries;
                }));
    }

    @Override
    public DocumentModelList query(QueryBuilder queryBuilder, boolean fetchReferences) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> queryIds(QueryBuilder queryBuilder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        getDirectory().removeSession(this);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, String columnName) {
        return getProjection(filter, Collections.emptySet(), columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName) {
        if (!hasPermission(SecurityConstants.READ)) {
            return Collections.emptyList();
        }
        filter = new LinkedHashMap<>(filter);
        filter.remove(passwordFieldPrefixed); // cannot filter on password

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(schemaName);
        String nxqlCol = nxqlColumn(schema.getField(columnName).getName().getPrefixedName());

        // TODO descriptor's queryLimitSize
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append(nxqlCol);
        query.append(" FROM ");
        query.append(docType);
        query.append(" WHERE ");
        addClauses(query, filter, fulltext);
        return TransactionHelper.runInTransaction(
                () -> CoreInstance.doPrivileged(getDirectory().repositoryName, session -> {
                    List<String> results = new ArrayList<>();
                    try (IterableQueryResult it = session.queryAndFetch(query.toString(), NXQL.NXQL)) {
                        for (Map<String, Serializable> map : it) {
                            results.add((String) map.get(nxqlCol));
                        }
                    }
                    return results;
                }));
    }

    /** Finds the NXQL column name to use for the given property. */
    protected String nxqlColumn(String prop) {
        if (idFieldPrefixed.equals(prop)) {
            return NXQL.ECM_NAME;
        } else if (prop.contains(":")) {
            return prop;
        } else {
            // for NXQL we need a fully-qualified column name as there may be ambiguities in schemas that don't have a
            // prefix (ex: vocabulary:label vs xvocabulary:label).
            return schemaName + ":" + prop;
        }
    }

    protected void addClauses(StringBuilder sb, Map<String, Serializable> filter, Set<String> fulltext) {
        List<String> clauses = new ArrayList<>();
        clauses.add("ecm:parentId = '" + getDirectory().directoryFolderId + "'");
        clauses.add("ecm:isProxy = 0");
        clauses.add("ecm:isVersion = 0");
        clauses.add("ecm:isTrashed = 0");

        // TODO deal with fetch ref

        for (Entry<String, Serializable> es : filter.entrySet()) {
            String key = es.getKey();
            if (fulltext.contains(key)) {
                continue;
            }
            String clause = nxqlColumn(key) + " = '" + NXQL.escapeStringInner((String) es.getValue()) + "'";
            clauses.add(clause);
        }
        if (!fulltext.isEmpty()) {
            StringBuilder ft = new StringBuilder();
            for (String key : fulltext) {
                ft.append(filter.get(key));
                ft.append(" ");
            }
            if (ft.length() > 0) {
                ft.setLength(ft.length() - 1);
                String clause = NXQL.ECM_FULLTEXT + " = '" + NXQL.escapeStringInner(ft.toString()) + "'";
                clauses.add(clause);
            }
        }
        for (Iterator<String> it = clauses.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" AND ");
            }
        }
    }

    @Override
    public boolean authenticate(String username, String password) {
        DocumentModel entry = getEntry(username, false);
        if (entry == null) {
            return false;
        }
        String storedPassword = (String) entry.getProperty(schemaName, directory.getPasswordField());
        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean hasEntry(String id) {
        // TODO optimize
        return getEntry(id, false) != null;
    }

}
