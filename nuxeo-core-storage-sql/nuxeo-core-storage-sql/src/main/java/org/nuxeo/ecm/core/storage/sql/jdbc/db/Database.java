/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

/**
 * A collection of {@link Table}s.
 *
 * @author Florent Guillaume
 */
public class Database implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Dialect dialect;

    protected final Map<String, Table> tables;

    protected final Set<String> physicalTables;

    public Database(Dialect dialect) {
        this.dialect = dialect;
        tables = new LinkedHashMap<String, Table>();
        physicalTables = new HashSet<String>();
    }

    public Table addTable(String name) throws IllegalArgumentException {
        String physicalName = dialect.getTableName(name);
        if (!physicalTables.add(physicalName)) {
            throw new IllegalArgumentException("Duplicate table name: "
                    + physicalName);
        }
        Table table = new TableImpl(dialect, physicalName, name);
        tables.put(name, table);
        return table;
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
            buf.append(table.getPhysicalName());
            if (iter.hasNext()) {
                buf.append(',');
            }
        }
        buf.append(')');
        return buf.toString();
    }

}
