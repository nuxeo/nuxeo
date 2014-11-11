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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Trivial in-memory implementation of a Directory to use in unit tests.
 *
 * @author Florent Guillaume
 */
public class MemoryDirectorySession extends BaseSession {

    protected final MemoryDirectory directory;

    protected final Map<String, Map<String, Object>> data;

    public MemoryDirectorySession(MemoryDirectory directory) {
        super(directory);
        this.directory = directory;
        data = Collections.synchronizedMap(new LinkedHashMap<String, Map<String, Object>>());
    }

    @Override
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

    @Override
    public void close() {
    }

    public void commit() {
    }

    public void rollback() throws DirectoryException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap)
            throws DirectoryException {
        if (isReadOnly()) {
            return null;
        }
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
        for (Entry<String, Object> e : fieldMap.entrySet()) {
            String fieldName = e.getKey();
            if (!directory.schemaSet.contains(fieldName)) {
                continue;
            }
            map.put(fieldName, e.getValue());
        }
        return getEntry(id);
    }

    @Override
    public DocumentModel getEntry(String id) throws DirectoryException {
        return getEntry(id, true);
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences)
            throws DirectoryException {
        // XXX no references here
        Map<String, Object> map = data.get(id);
        if (map == null) {
            return null;
        }
        try {
            DocumentModel entry = BaseSession.createEntryModel(null,
                    directory.schemaName, id, map, isReadOnly());
            return entry;
        } catch (PropertyException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws DirectoryException {
        String id = docModel.getId();
        DataModel dataModel;
        try {
            dataModel = docModel.getDataModel(directory.schemaName);
        } catch (ClientException e) {
            throw new DirectoryException(e);
        }

        Map<String, Object> map = data.get(id);
        if (map == null) {
            throw new DirectoryException("UpdateEntry failed: entry '" + id
                    + "' not found");
        }

        for (String fieldName : directory.schemaSet) {
            try {
                if (!dataModel.isDirty(fieldName)
                        || fieldName.equals(directory.idField)) {
                    continue;
                }
            } catch (PropertyNotFoundException e) {
                continue;
            }
            // TODO references
            try {
                map.put(fieldName, dataModel.getData(fieldName));
            } catch (PropertyException e) {
                throw new ClientRuntimeException(e);
            }
        }
        dataModel.getDirtyFields().clear();
    }

    @Override
    public DocumentModelList getEntries() throws DirectoryException {
        DocumentModelList list = new DocumentModelListImpl();
        for (String id : data.keySet()) {
            list.add(getEntry(id));
        }
        return list;
    }

    @Override
    public void deleteEntry(String id) throws DirectoryException {
        data.remove(id);
    }

    // given our storage model this doesn't even make sense, as id field is
    // unique
    @Override
    public void deleteEntry(String id, Map<String, String> map)
            throws DirectoryException {
        throw new DirectoryException("Not implemented");
    }

    @Override
    public void deleteEntry(DocumentModel docModel) throws DirectoryException {
        deleteEntry(docModel.getId());
    }

    @Override
    public String getIdField() {
        return directory.idField;
    }

    @Override
    public String getPasswordField() {
        return directory.passwordField;
    }

    @Override
    public boolean isAuthenticating() {
        return directory.passwordField != null;
    }

    @Override
    public boolean isReadOnly() {
        return directory.isReadOnly();
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter)
            throws DirectoryException {
        return query(filter, Collections.<String> emptySet());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext) throws DirectoryException {
        return query(filter, fulltext, Collections.<String, String> emptyMap());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException {
        return query(filter, fulltext, orderBy, true);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws DirectoryException {
        DocumentModelList results = new DocumentModelListImpl();
        // canonicalize filter
        Map<String, Object> filt = new HashMap<String, Object>();
        for (Entry<String, Serializable> e : filter.entrySet()) {
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
                    if (fulltext != null && fulltext.contains(fieldName)) {
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
        if (orderBy != null && !orderBy.isEmpty()) {
            directory.orderEntries(results, orderBy);
        }
        return results;
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter,
            String columnName) throws DirectoryException {
        return getProjection(filter, Collections.<String> emptySet(),
                columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter,
            Set<String> fulltext, String columnName) throws DirectoryException {
        DocumentModelList l = query(filter, fulltext);
        List<String> results = new ArrayList<String>(l.size());
        for (DocumentModel doc : l) {
            Object value;
            try {
                value = doc.getProperty(directory.schemaName, columnName);
            } catch (ClientException e) {
                throw new DirectoryException(e);
            }
            if (value != null) {
                results.add(value.toString());
            } else {
                results.add(null);
            }
        }
        return results;
    }

    @Override
    public DocumentModel createEntry(DocumentModel entry)
            throws ClientException {
        Map<String, Object> fieldMap = entry.getProperties(directory.schemaName);
        return createEntry(fieldMap);
    }

    @Override
    public boolean hasEntry(String id) throws ClientException {
        return data.containsKey(id);
    }

}
