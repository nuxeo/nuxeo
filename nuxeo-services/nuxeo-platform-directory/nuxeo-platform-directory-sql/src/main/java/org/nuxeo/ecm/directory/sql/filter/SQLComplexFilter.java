/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.directory.sql.filter;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.directory.DirectoryException;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Simple class to provide a complex filter that handles right side part and
 * operator to use while querying org.nuxeo.ecm.directory.sql.SQLDirectory
 * 
 * 
 * Warning, when using a complex filter fulltext is ignored on the field.
 * 
 * @since 5.7
 * @see org.nuxeo.ecm.directory.sql.SQLSession#query(java.util.Map,
 *      java.util.Set, java.util.Map, boolean, int, int)
 */
public abstract class SQLComplexFilter implements Serializable {

    protected String operator;

    protected Serializable value;

    public SQLComplexFilter(String operator) {
        this.operator = operator;
    }

    public int setFieldValue(PreparedStatement ps, int index, Column column)
            throws DirectoryException {
        try {
            return doSetFieldValue(ps, index, column);
        } catch (SQLException e) {
            throw new DirectoryException(
                    "SQLComplexFilter setFieldValue failed", e);
        }

    }

    public abstract int doSetFieldValue(PreparedStatement ps, int index,
            Column column) throws SQLException;

    public String getRightSide() {
        return "?";
    }

    public String getOperator() {
        return " " + operator + " ";
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
