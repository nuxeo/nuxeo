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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

/**
 * Make a BETWEEN SQL predicate
 *
 * @since 5.7
 * @deprecated since 10.3, use {@link org.nuxeo.ecm.directory.sql.SQLSession#query(org.nuxeo.ecm.core.query.sql.model.QueryBuilder, boolean)} instead
 */
@Deprecated
public class SQLBetweenFilter extends SQLComplexFilter {

    private static final long serialVersionUID = 1L;

    private Calendar from;

    private Calendar to;

    public SQLBetweenFilter(Calendar from, Calendar to) {
        super("BETWEEN");
        this.from = from;
        this.to = to;
    }

    @Override
    public int doSetFieldValue(PreparedStatement ps, int index, Column column) throws SQLException {
        column.setToPreparedStatement(ps, index, from);
        column.setToPreparedStatement(ps, index + 1, to);
        return index + 2;
    }

    @Override
    public String getRightSide() {
        return "? AND ?";
    }
}
