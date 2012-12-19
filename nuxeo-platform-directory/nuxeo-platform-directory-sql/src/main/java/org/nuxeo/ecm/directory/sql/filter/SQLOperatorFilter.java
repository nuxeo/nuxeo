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

import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Simple SQLComplexFilter to handle a different operator than =
 * It may be >, <, >=, <=
 *
 * Nothing is done on the right side part.
 *
 * @since 5.7
 */
public class SQLOperatorFilter extends SQLComplexFilter {

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
