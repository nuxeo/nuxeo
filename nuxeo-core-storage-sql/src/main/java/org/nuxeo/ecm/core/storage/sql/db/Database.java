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
 *
 * $Id: Database.java 18365 2007-2008-05-08 10:24:00Z sfermigier $
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.midi.Sequence;

/**
 * A collection of {@link Table}s and {@link Sequence}s.
 *
 * @author Florent Guillaume
 */
public class Database implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Table> tables;

    public Database() {
        tables = new LinkedHashMap<String, Table>();
    }

    public void addTable(Table table) throws IllegalArgumentException {
        String name = table.getName();
        if (tables.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate table " + name);
        }
        tables.put(name, table);
    }

    public Collection<Table> getTables() {
        return tables.values();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        Iterator<Table> iter = tables.values().iterator();
        while (iter.hasNext()) {
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
