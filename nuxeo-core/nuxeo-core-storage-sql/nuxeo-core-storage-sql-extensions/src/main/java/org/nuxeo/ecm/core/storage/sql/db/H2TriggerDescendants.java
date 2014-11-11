/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.api.CloseListener;
import org.h2.api.Trigger;

/**
 * H2 trigger updating the DESCENDANTS table when the HIERARCHY table changes.
 *
 * @author Florent Guillaume
 */
public class H2TriggerDescendants implements Trigger, CloseListener {

    private static final Log log = LogFactory.getLog(H2TriggerDescendants.class);

    int idIndex;

    int parentIdIndex;

    int isPropIndex;

    // for debug
    private static boolean isLogEnabled() {
        return false;
        // return log.isTraceEnabled();
    }

    // for debug
    private static void logDebug(String message, Object... args) {
        log.trace("SQL: " + String.format(message.replace("?", "'%s'"), args));
    }

    public void init(Connection conn, String schemaName, String triggerName,
            String tableName, boolean before, int type) throws SQLException {

        String HIER_TABLE = "HIERARCHY"; // TODO make parameter
        String SCHEMA = "PUBLIC";
        String MAIN_ID = "ID";
        String PARENT_ID = "PARENTID";
        String IS_PROPERTY = "ISPROPERTY";

        // find id and parentid column indices in result sets
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getColumns(null, SCHEMA, HIER_TABLE, null);
        while (rs.next()) {
            String name = rs.getString("COLUMN_NAME").toUpperCase();
            int index = rs.getInt("ORDINAL_POSITION") - 1;
            if (name.equals(MAIN_ID)) {
                idIndex = index;
            } else if (name.equals(PARENT_ID)) {
                parentIdIndex = index;
            } else if (name.equals(IS_PROPERTY)) {
                isPropIndex = index;
            }
        }
        rs.close();
    }

    /**
     * Trigger interface.
     */
    public void fire(Connection conn, Object[] oldRow, Object[] newRow)
            throws SQLException {
        if (oldRow != null) {
            if (newRow != null) {
                // update
                if (!Boolean.FALSE.equals((Boolean) newRow[isPropIndex])) {
                    return;
                }
                String id = (String) oldRow[idIndex];
                if (newRow[idIndex] != id) {
                    throw new SQLException("Cannot change a node's id: " + id);
                }
                String oldParentId = (String) oldRow[parentIdIndex];
                String newParentId = (String) newRow[parentIdIndex];
                if (oldParentId == newParentId) {
                    // no parent/child relationship changed
                    return;
                }
                update(conn, id, oldParentId, newParentId);
            } else {
                // delete
                // ON DELETE CASCADE will already have removed the rows
            }
        } else if (newRow != null) {
            // insert
            if (!Boolean.FALSE.equals((Boolean) newRow[isPropIndex])) {
                return;
            }
            String parentId = (String) newRow[parentIdIndex];
            String id = (String) newRow[idIndex];
            insert(conn, id, parentId);
        }
        Boolean.valueOf(true);
    }

    private void insert(Connection conn, String id, String parentId)
            throws SQLException {
        if (parentId == null) {
            // root or version, not a descendant of anything
            return;
        }
        if (id == null) {
            throw new SQLException("Cannot have a null id");
        }
        // the parent's ancestors gain a new descendant
        String sql = "INSERT INTO DESCENDANTS (ID, DESCENDANTID)"
                + " SELECT ID, ? FROM DESCENDANTS WHERE DESCENDANTID = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, id);
        ps.setString(2, parentId);
        if (isLogEnabled()) {
            logDebug(sql, id, parentId);
        }
        ps.execute();
        ps.close();
        // the parent gains a new descendant
        sql = "INSERT INTO DESCENDANTS (ID, DESCENDANTID) VALUES (?, ?)";
        ps = conn.prepareStatement(sql);
        ps.setString(1, parentId);
        ps.setString(2, id);
        if (isLogEnabled()) {
            logDebug(sql, parentId, id);
        }
        ps.execute();
        ps.close();
    }

    private void update(Connection conn, String id, String oldParentId,
            String newParentId) throws SQLException {
        String sql;
        PreparedStatement ps;
        if (oldParentId != null) {
            if (newParentId != null) {
                // check we're not moving under ourselves
                if (newParentId == id) {
                    throw new SQLException("Cannot move a node under itself");
                }
                sql = "SELECT 1 FROM DESCENDANTS WHERE ID = ? AND DESCENDANTID = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, id);
                ps.setString(2, newParentId);
                if (isLogEnabled()) {
                    logDebug(sql, id, newParentId);
                }
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException(
                            "Cannot move a node under one of its descendants");
                }
                ps.close();
            }
            // the old parent and its ancestors lose some descendants
            sql = "DELETE FROM DESCENDANTS" //
                    + " WHERE ID IN (SELECT ID FROM DESCENDANTS WHERE DESCENDANTID = ?)" //
                    + " AND DESCENDANTID IN (SELECT DESCENDANTID FROM DESCENDANTS WHERE ID = ?" //
                    + /* --------------- */" UNION ALL SELECT ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ps.setString(2, id);
            ps.setString(3, id);
            if (isLogEnabled()) {
                logDebug(sql, id, id, id);
            }
            ps.execute();
            ps.close();
        }
        if (newParentId != null) {
            // the new parent's ancestors gain as descendants the descendants of
            // the moved node (cross join)
            sql = "INSERT INTO DESCENDANTS (ID, DESCENDANTID)" //
                    + " (SELECT A.ID, B.DESCENDANTID FROM DESCENDANTS A JOIN DESCENDANTS B" //
                    + " WHERE A.DESCENDANTID = ? AND B.ID = ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, newParentId);
            ps.setString(2, id);
            if (isLogEnabled()) {
                logDebug(sql, newParentId, id);
            }
            ps.execute();
            ps.close();
            // the new parent's ancestors gain as descendant the moved node
            sql = "INSERT INTO DESCENDANTS (ID, DESCENDANTID)" //
                    + " SELECT ID, ? FROM DESCENDANTS WHERE DESCENDANTID = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ps.setString(2, newParentId);
            if (isLogEnabled()) {
                logDebug(sql, id, newParentId);
            }
            ps.execute();
            ps.close();
            // the new parent gains as descendants the descendants of the
            // moved node
            sql = "INSERT INTO DESCENDANTS (ID, DESCENDANTID)" //
                    + " SELECT ?, DESCENDANTID FROM DESCENDANTS WHERE ID = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, newParentId);
            ps.setString(2, id);
            if (isLogEnabled()) {
                logDebug(sql, newParentId, id);
            }
            ps.execute();
            ps.close();
            // the new parent gains as descendant the moved node
            sql = "INSERT INTO DESCENDANTS (ID, DESCENDANTID)" //
                    + " VALUES (?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, newParentId);
            ps.setString(2, id);
            if (isLogEnabled()) {
                logDebug(sql, newParentId, id);
            }
            ps.execute();
            ps.close();
        }
    }

    public void close() {
        // nothing
    }

    public void remove() {
        // nothing
    }
}
