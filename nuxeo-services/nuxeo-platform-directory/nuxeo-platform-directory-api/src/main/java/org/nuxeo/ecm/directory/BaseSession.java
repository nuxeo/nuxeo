/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * Base session class with helper methods common to all kinds of directory
 * sessions.
 *
 * @author Anahide Tchertchian
 * @since 5.2M4
 */
public abstract class BaseSession implements Session {

    protected static final String READONLY_ENTRY_FLAG = "READONLY_ENTRY";

    /**
     * Returns a bare document model suitable for directory implementations.
     * <p>
     * Can be used for creation screen
     *
     * @since 5.2M4
     */
    public static DocumentModel createEntryModel(String sessionId,
            String schema, String id, Map<String, Object> values)
            throws PropertyException {
        DocumentModelImpl entry = new DocumentModelImpl(sessionId, schema, id,
                null, null, null, null, new String[] { schema },
                new HashSet<String>(), null, null);
        DataModel dataModel;
        if (values == null) {
            dataModel = new DataModelImpl(schema,
                    Collections.<String, Object> emptyMap());
        } else {
            dataModel = new DataModelImpl(schema);
            dataModel.setMap(values); // makes fields dirty
        }
        entry.addDataModel(dataModel);
        return entry;
    }

    /**
     * Returns a bare document model suitable for directory implementations.
     * <p>
     * Allow setting the readonly entry flag to {@code Boolean.TRUE}. See
     * {@code Session#isReadOnlyEntry(DocumentModel)}
     *
     * @since 5.3.1
     */
    public static DocumentModel createEntryModel(String sessionId,
            String schema, String id, Map<String, Object> values,
            boolean readOnly) throws PropertyException {
        DocumentModel entry = createEntryModel(sessionId, schema, id, values);
        if (readOnly) {
            setReadOnlyEntry(entry);
        }
        return entry;
    }

    protected static Map<String, Serializable> mkSerializableMap(
            Map<String, Object> map) {
        Map<String, Serializable> serializableMap = null;
        if (map != null) {
            serializableMap = new HashMap<String, Serializable>();
            for (String key : map.keySet()) {
                serializableMap.put(key, (Serializable) map.get(key));
            }
        }
        return serializableMap;
    }

    protected static Map<String, Object> mkObjectMap(
            Map<String, Serializable> map) {
        Map<String, Object> objectMap = null;
        if (map != null) {
            objectMap = new HashMap<String, Object>();
            for (String key : map.keySet()) {
                objectMap.put(key, map.get(key));
            }
        }
        return objectMap;
    }

    /**
     * Test whether entry comes from a read-only back-end directory.
     *
     * @since 5.3.1
     */
    public static boolean isReadOnlyEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        return contextData.getScopedValue(ScopeType.REQUEST,
                READONLY_ENTRY_FLAG) == Boolean.TRUE;
    }

    /**
     * Set the read-only flag of a directory entry. To be used by EntryAdaptor
     * implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadOnlyEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        contextData.putScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG,
                Boolean.TRUE);
    }

    /**
     * Unset the read-only flag of a directory entry. To be used by EntryAdaptor
     * implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadWriteEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        contextData.putScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG,
                Boolean.FALSE);
    }

}
