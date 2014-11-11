/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Collection IO for arrays of scalar values.
 */
public class ScalarCollectionIO implements CollectionIO {

    public static final CollectionIO INSTANCE = new ScalarCollectionIO();

    @Override
    public Serializable getCurrentFromResultSet(ResultSet rs,
            List<Column> columns, Model model, Serializable[] returnId,
            int[] returnPos) throws SQLException {
        Serializable id = null;
        Serializable value = null;
        int i = 0;
        for (Column column : columns) {
            i++;
            String key = column.getKey();
            Serializable v = column.getFromResultSet(rs, i);
            if (key.equals(model.MAIN_KEY)) {
                id = v;
            } else if (key.equals(model.COLL_TABLE_POS_KEY)) {
                // (the pos column is ignored, results are already ordered by id
                // then pos)
            } else if (key.equals(model.COLL_TABLE_VALUE_KEY)) {
                value = v;
            } else {
                throw new RuntimeException(key);
            }
        }
        Serializable prevId = returnId[0];
        returnId[0] = id;
        int pos = (id != null && !id.equals(prevId)) ? 0 : returnPos[0] + 1;
        returnPos[0] = pos;
        return value;
    }

    @Override
    public void executeInserts(PreparedStatement ps, List<Row> rows,
            List<Column> columns, boolean supportsBatchUpdates, String sql,
            JDBCMapperLogger logger) throws SQLException {
        List<Serializable> debugValues = logger.isLogEnabled() ? new ArrayList<Serializable>()
                : null;
        String loggedSql = supportsBatchUpdates && rows.size() > 1 ? sql
                + " -- BATCHED" : sql;
        int batch = 0;
        for (Row row : rows) {
            batch++;
            Serializable id = row.id;
            Serializable[] array = row.values;
            for (int i = 0; i < array.length; i++) {
                int n = 0;
                for (Column column : columns) {
                    n++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(Model.MAIN_KEY)) {
                        v = id;
                    } else if (key.equals(Model.COLL_TABLE_POS_KEY)) {
                        v = Long.valueOf((long) i);
                    } else if (key.equals(Model.COLL_TABLE_VALUE_KEY)) {
                        v = array[i];
                    } else {
                        throw new RuntimeException(key);
                    }
                    column.setToPreparedStatement(ps, n, v);
                    if (debugValues != null) {
                        debugValues.add(v);
                    }
                }
                if (debugValues != null) {
                    logger.logSQL(loggedSql, debugValues);
                    debugValues.clear();
                }
                if (supportsBatchUpdates) {
                    ps.addBatch();
                    if (batch % JDBCRowMapper.UPDATE_BATCH_SIZE == 0) {
                        ps.executeBatch();
                    }
                } else {
                    ps.execute();
                }
            }
        }
        if (supportsBatchUpdates) {
            ps.executeBatch();
        }
    }

}
