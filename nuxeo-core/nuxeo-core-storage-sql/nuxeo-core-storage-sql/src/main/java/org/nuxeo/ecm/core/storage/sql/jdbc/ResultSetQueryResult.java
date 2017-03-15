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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;

/**
 * Iterable query result implemented as a cursor on a SQL {@link ResultSet}.
 */
public class ResultSetQueryResult implements IterableQueryResult, Iterator<Map<String, Serializable>> {

    private QueryMaker.Query q;

    private PreparedStatement ps;

    private ResultSet rs;

    private Map<String, Serializable> next;

    private boolean eof;

    private long pos;

    private long size = -1;

    private final JDBCLogger logger;

    public ResultSetQueryResult(QueryMaker queryMaker, String query, QueryFilter queryFilter, PathResolver pathResolver,
            JDBCMapper mapper, Object... params) throws SQLException {
        logger = mapper.logger;
        q = queryMaker.buildQuery(mapper.sqlInfo, mapper.model, pathResolver, query, queryFilter, params);
        if (q == null) {
            // no result
            size = 0;
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
        ps = mapper.connection.prepareStatement(q.selectInfo.sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        int i = 1;
        for (Serializable object : q.selectParams) {
            mapper.setToPreparedStatement(ps, i++, object);
        }
        rs = ps.executeQuery();
        mapper.countExecute();
        // rs.setFetchDirection(ResultSet.FETCH_UNKNOWN); fails in H2
    }

    protected static void closePreparedStatement(PreparedStatement ps) throws SQLException {
        try {
            ps.close();
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

    @Override
    public void close() {
        if (rs == null) {
            return;
        }
        try {
            rs.close();
            closePreparedStatement(ps);
        } catch (SQLException e) {
            logger.error("Error closing statement: " + e.getMessage(), e);
        } finally {
            pos = -1;
            rs = null;
            ps = null;
        }
    }

    @Override
    public boolean isLife() {
        return rs != null;
    }

    @Override
    public boolean mustBeClosed() {
        return rs != null;
    }

    public static class ClosedIteratorException extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        public final QueryMaker.Query query;

        protected ClosedIteratorException(QueryMaker.Query q) {
            super("Query results iterator closed (" + q.selectInfo.sql + ")");
            this.query = q;
        }

    }

    protected void checkNotClosed() {
        if (rs == null) {
            throw new ClosedIteratorException(q);
        }
    }

    @Override
    public long size() {
        if (size != -1) {
            return size;
        }
        checkNotClosed();
        try {
            // save cursor pos
            int old = rs.isBeforeFirst() ? -1 : rs.isAfterLast() ? -2 : rs.getRow();
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

    @Override
    public long pos() {
        checkNotClosed();
        return pos;
    }

    @Override
    public void skipTo(long pos) {
        checkNotClosed();
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

    @Override
    public Iterator<Map<String, Serializable>> iterator() {
        checkNotClosed();
        return this;
    }

    protected Map<String, Serializable> fetchNext() throws SQLException {
        checkNotClosed();
        if (!rs.next()) {
            if (logger.isLogEnabled()) {
                logger.log("  -> END");
            }
            return null;
        }
        return fetchCurrent();
    }

    protected Map<String, Serializable> fetchCurrent() throws SQLException {
        checkNotClosed();
        Map<String, Serializable> map = q.selectInfo.mapMaker.makeMap(rs);
        if (logger.isLogEnabled()) {
            logger.logMap(map);
        }
        return map;
    }

    @Override
    public boolean hasNext() {
        if (eof) {
            return false;
        }
        checkNotClosed();
        if (next != null) {
            return true;
        }
        try {
            next = fetchNext();
        } catch (SQLException e) {
            logger.error("Error fetching next: " + e.getMessage(), e);
        }
        eof = next == null;
        return !eof;
    }

    @Override
    public Map<String, Serializable> next() {
        checkNotClosed();
        if (!hasNext()) {
            pos = -1;
            throw new NoSuchElementException();
        }
        Map<String, Serializable> n = next;
        next = null;
        pos++;
        return n;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
