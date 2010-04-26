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

/**
 * A {@code DELETE} statement.
 *
 * @author Florent Guillaume
 */
public class Delete implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Table table;

    private String where;

    public Delete(Table table) {
        this.table = table;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getStatement() {
        StringBuilder buf = new StringBuilder(50);
        buf.append("DELETE FROM ");
        buf.append(table.getQuotedName());
        if (where != null && where.length() != 0) {
            buf.append(" WHERE ");
            buf.append(where);
        }
        return buf.toString();
    }
}
