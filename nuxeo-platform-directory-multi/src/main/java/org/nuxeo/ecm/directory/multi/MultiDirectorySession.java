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
 * $Id: MultiDirectorySession.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.directory.multi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 *
 */
public class MultiDirectorySession implements Session {

    private static final Log log = LogFactory.getLog(MultiDirectorySession.class);

    private final DirectoryService directoryService;

    private final SchemaManager schemaManager;

    private final MultiDirectory directory;

    private final MultiDirectoryDescriptor descriptor;

    private final String schemaName;

    private final String schemaIdField;

    private final String schemaPasswordField;

    private List<SourceInfo> sourceInfos;

    public MultiDirectorySession(MultiDirectory directory) {
        directoryService = MultiDirectoryFactory.getDirectoryService();
        schemaManager = Framework.getLocalService(SchemaManager.class);
        this.directory = directory;
        this.descriptor = directory.getDescriptor();
        this.schemaName = descriptor.schemaName;
        this.schemaIdField = descriptor.idField;
        this.schemaPasswordField = descriptor.passwordField;
    }

    protected class SubDirectoryInfo {

        final String dirName;

        final String dirSchemaName;

        final String idField;

        final boolean isAuthenticating;

        final Map<String, String> fromSource;

        final Map<String, String> toSource;

        Session session;

        SubDirectoryInfo(String dirName, String dirSchemaName, String idField,
                boolean isAuthenticating, Map<String, String> fromSource,
                Map<String, String> toSource) {
            this.dirName = dirName;
            this.dirSchemaName = dirSchemaName;
            this.idField = idField;
            this.isAuthenticating = isAuthenticating;
            this.fromSource = fromSource;
            this.toSource = toSource;
        }

        Session getSession() throws DirectoryException {
            if (session == null) {
                session = directoryService.open(dirName);
            }
            return session;
        }

        @Override
        public String toString() {
            return String.format("{directory=%s fromSource=%s toSource=%s}",
                    dirName, fromSource, toSource);
        }
    }

    protected static class SourceInfo {

        final SourceDescriptor source;

        final List<SubDirectoryInfo> subDirectoryInfos;

        final SubDirectoryInfo authDirectoryInfo;

        SourceInfo(SourceDescriptor source,
                List<SubDirectoryInfo> subDirectoryInfos,
                SubDirectoryInfo autDirectoryInfo) {
            this.source = source;
            this.subDirectoryInfos = subDirectoryInfos;
            this.authDirectoryInfo = autDirectoryInfo;
        }

        @Override
        public String toString() {
            return String.format("{source=%s infos=%s}", source.name,
                    subDirectoryInfos);
        }
    }

    private void init() throws DirectoryException {
        if (sourceInfos == null) {
            recomputeSourceInfos();
        }
    }

