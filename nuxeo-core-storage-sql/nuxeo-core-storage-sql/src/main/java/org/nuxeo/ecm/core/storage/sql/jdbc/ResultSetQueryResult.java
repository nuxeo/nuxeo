/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;

/**
 * Iterable query result implemented as a cursor on a SQL {@link ResultSet}.
 */
public class ResultSetQueryResult implements IterableQueryResult,
        Iterator<Map<String, Serializable>> {

    private QueryMaker.Query q;

    private PreparedStatement ps;

    private ResultSet rs;

    private Map<String, Serializable> next;

    private boolean eof;

    private long pos;

    private long size = -1;

    private final JDBCMapperLogger logger;

    public ResultSetQueryResult(QueryMaker queryMaker, String query,
            QueryFilter queryFilter, PathResolver pathResolver,
            JDBCMapper mapper, Object... params) throws StorageException,
            SQLException {
        logger = mapper.logger;
        q = queryMaker.buildQuery(mapper.sqlInfo, mapper.model, pathResolver,
                query, queryFilter, params);
        if (q == null) {
            logger.log("Query cannot return anything due to conflicting clauses");
            ps = null;
            rs = null;
            eof = true;
            return;
        } else {
            eof = false;
        }
        if (logger.isLogEnabled()) {
            logger.logSQL(q.selectInfo.sql, q.selectParams);
        }
        ps = mapper.connection.prepareStatement(q.selectInfo.sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        int i = 1;
        for (Object object : q.selectParams) {
            if (object instanceof Calendar) {
                Calendar cal = (Calendar) object;
                Timestamp ts = new Timestamp(cal.getTimeInMillis());
                ps.setTimestamp(i++, ts, cal); // cal passed for timezone
            } else if (object instanceof String[]) {
                Array array = mapper.sqlInfo.dialect.createArrayOf(
                        Types.VARCHAR, (Object[]) object, mapper.connection);
                ps.setArray(i++, array);
            } else {
                ps.setObject(i++, object);
            }
        }
        rs = ps.executeQuery();
        // rs.setFetchDirection(ResultSet.FETCH_UNKNOWN); fails in H2
    }

    protected static void closePreparedStatement(PreparedStatement ps)
            throws SQLException {
        try {
            ps.close();
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

    public void close() {
        if (rs != null) {
            try {
                rs.close();
                closePreparedStatement(ps);
            } catch (SQLException e) {
                logger.error("Error closing statement: " + e.getMessage(), e);
            } finally {
                pos = -1;
                rs = null;
                ps = null;
                q = null;
            }
        }
    }

    @Override
    protected void finalize() {
        if (rs != null) {
            logger.warn("Closing an IterableQueryResult for you. Please close them yourself.");
        }
        close();
    }

    public long size() {
        if (size != -1) {
            return size;
        }
        try {
            // save cursor pos
            int old = rs.isBeforeFirst() ? -1 : rs.isAfterLast() ? -2
                    : rs.getRow();
            // find size
            rs.last();
            size = rs.getRow();
            // set back cursor
            if (old == -1) {
                rs.beforeFirst();
            } else if (old == -2) {
                rs.afterLast();
            } else if (old != 0) {
                rs.absolute(old);
            }
            return size;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long pos() {
        return pos;
    }

    public void skipTo(long pos) {
        if (rs == null || pos < 0) {
            this.pos = -1;
            return;
        }
        try {
            boolean available = rs.absolute((int) pos + 1);
            if (available) {
                next = fetchCurrent();
                eof = false;
                this.pos = pos;
            } else {
                // after last row
                next = null;
                eof = true;
                this.pos = -1; // XXX
            }
        } catch (SQLException e) {
            logger.error("Error skipping to: " + pos + ": " + e.getMessage(), e);
        }
    }

    public Iterator<Map<String, Serializable>> iterator() {
        return this;
    }

    protected Map<String, Serializable> fetchNext() throws StorageException,
            SQLException {
        if (rs == null) {
            return null;
        }
        if (!rs.next()) {
            if (logger.isLogEnabled()) {
                logger.log("  -> END");
            }
            return null;
        }
        return fetchCurrent();
    }

    protected Map<String, Serializable> fetchCurrent() throws SQLException {
        Map<String, Serializable> map = q.selectInfo.mapMaker.makeMap(rs);
        if (logger.isLogEnabled()) {
            logger.logMap(map);
        }
        return map;
    }

    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        if (eof) {
            return false;
        }
        try {
            next = fetchNext();
        } catch (Exception e) {
            logger.error("Error fetching next: " + e.getMessage(), e);
        }
        eof = next == null;
        return !eof;
    }

    public Map<String, Serializable> next() {
        if (!hasNext()) {
            pos = -1;
            throw new NoSuchElementException();
        }
        Map<String, Serializable> n = next;
        next = null;
        pos++;
        return n;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
