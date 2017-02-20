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
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Collection IO for arrays of ACLs.
 */
public class ACLCollectionIO implements CollectionIO {

    /** Whether we always write all the row elements in a RowUpdate or just the values starting from pos. */
    protected final boolean insertAll;

    public ACLCollectionIO(boolean insertAll) {
        this.insertAll = insertAll;
    }

    @Override
    public ACLRow getCurrentFromResultSet(ResultSet rs, List<Column> columns, Model model, Serializable[] returnId,
            int[] returnPos) throws SQLException {
        Serializable id = null;
        String name = null;
        boolean grant = false;
        String permission = null;
        String creator = null;
        Calendar begin = null;
        Calendar end = null;
        String user = null;
        String group = null;
        Long status = null;
        int i = 0;
        for (Column column : columns) {
            i++;
            String key = column.getKey();
            Serializable v = column.getFromResultSet(rs, i);
            switch (key) {
            case Model.MAIN_KEY:
                id = v;
                break;
            case Model.ACL_NAME_KEY:
                name = (String) v;
                break;
            case Model.ACL_GRANT_KEY:
                grant = v == null ? false : (Boolean) v;
                break;
            case Model.ACL_PERMISSION_KEY:
                permission = (String) v;
                break;
            case Model.ACL_CREATOR_KEY:
                creator = (String) v;
                break;
            case Model.ACL_BEGIN_KEY:
                begin = (Calendar) v;
                break;
            case Model.ACL_END_KEY:
                end = (Calendar) v;
                break;
            case Model.ACL_USER_KEY:
                user = (String) v;
                break;
            case Model.ACL_GROUP_KEY:
                group = (String) v;
                break;
            case Model.ACL_STATUS_KEY:
                status = (Long) v;
                break;
            case Model.ACL_POS_KEY:
                // ignore, query already sorts by pos
                break;
            default:
                throw new RuntimeException(key);
            }
        }
        Serializable prevId = returnId[0];
        returnId[0] = id;
        int pos = (id != null && !id.equals(prevId)) ? 0 : returnPos[0] + 1;
        returnPos[0] = pos;
        return new ACLRow(pos, name, grant, permission, user, group, creator, begin, end, status);
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
                ACLRow acl = (ACLRow) array[i];
                int n = 0;
                for (Column column : columns) {
                    n++;
                    String key = column.getKey();
                    Serializable v;
                    switch (key) {
                    case Model.MAIN_KEY:
                        v = id;
                        break;
                    case Model.ACL_POS_KEY:
                        v = (long) acl.pos;
                        break;
                    case Model.ACL_NAME_KEY:
                        v = acl.name;
                        break;
                    case Model.ACL_GRANT_KEY:
                        v = acl.grant;
                        break;
                    case Model.ACL_PERMISSION_KEY:
                        v = acl.permission;
                        break;
                    case Model.ACL_CREATOR_KEY:
                        v = acl.creator;
                        break;
                    case Model.ACL_BEGIN_KEY:
                        v = acl.begin;
                        break;
                    case Model.ACL_END_KEY:
                        v = acl.end;
                        break;
                    case Model.ACL_STATUS_KEY:
                        v = acl.status;
                        break;
                    case Model.ACL_USER_KEY:
                        v = acl.user;
                        break;
                    case Model.ACL_GROUP_KEY:
                        v = acl.group;
                        break;
                    default:
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
