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
package org.nuxeo.ecm.core.storage.dbs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of invalidations for a given repository.
 * <p>
 * Records both modified and deleted fragments, as well as "parents modified" fragments.
 *
 * @since 8.10
 */
public class DBSInvalidations {

    /**
     * Maximum number of invalidations kept, after which only {@link #all} is set. This avoids accumulating too many
     * invalidations in memory, at the expense of more coarse-grained invalidations.
     */
    public static final int MAX_SIZE = 10000;

    /**
     * Used locally when invalidating everything, or when too many invalidations have been received.
     */
    public boolean all;

    /** null when empty */
    public Set<String> ids;

    public DBSInvalidations() {
    }

    public DBSInvalidations(boolean all) {
        this.all = all;
    }

    public boolean isEmpty() {
        return ids == null && !all;
    }

    public void clear() {
        all = false;
        ids = null;
    }

    protected void setAll() {
        all = true;
        ids = null;
    }

    protected void checkMaxSize() {
        if (ids != null && ids.size() > MAX_SIZE) {
            setAll();
        }
    }

    public void add(DBSInvalidations other) {
        if (other == null) {
            return;
        }
        if (all) {
            return;
        }
        if (other.all) {
            setAll();
            return;
        }
        if (other.ids != null) {
            if (ids == null) {
                ids = new HashSet<>();
            }
            ids.addAll(other.ids);
        }
        checkMaxSize();
    }

    public void add(String id) {
        if (all) {
            return;
        }
        if (ids == null) {
            ids = new HashSet<>();
        }
        ids.add(id);
        checkMaxSize();
    }

    public void addAll(Collection<String> idsToAdd) {
        if (all) {
            return;
        }
        if (ids == null) {
            ids = new HashSet<>(idsToAdd);
        } else {
            ids.addAll(idsToAdd);
        }
        checkMaxSize();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + '(');
        if (all) {
            sb.append("all=true");
        }
        if (ids != null) {
            sb.append("ids=");
            sb.append(ids);
        }
        sb.append(')');
        return sb.toString();
    }

}
