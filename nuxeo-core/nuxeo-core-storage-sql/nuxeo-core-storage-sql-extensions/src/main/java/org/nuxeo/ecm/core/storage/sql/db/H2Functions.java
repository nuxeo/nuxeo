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

    public static boolean isInTreeString(Connection conn, String id, String baseId) throws SQLException {
        return isInTree(conn, id, baseId);
    }

    public static boolean isInTreeLong(Connection conn, Long id, Long baseId) throws SQLException {
        return isInTree(conn, id, baseId);
    }

    public static boolean isAccessAllowedString(Connection conn, String id, String principals, String permissions)
            throws SQLException {
        return isAccessAllowed(conn, id, split(principals), split(permissions));
    }

    public static boolean isAccessAllowedLong(Connection conn, Long id, String principals, String permissions)
            throws SQLException {
        return isAccessAllowed(conn, id, split(principals), split(permissions));
    }

    /**
     * Adds an invalidation from this cluster node to the invalidations list.
     */
    @SuppressWarnings("boxing")
    public static void clusterInvalidateString(Connection conn, long nodeId, String id, String fragments, int kind)
            throws SQLException {
        // find other node ids
        String sql = "SELECT \"NODEID\" FROM \"CLUSTER_NODES\" WHERE \"NODEID\" <> ?";
        if (isLogEnabled()) {
            logDebug(sql, nodeId);
        }
        List<Long> nodeIds = new LinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nodeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nodeIds.add(rs.getLong(1));
                }
            }
        }
        if (isLogEnabled()) {
            logDebug("  -> " + nodeIds);
        }
        // invalidate
        sql = "INSERT INTO \"CLUSTER_INVALS\" " + "(\"NODEID\", \"ID\", \"FRAGMENTS\", \"KIND\") "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Long nid : nodeIds) {
                if (isLogEnabled()) {
                    logDebug(sql, nid, id, fragments, kind);
                }
                ps.setLong(1, nid);
                ps.setObject(2, id);
                ps.setString(3, fragments);
                ps.setInt(4, kind);
                ps.execute();
            }
        }
    }

    /**
     * Gets the invalidations for this cluster node.
     *
     * @return a result set with columns id, fragments, kind
     */
    public static ResultSet getClusterInvalidationsString(Connection conn, long nodeId) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        SimpleResultSet result = new SimpleResultSet();
        result.addColumn("ID", Types.VARCHAR, 0, 0); // String id
        result.addColumn("FRAGMENTS", Types.VARCHAR, 0, 0);
        result.addColumn("KIND", Types.INTEGER, 0, 0);
        if (meta.getURL().startsWith("jdbc:columnlist:")) {
            // this is just to query the result set columns
            return result;
        }

        String sql = "SELECT \"ID\", \"FRAGMENTS\", \"KIND\" FROM \"CLUSTER_INVALS\" "
                + "WHERE \"NODEID\" = ?";
        if (isLogEnabled()) {
            logDebug(sql, nodeId);
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nodeId);
            List<Serializable> debugValues = null;
            if (isLogEnabled()) {
                debugValues = new LinkedList<>();
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    String fragments = rs.getString(2);
                    long kind = rs.getLong(3);
                    result.addRow(new Object[] { id, fragments, Long.valueOf(kind) });
                    if (debugValues != null) {
                        debugValues.add(id + ',' + fragments + ',' + kind);
                    }
                }
            }
            if (debugValues != null) {
                logDebug("  -> " + debugValues);
            }

            // remove processed invalidations
            sql = "DELETE FROM \"CLUSTER_INVALS\" WHERE \"NODEID\" = ?";
            if (isLogEnabled()) {
                logDebug(sql);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nodeId);
            ps.execute();
        }
        // return invalidations
        return result;
    }

    public static ResultSet upgradeVersions(Connection conn) throws SQLException {
        String sql = "SELECT v.id, v.versionableid, h.majorversion, h.minorversion"
                + "  FROM versions v JOIN hierarchy h ON v.id = h.id"
                + "  ORDER BY v.versionableid, v.created DESC";
        try (PreparedStatement ps1 = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps1.executeQuery()) {
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
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "UPDATE versions SET label = ?, islatest = ?, islatestmajor = ?" + " WHERE id = ?")) {
                        ps2.setString(1, maj + "." + min);
                        ps2.setBoolean(2, isLatest);
                        ps2.setBoolean(3, isMajor && isLatestMajor);
                        ps2.setString(4, id);
                        ps2.executeUpdate();
                    }
                    // next
                    isLatest = false;
                    if (isMajor) {
                        isLatestMajor = false;
                    }
                }
            }
        }

        return new SimpleResultSet();
    }

    public static ResultSet upgradeLastContributor(Connection conn) throws SQLException {
        String sql = "SELECT dc_c.id, dc_c.item"
                + "  FROM dublincore dc"
                + "    JOIN (SELECT id, max(pos) AS pos FROM dc_contributors GROUP BY id) AS tmp ON (dc.id = tmp.id)"
                + "    JOIN dc_contributors dc_c ON (tmp.id = dc_c.id AND tmp.pos = dc_c.pos)"
                + "  WHERE dc.lastContributor IS NULL;";
        try (PreparedStatement ps1 = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps1.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String lastContributor = rs.getString("item");
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "UPDATE dublincore SET lastContributor = ? WHERE id = ?")) {
                        ps2.setString(1, lastContributor);
                        ps2.setString(2, id);
                        ps2.executeUpdate();
                    }
                }
            }
        }

        return new SimpleResultSet();
    }

    public static ResultSet getAncestorsIds(Connection conn, String idsString) throws SQLException {
        Set<String> ids = split(idsString);
        DatabaseMetaData meta = conn.getMetaData();
        SimpleResultSet result = new SimpleResultSet();
        result.addColumn("ID", Types.VARCHAR, 0, 0); // String id
        if (meta.getURL().startsWith("jdbc:columnlist:")) {
            // this is just to query the result set columns
            return result;
        }

        LinkedList<String> todo = new LinkedList<String>(ids);
        Set<String> done = new HashSet<String>();
        Set<String> res = new HashSet<String>();
        List<String> debugIds = null;
        if (isLogEnabled()) {
            debugIds = new LinkedList<String>();
        }
        while (!todo.isEmpty()) {
            done.addAll(todo);
            String sql = getSelectParentIdsByIdsSql(todo.size());
            if (isLogEnabled()) {
                logDebug(sql, todo);
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                for (String id : todo) {
                    ps.setString(i++, id);
                }
                todo = new LinkedList<String>();
                try (ResultSet rs = ps.executeQuery()) {
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
                            if (debugIds != null) {
                                debugIds.add(id);
                            }
                        }
                    }
                }
            }
            if (isLogEnabled()) {
                logDebug("  -> " + debugIds);
            }
        }
        return result;
    }

    protected static String getSelectParentIdsByIdsSql(int size) {
        StringBuilder buf = new StringBuilder("SELECT DISTINCT \"PARENTID\" FROM \"HIERARCHY\" WHERE \"ID\" IN (");
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
