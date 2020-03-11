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
        StringBuilder sb = new StringBuilder(50);
        sb.append("DELETE FROM ");
        sb.append(table.getQuotedName());
        if (where != null && where.length() != 0) {
            sb.append(" WHERE ");
            sb.append(where);
        }
        return sb.toString();
    }
}
