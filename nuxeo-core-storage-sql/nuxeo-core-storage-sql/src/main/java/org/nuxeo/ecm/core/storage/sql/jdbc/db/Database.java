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

package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

/**
 * A collection of {@link Table}s.
 *
 * @author Florent Guillaume
 */
public class Database implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final BinaryManager binaryManager;

    protected final Dialect dialect;

    protected final Map<String, Table> tables;

    protected final Set<String> physicalTables;

    public Database(RepositoryImpl repository, Dialect dialect) {
        binaryManager = repository.getBinaryManager();
        this.dialect = dialect;
        tables = new LinkedHashMap<String, Table>();
        physicalTables = new HashSet<String>();
    }

    public BinaryManager getBinaryManager() {
        return binaryManager;
    }

    public Table addTable(String name) throws IllegalArgumentException {
        String physicalName = getTablePhysicalName(name);
        if (!physicalTables.add(physicalName)) {
            throw new IllegalArgumentException("Duplicate table name: "
                    + physicalName);
        }
        Table table = new TableImpl(this, physicalName);
        tables.put(name, table);
        return table;
    }

    protected String getPhysicalName(String name) {
        String physicalName = dialect.storesUpperCaseIdentifiers() ? name.toUpperCase()
                : name.toLowerCase();
        return physicalName.replace(':', '_');
    }

    protected String getTablePhysicalName(String name) {
        return getPhysicalName(name);
    }

    protected String getColumnPhysicalName(String name) {
        return getPhysicalName(name);
    }

    public Table getTable(String name) {
        return tables.get(name);
    }

    public Collection<Table> getTables() {
        return tables.values();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        for (Iterator<Table> iter = tables.values().iterator(); iter.hasNext();) {
            Table table = iter.next();
            buf.append(table.getName());
            if (iter.hasNext()) {
                buf.append(',');
            }
        }
        buf.append(')');
        return buf.toString();
    }

}
