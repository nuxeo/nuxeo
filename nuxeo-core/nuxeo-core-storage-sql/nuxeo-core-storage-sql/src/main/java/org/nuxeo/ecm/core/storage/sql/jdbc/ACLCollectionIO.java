/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Collection IO for arrays of ACLs.
 */
public class ACLCollectionIO implements CollectionIO {

    public static final CollectionIO INSTANCE = new ACLCollectionIO();

    @Override
    public ACLRow getCurrentFromResultSet(ResultSet rs, List<Column> columns,
            Model model, Serializable[] returnId, int[] returnPos)
            throws SQLException {
        Serializable id = null;
        String name = null;
        boolean grant = false;
        String permission = null;
        String user = null;
        String group = null;
        int i = 0;
        for (Column column : columns) {
            i++;
            String key = column.getKey();
            Serializable v = column.getFromResultSet(rs, i);
            if (key.equals(model.MAIN_KEY)) {
                id = v;
            } else if (key.equals(model.ACL_NAME_KEY)) {
                name = (String) v;
            } else if (key.equals(model.ACL_GRANT_KEY)) {
                grant = v == null ? false : (Boolean) v;
            } else if (key.equals(model.ACL_PERMISSION_KEY)) {
                permission = (String) v;
            } else if (key.equals(model.ACL_USER_KEY)) {
                user = (String) v;
            } else if (key.equals(model.ACL_GROUP_KEY)) {
                group = (String) v;
            } else if (key.equals(model.ACL_POS_KEY)) {
                // ignore, query already sorts by pos
            } else {
                throw new RuntimeException(key);
            }
        }
        Serializable prevId = returnId[0];
        returnId[0] = id;
        int pos = (id != null && !id.equals(prevId)) ? 0 : returnPos[0] + 1;
        returnPos[0] = pos;
        return new ACLRow(pos, name, grant, permission, user, group);
    }

    @Override
    public void executeInserts(PreparedStatement ps, List<Row> rows,
            List<Column> columns, boolean supportsBatchUpdates, String sql,
            JDBCConnection connection) throws SQLException {
        List<Serializable> debugValues = connection.logger.isLogEnabled() ? new ArrayList<Serializable>()
                : null;
        String loggedSql = supportsBatchUpdates ? sql + " -- BATCHED" : sql;
        int batch = 0;
        for (Row row : rows) {
            batch++;
            Serializable id = row.id;
            Serializable[] array = row.values;
            for (int i = 0; i < array.length; i++) {
                ACLRow acl = (ACLRow) array[i];
                int n = 0;
                for (Column column : columns) {
                    n++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(Model.MAIN_KEY)) {
                        v = id;
                    } else if (key.equals(Model.ACL_POS_KEY)) {
                        v = (long) acl.pos;
                    } else if (key.equals(Model.ACL_NAME_KEY)) {
                        v = acl.name;
                    } else if (key.equals(Model.ACL_GRANT_KEY)) {
                        v = acl.grant;
                    } else if (key.equals(Model.ACL_PERMISSION_KEY)) {
                        v = acl.permission;
                    } else if (key.equals(Model.ACL_USER_KEY)) {
                        v = acl.user;
                    } else if (key.equals(Model.ACL_GROUP_KEY)) {
                        v = acl.group;
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
                if (supportsBatchUpdates) {
                    ps.addBatch();
                    if (batch % JDBCRowMapper.UPDATE_BATCH_SIZE == 0) {
                        ps.executeBatch();
                        connection.countExecute();
                    }
                } else {
                    ps.execute();
                    connection.countExecute();
                }
            }
        }
        if (supportsBatchUpdates) {
            ps.executeBatch();
            connection.countExecute();
        }
    }

}
