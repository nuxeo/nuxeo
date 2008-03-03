/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id: MemoryDirectorySession.java 30374 2008-02-20 16:31:28Z gracinet $
 */

package org.nuxeo.ecm.directory.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;

/**
 * Trivial in-memory implementation of a Directory to use in unit tests.
 *
 * @author Florent Guillaume
 *
 */
public class MemoryDirectorySession implements Session {

    protected final MemoryDirectory directory;

    protected final Map<String, Map<String, Object>> data;

    public MemoryDirectorySession(MemoryDirectory directory) {
        this.directory = directory;
        data = new LinkedHashMap<String, Map<String, Object>>();
    }

    public boolean authenticate(String username, String password)
            throws DirectoryException {
        Map<String, Object> map = data.get(username);
        if (map == null) {
            return false;
        }
        String expected = (String) map.get(directory.passwordField);
        if (expected == null) {
            return false;
        }
        return expected.equals(password);
    }

    public void close() {
    }

    public void commit() {
    }

    public void rollback() throws DirectoryException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public DocumentModel createEntry(Map<String, Object> fieldMap)
            throws DirectoryException {
        // find id
        Object rawId = fieldMap.get(directory.idField);
        if (rawId == null) {
            throw new DirectoryException("Missing id");
        }
        String id = String.valueOf(rawId);
        Map<String, Object> map = data.get(id);
        if (map != null) {
            throw new DirectoryException(String.format(
                    "Entry with id %s already exists", id));
        }
        map = new HashMap<String, Object>();
        data.put(id, map);
        // put fields in map
        for (Map.Entry<String, Object> e : fieldMap.entrySet()) {
            String fieldName = e.getKey();
            if (!directory.schemaSet.contains(fieldName)) {
                continue;
            }
            map.put(fieldName, e.getValue());
        }
        return getEntry(id);
    }

    public DocumentModel getEntry(String id) throws DirectoryException {
        Map<String, Object> map = data.get(id);
        if (map == null) {
            return null;
        }
        DataModel dataModel = new DataModelImpl(directory.schemaName, map);
        // FIXME AT: document model is built using the schema name instead of
        // the type name ; plus in default app, "User" is the type name and
        // "user" is the schema name.
        DocumentModelImpl entry = new DocumentModelImpl(null,
                directory.schemaName, id, null, null, null,
                new String[]{ directory.schemaName }, null);
        entry.addDataModel(dataModel);
        return entry;
    }

    public void updateEntry(DocumentModel docModel) throws DirectoryException {
        String id = docModel.getId();
        DataModel dataModel = docModel.getDataModel(directory.schemaName);

        Map<String, Object> map = data.get(id);
        if (map == null) {
            // silently ignore attempts to update nonexisting entries
            return;
        }

        for (String fieldName : directory.schemaSet) {
            if (!dataModel.isDirty(fieldName)
                    || fieldName.equals(directory.idField)) {
                continue;
            }
            // TODO references
            map.put(fieldName, dataModel.getData(fieldName));
        }
        dataModel.getDirtyFields().clear();
    }

    public DocumentModelList getEntries() throws DirectoryException {
        DocumentModelList list = new DocumentModelListImpl();
        for (String id : data.keySet()) {
            list.add(getEntry(id));
        }
        return list;
    }

    public void deleteEntry(String id) throws DirectoryException {
        data.remove(id);
    }

    // given our storage model this doesn't even make sense, as id field is
    // unique
    public void deleteEntry(String id, Map<String, String> map)
            throws DirectoryException {
        throw new DirectoryException("Not implemented");
    }

    public void deleteEntry(DocumentModel docModel) throws DirectoryException {
        deleteEntry(docModel.getId());
    }

    public String getIdField() {
        return directory.idField;
    }

    public String getPasswordField() {
        return directory.passwordField;
    }

    public boolean isAuthenticating() {
        return directory.passwordField != null;
    }

    public boolean isReadOnly() {
        return false;
    }

    public DocumentModelList query(Map<String, Object> filter)
            throws DirectoryException {
        return query(filter, Collections.<String> emptySet());
    }

    public DocumentModelList query(Map<String, Object> filter,
            Set<String> fulltext) throws DirectoryException {
        return query(filter, fulltext, Collections.<String, String> emptyMap());
    }

    public DocumentModelList query(Map<String, Object> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException {
        DocumentModelList results = new DocumentModelListImpl();
        // canonicalize filter
        Map<String, Object> filt = new HashMap<String, Object>();
        for (Entry<String, Object> e : filter.entrySet()) {
            String fieldName = e.getKey();
            if (!directory.schemaSet.contains(fieldName)) {
                continue;
            }
            filt.put(fieldName, e.getValue());
        }
        // do the search
        data_loop: for (Entry<String, Map<String, Object>> datae : data.entrySet()) {
            String id = datae.getKey();
            Map<String, Object> map = datae.getValue();
            for (Entry<String, Object> e : filt.entrySet()) {
                String fieldName = e.getKey();
                Object expected = e.getValue();
                Object value = map.get(fieldName);
                if (value == null) {
                    if (expected != null) {
                        continue data_loop;
                    }
                } else {
                    if (fulltext.contains(fieldName)) {
                        if (!value.toString().toLowerCase().startsWith(
                                expected.toString().toLowerCase())) {
                            continue data_loop;
                        }
                    } else {
                        if (!value.equals(expected)) {
                            continue data_loop;
                        }
                    }
                }
            }
            // this entry matches
            results.add(getEntry(id));
        }
        // order entries
        if (!orderBy.isEmpty()) {
            directory.orderEntries(results, orderBy);
        }
        return results;
    }

    public List<String> getProjection(Map<String, Object> filter,
            String columnName) throws DirectoryException {
        return getProjection(filter, Collections.<String> emptySet(),
                columnName);
    }

    public List<String> getProjection(Map<String, Object> filter,
            Set<String> fulltext, String columnName) throws DirectoryException {
        DocumentModelList l = query(filter, fulltext);
        List<String> results = new ArrayList<String>(l.size());
        for (DocumentModel doc : l) {
            Object value = doc.getProperty(directory.schemaName, columnName);
            if (value != null) {
                results.add(value.toString());
            } else {
                results.add(null);
            }
        }
        return results;
    }
}
