/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
