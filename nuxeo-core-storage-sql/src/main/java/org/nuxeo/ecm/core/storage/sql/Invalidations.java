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
public class Invalidations {

    public static final String PARENTS_KEY = "__PARENTS__";

    public final Map<String, Set<Serializable>> modified = new HashMap<String, Set<Serializable>>();

    public final Map<String, Set<Serializable>> deleted = new HashMap<String, Set<Serializable>>();

    public void addModified(String tableName, Set<Serializable> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<Serializable> set = modified.get(tableName);
        if (set == null) {
            set = new HashSet<Serializable>();
            modified.put(tableName, set);
        }
        set.addAll(ids);
    }

    public void addDeleted(String tableName, Set<Serializable> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<Serializable> set = deleted.get(tableName);
        if (set == null) {
            set = new HashSet<Serializable>();
            deleted.put(tableName, set);
        }
        set.addAll(ids);
    }

}
