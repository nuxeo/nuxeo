/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.directory.sql.filter;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Simple class to provide a complex filter that handles right side part and operator to use while querying
 * org.nuxeo.ecm.directory.sql.SQLDirectory Warning, when using a complex filter fulltext is ignored on the field.
 *
 * @since 5.7
 * @see org.nuxeo.ecm.directory.sql.SQLSession#query(java.util.Map, java.util.Set, java.util.Map, boolean, int, int)
 */
public abstract class SQLComplexFilter implements Serializable {

    protected String operator;

    protected Serializable value;

    public SQLComplexFilter(String operator) {
        this.operator = operator;
    }

    public int setFieldValue(PreparedStatement ps, int index, Column column) {
        try {
            return doSetFieldValue(ps, index, column);
        } catch (SQLException e) {
            throw new DirectoryException("SQLComplexFilter setFieldValue failed", e);
        }

    }

    public abstract int doSetFieldValue(PreparedStatement ps, int index, Column column) throws SQLException;

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
