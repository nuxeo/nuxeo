/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.pubsub.SerializableInvalidations;

/**
 * A set of invalidations for a given repository.
 * <p>
 * Records both modified and deleted fragments, as well as "parents modified" fragments.
 *
 * @since 8.10
 */
public class DBSInvalidations implements SerializableInvalidations {

    private static final long serialVersionUID = 1L;

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

    @Override
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

    @Override
    public void add(SerializableInvalidations o) {
        DBSInvalidations other = (DBSInvalidations) o;
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

    private static final String UTF_8 = "UTF-8";

    private static final int ALL_IDS = (byte) 'A';

    private static final int ID_SEP = (byte) ',';

    @Override
    public void serialize(OutputStream out) throws IOException {
        if (all) {
            out.write(ALL_IDS);
        } else if (ids != null) {
            for (String id : ids) {
                out.write(ID_SEP);
                out.write(id.getBytes(UTF_8));
            }
        }
    }

    public static DBSInvalidations deserialize(InputStream in) throws IOException {
        int first = in.read();
        if (first == -1) {
            // empty message
            return null;
        }
        DBSInvalidations invalidations = new DBSInvalidations();
        if (first == ALL_IDS) {
            invalidations.setAll();
        } else if (first != ID_SEP) {
            // invalid message
            return null;
        } else {
            ByteArrayOutputStream baout = new ByteArrayOutputStream(36); // typical uuid size
            for (;;) {
                int b = in.read(); // we read from a ByteArrayInputStream so one at a time is ok
                if (b == ID_SEP || b == -1) {
                    invalidations.add(baout.toString(UTF_8));
                    if (b == -1) {
                        break;
                    }
                    baout.reset();
                } else {
                    baout.write(b);
                }
            }
        }
        return invalidations;
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
