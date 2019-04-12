/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Simple SQLComplexFilter to handle a different operator than = It may be >, <, >=, <= Nothing is done on the right
 * side part.
 *
 * @since 5.7
 * @deprecated since 10.3, use {@link org.nuxeo.ecm.directory.sql.SQLSession#query(org.nuxeo.ecm.core.query.sql.model.QueryBuilder, boolean)} instead
 */
@Deprecated
public class SQLOperatorFilter extends SQLComplexFilter {

    private static final long serialVersionUID = 1L;
    private Serializable value;

    public SQLOperatorFilter(String operator, Serializable value) {
        super(operator);
        this.value = value;
    }

    @Override
    public int doSetFieldValue(PreparedStatement ps, int index, Column column) throws SQLException {
        column.setToPreparedStatement(ps, index, value);
        return index + 1;
    }
}