    /**
     * Recompute all the info needed for efficient access.
     */
    private void recomputeSourceInfos() throws DirectoryException {

        final Schema schema = schemaManager.getSchema(schemaName);
        if (schema == null) {
            throw new DirectoryException(String.format(
                    "Directory '%s' has unknown schema '%s'",
                    directory.getName(), schemaName));
        }
        final Set<String> sourceFields = new HashSet<String>();
        for (Field f : schema.getFields()) {
            sourceFields.add(f.getName().getLocalName());
        }
        if (!sourceFields.contains(schemaIdField)) {
            throw new DirectoryException(String.format(
                    "Directory '%s' schema '%s' has no id field '%s'",
                    directory.getName(), schemaName, schemaIdField));
        }

        List<SourceInfo> newSourceInfos = new ArrayList<SourceInfo>(2);
        for (SourceDescriptor source : descriptor.sources) {
            int ndirs = source.subDirectories.length;
            if (ndirs == 0) {
                throw new DirectoryException(String.format(
                        "Directory '%s' source '%s' has no subdirectories",
                        directory.getName(), source.name));
            }

            final List<SubDirectoryInfo> subDirectoryInfos = new ArrayList<SubDirectoryInfo>(
                    ndirs);

            SubDirectoryInfo authDirectoryInfo = null;
            for (SubDirectoryDescriptor subDir : source.subDirectories) {
                final String dirName = subDir.name;
                final String dirSchemaName = directoryService.getDirectorySchema(dirName);
                final String dirIdField = directoryService.getDirectoryIdField(dirName);
                final boolean dirIsAuth = directoryService.getDirectoryPasswordField(dirName) != null;
                final Map<String, String> fromSource = new HashMap<String, String>();
                final Map<String, String> toSource = new HashMap<String, String>();

                // XXX check authenticating
                final Schema dirSchema = schemaManager.getSchema(dirSchemaName);
                if (dirSchema == null) {
                    throw new DirectoryException(String.format(
                            "Directory '%s' source '%s' subdirectory '%s' "
                                    + "has unknown schema '%s'",
                            directory.getName(), source.name, dirName,
                            dirSchemaName));
                }
                // record default field mappings if same name
                final Set<String> dirSchemaFields = new HashSet<String>();
                for (Field f : dirSchema.getFields()) {
                    final String fieldName = f.getName().getLocalName();
                    dirSchemaFields.add(fieldName);
                    if (sourceFields.contains(fieldName)) {
                        // XXX check no duplicates!
                        fromSource.put(fieldName, fieldName);
                        toSource.put(fieldName, fieldName);
                    }
                }
                // treat renamings
                // XXX id field ?
                for (FieldDescriptor field : subDir.fields) {
                    final String sourceFieldName = field.forField;
                    final String fieldName = field.name;
                    if (!sourceFields.contains(sourceFieldName)) {
                        throw new DirectoryException(String.format(
                                "Directory '%s' source '%s' subdirectory '%s' "
                                        + "has mapping for unknown field '%s'",
                                directory.getName(), source.name, dirName,
                                sourceFieldName));
                    }
                    if (!dirSchemaFields.contains(fieldName)) {
                        throw new DirectoryException(String.format(
                                "Directory '%s' source '%s' subdirectory '%s' "
                                        + "has mapping of unknown field' '%s'",
                                directory.getName(), source.name, dirName,
                                fieldName));
                    }
                    fromSource.put(sourceFieldName, fieldName);
                    toSource.put(fieldName, sourceFieldName);
                }
                SubDirectoryInfo subDirectoryInfo = new SubDirectoryInfo(
                        dirName, dirSchemaName, dirIdField, dirIsAuth,
                        fromSource, toSource);
                subDirectoryInfos.add(subDirectoryInfo);

                if (dirIsAuth) {
                    if (authDirectoryInfo != null) {
                        throw new DirectoryException(
                                String.format(
                                        "Directory '%s' source '%s' has two subdirectories "
                                                + "with a password field, '%s' and '%s'",
                                        directory.getName(), source.name,
                                        authDirectoryInfo.dirName, dirName));
                    }
                    authDirectoryInfo = subDirectoryInfo;
                }
            }
            if (isAuthenticating() && authDirectoryInfo == null) {
                throw new DirectoryException(String.format(
                        "Directory '%s' source '%s' has no subdirectory "
                                + "with a password field", directory.getName(),
                        source.name));
            }
            newSourceInfos.add(new SourceInfo(source, subDirectoryInfos,
                    authDirectoryInfo));
        }
        sourceInfos = newSourceInfos;
    }

    public void close() throws DirectoryException {
        if (sourceInfos == null) {
            return;
        }
        DirectoryException exc = null;
        for (SourceInfo sourceInfo : sourceInfos) {
            for (SubDirectoryInfo subDirectoryInfo : sourceInfo.subDirectoryInfos) {
                Session session = subDirectoryInfo.session;
                subDirectoryInfo.session = null;
                if (session != null) {
                    try {
                        session.close();
                    } catch (DirectoryException e) {
                        // remember exception, we want to close all session
                        // first
                        if (exc == null) {
                            exc = e;
                        } else {
                            // we can't reraise both, log this one
                            log.error("Error closing directory "
                                    + subDirectoryInfo.dirName, e);
                        }
                    }
                }
            }
        }
        directory.removeSession(this);
        if (exc != null) {
            throw exc;
        }
    }

