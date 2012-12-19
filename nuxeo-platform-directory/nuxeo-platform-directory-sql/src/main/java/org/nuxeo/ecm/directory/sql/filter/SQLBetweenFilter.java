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
import org.nuxeo.ecm.directory.DirectoryException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

/**
 * Make a BETWEEN SQL predicate
 *
 * @since 5.7
 */
public class SQLBetweenFilter extends SQLComplexFilter {

    private Calendar from;

    private Calendar to;

    public SQLBetweenFilter(Calendar from, Calendar to) {
        super("BETWEEN");
        this.from = from;
        this.to = to;
    }

    @Override
    public int doSetFieldValue(PreparedStatement ps, int index, Column column)
            throws SQLException {
        column.setToPreparedStatement(ps, index, from);
        column.setToPreparedStatement(ps, index + 1, to);
        return index + 2;
    }

    @Override
    public String getRightSide() {
        return "? AND ?";
    }
}
