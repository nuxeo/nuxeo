/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

/**
 * A simple value holding one row of the ACLs table.
 *
 * @author Florent Guillaume
 */
public class ACLRow implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int aclpos;

    public final String aclname;

    public final int pos;

    public final boolean grant;

    public final String permission;

    public final String user;

    public final String group;

    public ACLRow(int aclpos, String aclname, int pos, boolean grant,
            String permission, String user, String group) {
        this.aclpos = aclpos;
        this.aclname = aclname;
        this.pos = pos;
        this.grant = grant;
        this.permission = permission;
        this.user = user;
        this.group = group;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + aclpos + ',' + aclname +
                ',' + pos + ',' + (grant ? "GRANT" : "DENY") + ',' +
                permission + ',' + user + ',' + group + ')';
    }
}
