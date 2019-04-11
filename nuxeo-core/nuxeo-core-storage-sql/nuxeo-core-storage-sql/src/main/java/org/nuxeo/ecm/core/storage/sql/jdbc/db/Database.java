/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
        tables = new LinkedHashMap<>();
        physicalTables = new HashSet<>();
    }

    public Table addTable(String name) throws IllegalArgumentException {
        String physicalName = dialect.getTableName(name);
        if (!physicalTables.add(physicalName)) {
            throw new IllegalArgumentException("Duplicate table name: " + physicalName);
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
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        for (Iterator<Table> iter = tables.values().iterator(); iter.hasNext();) {
            Table table = iter.next();
            sb.append(table.getPhysicalName());
            if (iter.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(')');
        return sb.toString();
    }

}
