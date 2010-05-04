/*
 * (C) Copyright 2008-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A set of invalidations.
 * <p>
 * Records both modified and deleted fragments, as well as "parents modified"
 * fragments.
 */
public class Invalidations implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Pseudo-table to use to notify about children invalidated. */
    public static final String PARENT = "__PARENT__";

    public static final int MODIFIED = 1;

    public static final int DELETED = 2;

    public final Map<String, Set<Serializable>> modified = new HashMap<String, Set<Serializable>>();

    public final Map<String, Set<Serializable>> deleted = new HashMap<String, Set<Serializable>>();

    public boolean isEmpty() {
        return modified.isEmpty() && deleted.isEmpty();
    }

    public Map<String, Set<Serializable>> getKindMap(int kind) {
        switch (kind) {
        case MODIFIED:
            return modified;
        case DELETED:
            return deleted;
        }
        throw new AssertionError();
    }

    public void addModified(String tableName, Set<Serializable> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<Serializable> set = modified.get(tableName);
        if (set == null) {
            modified.put(tableName, set = new HashSet<Serializable>());
        }
        set.addAll(ids);
    }

    public void addDeleted(String tableName, Set<Serializable> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<Serializable> set = deleted.get(tableName);
        if (set == null) {
            deleted.put(tableName, set = new HashSet<Serializable>());
        }
        set.addAll(ids);
    }

    public void add(Serializable id, String[] tableNames, int kind) {
        Map<String, Set<Serializable>> map = getKindMap(kind);
        for (String tableName : tableNames) {
            Set<Serializable> set = map.get(tableName);
            if (set == null) {
                map.put(tableName, set = new HashSet<Serializable>());
            }
            set.add(id);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
                this.getClass().getSimpleName() + '(');
        if (!modified.isEmpty()) {
            sb.append("modified=");
            sb.append(modified.toString());
            if (!deleted.isEmpty()) {
                sb.append(',');
            }
        }
        if (!deleted.isEmpty()) {
            sb.append("deleted=");
            sb.append(deleted.toString());
        }
        sb.append(')');
        return sb.toString();
    }

}
