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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;

/**
 * A simple value holding one row of the ACLs table.
 *
 * @author Florent Guillaume
 */
public class ACLRow implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int pos;

    public final String name;

    public final boolean grant;

    public final String permission;

    public final String user;

    public final String group;

    /**
     * @since 7.4
     */
    public final Calendar begin;

    /**
     * @since 7.4
     */
    public final Calendar end;

    /**
     * @since 7.4
     */
    public final String creator;

    /**
     * Status of the ACL row: null, 0, 1 or 2.
     *
     * @see org.nuxeo.ecm.core.api.security.ACE
     * @since 7.4.
     */
    public final Long status;

    /**
     * @since 7.4
     */
    public ACLRow(int pos, String name, boolean grant, String permission, String user, String group, String creator,
            Calendar begin, Calendar end, Long status) {
        this.pos = pos;
        this.name = name;
        this.grant = grant;
        this.permission = permission;
        this.user = user;
        this.group = group;
        this.creator = creator;
        this.begin = begin;
        this.end = end;
        this.status = status;
    }

    public ACLRow(int pos, String name, boolean grant, String permission, String user, String group) {
        this(pos, name, grant, permission, user, group, null, null, null, null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + pos + ',' + name + ',' + (grant ? "GRANT" : "DENY") + ',' + permission
                + ',' + user + ',' + group + ',' + begin + ',' + end + +',' + status + ')';
    }

    /**
     * Comparator of {@link ACLRow}s according to their pos field.
     */
    public static class ACLRowPositionComparator implements Comparator<ACLRow> {

        public static final ACLRowPositionComparator INSTANCE = new ACLRowPositionComparator();

        @Override
        public int compare(ACLRow acl1, ACLRow acl2) {
            return acl1.pos - acl2.pos;
        }
    }

}
