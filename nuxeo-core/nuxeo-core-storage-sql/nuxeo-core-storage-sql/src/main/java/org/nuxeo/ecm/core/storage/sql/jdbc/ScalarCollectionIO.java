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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Collection IO for arrays of scalar values.
 */
public class ScalarCollectionIO implements CollectionIO {

    /** Whether we always insert all the rows in the row update or just the values starting from pos. */
    protected final boolean insertAll;

    public ScalarCollectionIO(boolean insertAll) {
        this.insertAll = insertAll;
    }

    @Override
    public Serializable getCurrentFromResultSet(ResultSet rs, List<Column> columns, Model model,
            Serializable[] returnId, int[] returnPos) throws SQLException {
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
    public void executeInserts(PreparedStatement ps, List<RowUpdate> rowus, List<Column> columns,
            boolean supportsBatchUpdates, String sql, JDBCConnection connection) throws SQLException {
        List<Serializable> debugValues = connection.logger.isLogEnabled() ? new ArrayList<Serializable>() : null;
        boolean batched = supportsBatchUpdates && rowus.size() > 1;
        String loggedSql = batched ? sql + " -- BATCHED" : sql;
        int batch = 0;
        for (Iterator<RowUpdate> rowIt = rowus.iterator(); rowIt.hasNext();) {
            RowUpdate rowu = rowIt.next();
            int start;
            if (rowu.pos == -1 || insertAll) {
                start = 0;
            } else {
                start = rowu.pos;
            }
            Serializable id = rowu.row.id;
            Serializable[] array = rowu.row.values;
            for (int i = start; i < array.length; i++) {
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
                    connection.logger.logSQL(loggedSql, debugValues);
                    debugValues.clear();
                }
                if (batched) {
                    ps.addBatch();
                    batch++;
                    if (batch % JDBCRowMapper.UPDATE_BATCH_SIZE == 0 || !rowIt.hasNext()) {
                        ps.executeBatch();
                        connection.countExecute();
                    }
                } else {
                    ps.execute();
                    connection.countExecute();
                }
            }
        }
    }

}