    public void commit() throws ClientException {
        if (sourceInfos == null) {
            return;
        }
        for (SourceInfo sourceInfo : sourceInfos) {
            for (SubDirectoryInfo subDirectoryInfo : sourceInfo.subDirectoryInfos) {
                Session session = subDirectoryInfo.session;
                if (session != null) {
                    session.commit();
                }
            }
        }
    }

    public void rollback() throws ClientException {
        if (sourceInfos == null) {
            return;
        }
        for (SourceInfo sourceInfo : sourceInfos) {
            for (SubDirectoryInfo subDirectoryInfo : sourceInfo.subDirectoryInfos) {
                Session session = subDirectoryInfo.session;
                if (session != null) {
                    session.rollback();
                }
            }
        }
    }

    public String getIdField() {
        return schemaIdField;
    }

    public String getPasswordField() {
        return schemaPasswordField;
    }

    public boolean isAuthenticating() {
        return schemaPasswordField != null;
    }

    public boolean isReadOnly() {
        return Boolean.TRUE.equals(descriptor.readOnly);
    }

    public boolean authenticate(String username, String password)
            throws ClientException {
        init();
        for (SourceInfo sourceInfo : sourceInfos) {
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                if (!dirInfo.isAuthenticating) {
                    continue;
                }
                if (dirInfo.getSession().authenticate(username, password)) {
                    return true;
                }
            }
        }
        return false;
    }

    public DocumentModel getEntry(String id) throws DirectoryException {
        init();
        source_loop: for (SourceInfo sourceInfo : sourceInfos) {
            final Map<String, Object> map = new HashMap<String, Object>();
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                final DocumentModel entry = dirInfo.getSession().getEntry(id);
                if (entry == null) {
                    // not in this source
                    continue source_loop;
                    // TODO treat the case of "lazily filled dirs"
                    // (CPS "missing entry")
                }
                for (Entry<String, String> e : dirInfo.toSource.entrySet()) {
                    map.put(e.getValue(), entry.getProperty(
                            dirInfo.dirSchemaName, e.getKey()));
                }

            }
            // ok we have the data
            final DocumentModelImpl entry = new DocumentModelImpl(null,
                    schemaName, id, null, null, null,
                    new String[] { schemaName }, null);
            entry.addDataModel(new DataModelImpl(schemaName, map));
            return entry;
        }
        return null;
    }

    @SuppressWarnings("boxing")
    public DocumentModelList getEntries() throws ClientException {
        init();

        // list of entries
        final DocumentModelList results = new DocumentModelListImpl();
        // entry ids already seen (mapped to the source name)
        final Map<String, String> seen = new HashMap<String, String>();

        for (SourceInfo sourceInfo : sourceInfos) {
            // accumulated map for each entry
            final Map<String, Map<String, Object>> maps = new HashMap<String, Map<String, Object>>();
            // number of dirs seen for each entry
            final Map<String, Integer> counts = new HashMap<String, Integer>();
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                final DocumentModelList entries = dirInfo.getSession().getEntries();
                for (DocumentModel entry : entries) {
                    final String id = entry.getId();
                    // find or create map for this entry
                    Map<String, Object> map = maps.get(id);
                    if (map == null) {
                        map = new HashMap<String, Object>();
                        maps.put(id, map);
                        counts.put(id, 1);
                    } else {
                        counts.put(id, counts.get(id) + 1);
                    }
                    // put entry data in map
                    for (Entry<String, String> e : dirInfo.toSource.entrySet()) {
                        map.put(e.getValue(), entry.getProperty(
                                dirInfo.dirSchemaName, e.getKey()));
                    }
                }
            }

            // now create entries for all full maps
            int numdirs = sourceInfo.subDirectoryInfos.size();
            ((ArrayList<?>) results).ensureCapacity(results.size()
                    + maps.size());
            for (Entry<String, Map<String, Object>> e : maps.entrySet()) {
                final String id = e.getKey();
                if (seen.containsKey(id)) {
                    log.warn(String.format(
                            "Entry '%s' is present in source '%s' but also in source '%s'. "
                                    + "The second one will be ignored.", id,
                            seen.get(id), sourceInfo.source.name));
                    continue;
                }
                final Map<String, Object> map = e.getValue();
                if (counts.get(id) != numdirs) {
                    log.warn(String.format(
                            "Entry '%s' for source '%s' is not present in all directories. "
                                    + "It will be skipped.", id,
                            sourceInfo.source.name));
                    continue;
                }
                seen.put(id, sourceInfo.source.name);
                final DocumentModelImpl entry = new DocumentModelImpl(null,
                        schemaName, id, null, null, null,
                        new String[] { schemaName }, null);
                entry.addDataModel(new DataModelImpl(schemaName, map));
                results.add(entry);
            }
        }
        return results;
    }

    public DocumentModel createEntry(Map<String, Object> fieldMap)
            throws ClientException {
        init();
        final Object rawid = fieldMap.get(schemaIdField);
        if (rawid == null) {
            throw new DirectoryException(String.format(
                    "Entry is missing id field '%s'", schemaIdField));
        }
        final String id = String.valueOf(rawid); // XXX allow longs too
        for (SourceInfo sourceInfo : sourceInfos) {
            if (!sourceInfo.source.creation) {
                continue;
            }
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(dirInfo.idField, id);
                for (Entry<String, String> e : dirInfo.fromSource.entrySet()) {
                    map.put(e.getValue(), fieldMap.get(e.getKey()));
                }
                dirInfo.getSession().createEntry(map);
            }
            return getEntry(id);
        }
        throw new DirectoryException(String.format(
                "Directory '%s' has no source allowing creation",
                directory.getName()));
    }

    public void deleteEntry(DocumentModel docModel) throws ClientException {
        deleteEntry(docModel.getId());
    }

    public void deleteEntry(String id) throws ClientException {
        init();
        for (SourceInfo sourceInfo : sourceInfos) {
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                dirInfo.getSession().deleteEntry(id);
            }
        }
    }

    public void deleteEntry(String id, Map<String, String> map)
            throws DirectoryException {
        log.warn("Calling deleteEntry extended on multi directory");
        try {
            deleteEntry(id);
        } catch (ClientException e) {
            // XXX doh
            if (e instanceof DirectoryException) {
                throw (DirectoryException) e;
            } else {
                throw new DirectoryException(e);
            }
        }
    }

    public void updateEntry(DocumentModel docModel) throws ClientException {
        init();
        final String id = docModel.getId();
        Map<String, Object> fieldMap = docModel.getDataModel(schemaName).getMap();

        for (SourceInfo sourceInfo : sourceInfos) {
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(dirInfo.idField, id);
                for (Entry<String, String> e : dirInfo.fromSource.entrySet()) {
                    map.put(e.getValue(), fieldMap.get(e.getKey()));
                }
                if (map.size() > 1) {
                    final DocumentModelImpl entry = new DocumentModelImpl(null,
                            dirInfo.dirSchemaName, id, null, null, null,
                            new String[] { dirInfo.dirSchemaName }, null);
                    DataModel dataModel = new DataModelImpl(
                            dirInfo.dirSchemaName);
                    dataModel.setMap(map); // makes fields dirty
                    entry.addDataModel(dataModel);
                    dirInfo.getSession().updateEntry(entry);
                }
            }
        }
    }

    public DocumentModelList query(Map<String, Object> filter)
            throws ClientException {
        return query(filter, Collections.<String> emptySet());
    }

    public DocumentModelList query(Map<String, Object> filter,
            Set<String> fulltext) throws ClientException {
        return query(filter, fulltext, Collections.<String, String> emptyMap());
    }

    @SuppressWarnings("boxing")
    public DocumentModelList query(Map<String, Object> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws ClientException {

        init();

        // list of entries
        final DocumentModelList results = new DocumentModelListImpl();
        // entry ids already seen (mapped to the source name)
        final Map<String, String> seen = new HashMap<String, String>();

        for (SourceInfo sourceInfo : sourceInfos) {
            // accumulated map for each entry
            final Map<String, Map<String, Object>> maps = new HashMap<String, Map<String, Object>>();
            // number of dirs seen for each entry
            final Map<String, Integer> counts = new HashMap<String, Integer>();

            boolean firstDir = true;
            for (SubDirectoryInfo dirInfo : sourceInfo.subDirectoryInfos) {
                // compute filter
                final Map<String, Object> dirFilter = new HashMap<String, Object>();
                for (Entry<String, Object> e : filter.entrySet()) {
                    final String fieldName = dirInfo.fromSource.get(e.getKey());
                    if (fieldName == null) {
                        continue;
                    }
                    dirFilter.put(fieldName, e.getValue());
                }
                // compute fulltext
                Set<String> dirFulltext = new HashSet<String>();
                for (String sourceFieldName : fulltext) {
                    final String fieldName = dirInfo.fromSource.get(sourceFieldName);
                    if (fieldName != null) {
                        dirFulltext.add(fieldName);
                    }
                }
                // make query to subdirectory
                DocumentModelList l = dirInfo.getSession().query(dirFilter,
                        dirFulltext);
                for (DocumentModel entry : l) {
                    final String id = entry.getId();
                    Map<String, Object> map;
                    if (firstDir) {
                        map = new HashMap<String, Object>();
                        maps.put(id, map);
                        counts.put(id, 1);
                    } else {
                        map = maps.get(id);
                        if (map == null) {
                            // intersection of all subdirectories
                            continue;
                        }
                        counts.put(id, counts.get(id) + 1);
                    }
                    for (Entry<String, String> e : dirInfo.toSource.entrySet()) {
                        map.put(e.getValue(), entry.getProperty(
                                dirInfo.dirSchemaName, e.getKey()));
                    }
                }
                firstDir = false;
            }
            // intersection, ignore entries not in all subdirectories
            final int numdirs = sourceInfo.subDirectoryInfos.size();
            for (Iterator<String> it = maps.keySet().iterator(); it.hasNext();) {
                final String id = it.next();
                if (counts.get(id) != numdirs) {
                    it.remove();
                }
            }
            // now create entries
            ((ArrayList<?>) results).ensureCapacity(results.size()
                    + maps.size());
            for (Entry<String, Map<String, Object>> e : maps.entrySet()) {
                final String id = e.getKey();
                if (seen.containsKey(id)) {
                    log.warn(String.format(
                            "Entry '%s' is present in source '%s' but also in source '%s'. "
                                    + "The second one will be ignored.", id,
                            seen.get(id), sourceInfo.source.name));
                    continue;
                }
                final Map<String, Object> map = e.getValue();
                seen.put(id, sourceInfo.source.name);
                final DocumentModelImpl entry = new DocumentModelImpl(null,
                        schemaName, id, null, null, null,
                        new String[] { schemaName }, null);
                entry.addDataModel(new DataModelImpl(schemaName, map));
                results.add(entry);
            }
        }
        if (!orderBy.isEmpty()) {
            directory.orderEntries(results, orderBy);
        }
        return results;
    }

    public List<String> getProjection(Map<String, Object> filter,
            String columnName) throws ClientException {
        return getProjection(filter, Collections.<String> emptySet(),
                columnName);
    }

    public List<String> getProjection(Map<String, Object> filter,
            Set<String> fulltext, String columnName) throws ClientException {

        // There's no way to do an efficient getProjection to a source with
        // multiple subdirectories given the current API (we'd need an API that
        // passes several columns).
        // So just do a non-optimal implementation for now.

        final DocumentModelList entries = query(filter, fulltext);
        final List<String> results = new ArrayList<String>(entries.size());
        for (DocumentModel entry : entries) {
            final Object value = entry.getProperty(schemaName, columnName);
            if (value == null) {
                results.add(null);
            } else {
                results.add(value.toString());
            }
        }
        return results;
    }
}
