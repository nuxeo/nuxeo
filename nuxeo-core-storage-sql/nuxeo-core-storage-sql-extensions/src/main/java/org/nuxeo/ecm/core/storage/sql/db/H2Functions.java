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

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.h2.tools.SimpleResultSet;

/**
 * Functions used as stored procedures for H2.
 *
 * @author Florent Guillaume
 */
public class H2Functions extends EmbeddedFunctions {

    // for debug
    private static boolean isLogEnabled() {
        return false;
        // return log.isTraceEnabled();
    }

    // for debug
    private static void logDebug(String message, Object... args) {
        // log.trace("SQL: " + String.format(message.replace("?", "%s"), args));
    }

    public static boolean isInTreeString(Connection conn, String id,
            String baseId) throws SQLException {
        return isInTree(conn, id, baseId);
    }

    public static boolean isInTreeLong(Connection conn, Long id, Long baseId)
            throws SQLException {
        return isInTree(conn, id, baseId);
    }

    public static boolean isAccessAllowedString(Connection conn, String id,
            String principals, String permissions) throws SQLException {
        return isAccessAllowed(conn, id, split(principals), split(permissions));
    }

    public static boolean isAccessAllowedLong(Connection conn, Long id,
            String principals, String permissions) throws SQLException {
        return isAccessAllowed(conn, id, split(principals), split(permissions));
    }

    /**
     * Adds an invalidation from this cluster node to the invalidations list.
     */
    @SuppressWarnings("boxing")
    public static void clusterInvalidateString(Connection conn, String id,
            String fragments, int kind) throws SQLException {
        PreparedStatement ps = null;
        try {
            // find other node ids
            String sql = "SELECT \"NODEID\" FROM \"CLUSTER_NODES\" "
                    + "WHERE \"NODEID\" <> SESSION_ID()";
            if (isLogEnabled()) {
                logDebug(sql);
            }
            ps = conn.prepareStatement(sql);
            List<Long> nodeIds = new LinkedList<Long>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodeIds.add(rs.getLong(1));
            }
            if (isLogEnabled()) {
                logDebug("  -> " + nodeIds);
            }
            // invalidate
            sql = "INSERT INTO \"CLUSTER_INVALS\" "
                    + "(\"NODEID\", \"ID\", \"FRAGMENTS\", \"KIND\") "
                    + "VALUES (?, ?, ?, ?)";
            for (Long nodeId : nodeIds) {
                if (isLogEnabled()) {
                    logDebug(sql, nodeId, id, fragments, kind);
                }
                ps = conn.prepareStatement(sql);
                ps.setLong(1, nodeId);
                ps.setObject(2, id);
                ps.setString(3, fragments);
                ps.setInt(4, kind);
                ps.execute();
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /**
     * Gets the invalidations for this cluster node.
     *
     * @return a result set with columns id, fragments, kind
     */
    public static ResultSet getClusterInvalidationsString(Connection conn)
            throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        SimpleResultSet result = new SimpleResultSet();
        result.addColumn("ID", Types.VARCHAR, 0, 0); // String id
        result.addColumn("FRAGMENTS", Types.VARCHAR, 0, 0);
        result.addColumn("KIND", Types.INTEGER, 0, 0);
        if (meta.getURL().startsWith("jdbc:columnlist:")) {
            // this is just to query the result set columns
            return result;
        }

        PreparedStatement ps = null;
        Statement st = null;
        try {
            String sql = "SELECT \"ID\", \"FRAGMENTS\", \"KIND\" FROM \"CLUSTER_INVALS\" "
                    + "WHERE \"NODEID\" = SESSION_ID()";
            if (isLogEnabled()) {
                logDebug(sql);
            }
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            List<Serializable> debugValues = null;
            if (isLogEnabled()) {
                debugValues = new LinkedList<Serializable>();
            }
            while (rs.next()) {
                String id = rs.getString(1);
                String fragments = rs.getString(2);
                long kind = rs.getLong(3);
                result.addRow(new Object[] { id, fragments, Long.valueOf(kind) });
                if (debugValues != null) {
                    debugValues.add(id + ',' + fragments + ',' + kind);
                }
            }
            if (debugValues != null) {
                logDebug("  -> " + debugValues);
            }

            // remove processed invalidations
            sql = "DELETE FROM \"CLUSTER_INVALS\" WHERE \"NODEID\" = SESSION_ID()";
            if (isLogEnabled()) {
                logDebug(sql);
            }
            st = conn.createStatement();
            st.execute(sql);

            // return invalidations
            return result;
        } finally {
            if (ps != null) {
                ps.close();
            }
            if (st != null) {
                st.close();
            }
        }
    }

    public static ResultSet upgradeVersions(Connection conn)
            throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            String sql = "SELECT v.id, v.versionableid, h.majorversion, h.minorversion"
                    + "  FROM versions v JOIN hierarchy h ON v.id = h.id"
                    + "  ORDER BY v.versionableid, v.created DESC";
            ps1 = conn.prepareStatement(sql);
            ResultSet rs = ps1.executeQuery();
            String series = null;
            boolean isLatest = false;
            boolean isLatestMajor = false;
            while (rs.next()) {
                String id = rs.getString("id");
                String vid = rs.getString("versionableid");
                long maj = rs.getLong("majorversion");
                long min = rs.getLong("minorversion");
                if (vid == null || !vid.equals(series)) {
                    // restart
                    isLatest = true;
                    isLatestMajor = true;
                    series = vid;
                }
                boolean isMajor = min == 0;
                ps2 = conn.prepareStatement("UPDATE versions SET label = ?, islatest = ?, islatestmajor = ?"
                        + " WHERE id = ?");
                ps2.setString(1, maj + "." + min);
                ps2.setBoolean(2, isLatest);
                ps2.setBoolean(3, isMajor && isLatestMajor);
                ps2.setString(4, id);
                ps2.executeUpdate();
                // next
                isLatest = false;
                if (isMajor) {
                    isLatestMajor = false;
                }
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
        }

        return new SimpleResultSet();
    }

