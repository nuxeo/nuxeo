/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The persistence context in use by a session.
 * <p>
 * All non-saved modified data is referenced here. At save time, the data is
 * sent to the database by the {@link Mapper}.
 * <p>
 * This class is not thread-safe, it should be tied to a single session and the
 * session itself should not be used concurrently.
 *
 * @author Florent Guillaume
 */
public class PersistenceContext {

    private static final Log log = LogFactory.getLog(PersistenceContext.class);

    private final Mapper mapper;

    private final Map<String, PersistenceContextByTable> contexts;

    private final Model model;

    PersistenceContext(Mapper mapper) {
        this.mapper = mapper;
        model = mapper.getModel();
        contexts = new HashMap<String, PersistenceContextByTable>();
    }

    public void close() {
        mapper.close();
        for (PersistenceContextByTable context : contexts.values()) {
            context.close();
        }
        // don't clean the contexts, we keep the pristine cache around
    }

    /**
     * Saves all the data to persistent storage.
     *
     * @throws StorageException
     */
    public void save() throws StorageException {
        log.debug("Saving persistence context");
        /*
         * First, create the main rows to get final ids for each.
         */
        PersistenceContextByTable mainContext = contexts.get(model.MAIN_TABLE_NAME);
        Map<Serializable, Serializable> idMap;
        if (mainContext != null) {
            idMap = mainContext.saveMain();
        } else {
            idMap = Collections.emptyMap();
        }
        /*
         * Then save all other rows, taking the map of ids into account.
         */
        for (PersistenceContextByTable context : contexts.values()) {
            context.save(idMap);
        }
        // no need to clear the contexts, they'd get reallocate soon anyway
        log.debug("End of save");
    }

    /**
     * Creates a new row in the context.
     *
     * @param tableName the table name
     * @param id the id
     * @param map the fragments map, or {@code null}
     * @return the created row
     * @throws StorageException if the row is already in the context
     */
    public Fragment createSingleRow(String tableName, Serializable id,
            Map<String, Serializable> map) throws StorageException {
        PersistenceContextByTable context = contexts.get(tableName);
        if (context == null) {
            context = new PersistenceContextByTable(tableName, mapper);
            contexts.put(tableName, context);
        }
        return context.create(id, map);
    }

    /**
     * Gets a row given a table name and an id. If the row is not in the
     * context, fetch it from the mapper.
     * <p>
     * XXX differentiate unknown vs deleted
     *
     * @param tableName the row table name
     * @param id the row id
     * @return the row, or {@code null} if none is found
     * @throws StorageException
     */
    public Fragment get(String tableName, Serializable id)
            throws StorageException {
        PersistenceContextByTable context = contexts.get(tableName);
        if (context == null) {
            context = new PersistenceContextByTable(tableName, mapper);
            contexts.put(tableName, context);
        }
        return context.get(id);
    }

    /**
     * Find a row in the hierarchy table given its parent id and name. If the
     * row is not in the context, fetch it from the mapper.
     *
     * @param parentId the parent id
     * @param name the name
     * @return the row, or {@code null} if none is found
     * @throws StorageException
     */
    public SingleRow getByHier(Serializable parentId, String name)
            throws StorageException {
        String tableName = model.HIER_TABLE_NAME;
        PersistenceContextByTable context = contexts.get(tableName);
        if (context == null) {
            context = new PersistenceContextByTable(tableName, mapper);
            contexts.put(tableName, context);
        }
        return context.getByHier(parentId, name);
    }

    /**
     * Removes a row.
     *
     * @param row
     * @throws StorageException
     */
    public void remove(Fragment row) throws StorageException {
        String tableName = row.getTableName();
        PersistenceContextByTable context = contexts.get(tableName);
        if (context == null) {
            log.error("Removing row not in a context: " + row);
            return;
        }
        context.remove(row);
    }

}