    public static ResultSet upgradeLastContributor(Connection conn)
            throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            String sql = "SELECT dc_c.id, dc_c.item"
                    + "  FROM dublincore dc"
                    + "    JOIN (SELECT id, max(pos) AS pos FROM dc_contributors GROUP BY id) AS tmp ON (dc.id = tmp.id)"
                    + "    JOIN dc_contributors dc_c ON (tmp.id = dc_c.id AND tmp.pos = dc_c.pos)"
                    + "  WHERE dc.lastContributor IS NULL;";
            ps1 = conn.prepareStatement(sql);
            ResultSet rs = ps1.executeQuery();
            String series = null;
            while (rs.next()) {
                String id = rs.getString("id");
                String lastContributor = rs.getString("item");

                ps2 = conn.prepareStatement("UPDATE dublincore SET lastContributor = ? WHERE id = ?");
                ps2.setString(1, lastContributor);
                ps2.setString(2, id);

                ps2.executeUpdate();
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
        }

        return new SimpleResultSet();
    }

    public static ResultSet getAncestorsIds(Connection conn, String idsString)
            throws SQLException {
        Set<String> ids = split(idsString);
        DatabaseMetaData meta = conn.getMetaData();
        SimpleResultSet result = new SimpleResultSet();
        result.addColumn("ID", Types.VARCHAR, 0, 0); // String id
        if (meta.getURL().startsWith("jdbc:columnlist:")) {
            // this is just to query the result set columns
            return result;
        }

        PreparedStatement ps = null;
        try {
            LinkedList<String> todo = new LinkedList<String>(ids);
            Set<String> done = new HashSet<String>();
            Set<String> res = new HashSet<String>();
            while (!todo.isEmpty()) {
                done.addAll(todo);
                String sql = getSelectParentIdsByIdsSql(todo.size());
                if (isLogEnabled()) {
                    logDebug(sql, todo);
                }
                ps = conn.prepareStatement(sql);
                int i = 1;
                for (String id : todo) {
                    ps.setString(i++, id);
                }
                todo = new LinkedList<String>();
                List<String> debugIds = null;
                if (isLogEnabled()) {
                    debugIds = new LinkedList<String>();
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String id = rs.getString(1);
                    if (id != null) {
                        if (!res.contains(id)) {
                            res.add(id);
                            result.addRow(new Object[] { id });
                        }
                        if (!done.contains(id)) {
                            todo.add(id);
                        }
                        if (isLogEnabled()) {
                            debugIds.add(id);
                        }
                    }
                }
                if (isLogEnabled()) {
                    logDebug("  -> " + debugIds);
                }
                ps.close();
                ps = null;
            }
            return result;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    protected static String getSelectParentIdsByIdsSql(int size) {
        StringBuilder buf = new StringBuilder(
                "SELECT DISTINCT \"PARENTID\" FROM \"HIERARCHY\" WHERE \"ID\" IN (");
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buf.append(", ");
            }
            buf.append('?');
        }
        buf.append(')');
        return buf.toString();
    }

}
