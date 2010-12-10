/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.api.CloseListener;
import org.h2.tools.SimpleResultSet;

/**
 * Functions used as stored procedures for H2.
 *
 * @author Florent Guillaume
 */
public class H2Functions extends EmbeddedFunctions {

    private static final Log log = LogFactory.getLog(H2Functions.class);

    // for debug
    private static boolean isLogEnabled() {
        return false;
        // return log.isTraceEnabled();
    }

    // for debug
    private static void logDebug(String message, Object... args) {
        log.trace("SQL: " + String.format(message.replace("?", "%s"), args));
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

    protected static String getLocalReadAcl(Connection conn, String id,
            String userSeparator) throws SQLException {
        String sql = "SELECT \"GRANT\", \"USER\"" //
                + " FROM ACLS" //
                + " WHERE" //
                + "   \"ID\" = '"
                + id
                + "'"//
                + "   AND \"PERMISSION\" IN (SELECT permission FROM read_acl_permissions)"
                + " ORDER BY \"POS\"";
        if (isLogEnabled()) {
            logDebug(sql);
        }
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        StringBuffer readAcl = new StringBuffer();
        try {
            while (rs.next()) {
                Boolean grant = rs.getBoolean(1);
                String op;
                if (grant) {
                    op = rs.getString(2);
                } else {
                    op = '-' + rs.getString(2);
                }
                if (readAcl.length() == 0) {
                    readAcl.append(op);
                } else {
                    readAcl.append(userSeparator + op);
                }
            }
        } finally {
            ps.close();
        }
        return readAcl.toString();
    }

    public static String getReadAcl(Connection conn, String id,
            String userSeparator) throws SQLException {
        StringBuffer readAcl = new StringBuffer();

        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            ps1 = conn.prepareStatement("SELECT PARENTID FROM HIERARCHY WHERE ID = ?;");
            boolean first = true;
            do {
                // local acl
                String localReadAcl = getLocalReadAcl(conn, id, userSeparator);
                if (localReadAcl != null && (localReadAcl.length() > 0)) {
                    if (readAcl.length() == 0) {
                        readAcl.append(localReadAcl);
                    } else {
                        readAcl.append(userSeparator + localReadAcl);
                    }
                }
                // get parent
                ps1.setObject(1, id);
                ResultSet rs = ps1.executeQuery();
                String newId;
                if (rs.next()) {
                    newId = (String) rs.getObject(1);
                    if (rs.wasNull()) {
                        newId = null;
                    }
                } else {
                    // no such id
                    newId = null;
                }
                if (first && newId == null) {
                    // there is no parent for the first level
                    // we may have a version on our hands, find the live doc
                    ps2 = conn.prepareStatement("SELECT VERSIONABLEID FROM VERSIONS WHERE ID = ?;");
                    ps2.setObject(1, id);
                    rs = ps2.executeQuery();
                    if (rs.next()) {
                        newId = (String) rs.getObject(1);
                        if (rs.wasNull()) {
                            newId = null;
                        }
                    } else {
                        // no such id
                        newId = null;
                    }
                }
                first = false;
                id = newId;
            } while (id != null);
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
        }
        return readAcl.toString();
    }

    public static ResultSet getReadAclsFor(Connection conn, String principals,
            String userSeparator) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        SimpleResultSet result = new SimpleResultSet();
        result.addColumn("ID", Types.VARCHAR, 0, 0); // String id
        if (meta.getURL().startsWith("jdbc:columnlist:")) {
            // this is just to query the result set columns
            return result;
        }
        if (principals == null) {
            return result;
        }
        Set<String> principalsList = split(principals);
        Set<String> blackList = new HashSet<String>();
        for (String user : principalsList) {
            blackList.add('-' + user);
        }
        // log.debug("getReadAclFor " + principals);
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement("SELECT \"ID\", \"ACL\" FROM READ_ACLS");
            rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString(1);
                String[] acl = rs.getString(2).split(userSeparator);
                for (String ace : acl) {
                    // log.debug("ace: " + ace);
                    if (principalsList.contains(ace)) {
                        result.addRow(new Object[] { id });
                        // log.debug("allowed: " + id);
                    } else if (blackList.contains(ace)) {
                        // log.debug("deny: " + id);
                        break;
                    }
                }
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
        return result;
    }

    public static class LogAclsModified implements org.h2.api.Trigger,
            CloseListener {

        private final String idName = "ID";

        private int idIndex;

        private int idType;

        /**
         * Trigger interface: initialization.
         */
        @Override
        public void init(Connection conn, String schema, String triggerName,
                String table, boolean before, int opType) throws SQLException {
            ResultSet rs = null;
            try {
                DatabaseMetaData meta = conn.getMetaData();
                rs = meta.getColumns(null, schema, table, idName);
                if (!rs.next()) {
                    throw new SQLException("No id key for: " + schema + '.'
                            + table);
                }
                idType = rs.getInt("DATA_TYPE");
                idIndex = rs.getInt("ORDINAL_POSITION") - 1;
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        }

        /**
         * Trigger interface.
         */
        @Override
        public void fire(Connection conn, Object[] oldRow, Object[] newRow)
                throws SQLException {
            String id = null;
            if (oldRow != null) {
                // update or delete
                id = oldRow[idIndex].toString();
            } else if (newRow != null) {
                // insert
                id = newRow[idIndex].toString();
            } else {
                return;
            }
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement("INSERT INTO hierarchy_modified_acl VALUES(?, ?);");
                ps.setString(1, id);
                ps.setString(2, "f");
                ps.executeUpdate();
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        }

        @Override
        public void close() throws SQLException {
        }

        @Override
        public void remove() {
        }

    }

    public static class LogHierarchyModified implements org.h2.api.Trigger,
            CloseListener {
        private final String idName = "ID";

        private int idIndex;

        private int idType;

        private final String parentIdName = "PARENTID";

        private int parentIdIndex;

        private final String isPropertyName = "ISPROPERTY";

        private int isPropertyIndex;

        /**
         * Trigger interface: initialization.
         */
        @Override
        public void init(Connection conn, String schema, String triggerName,
                String table, boolean before, int opType) throws SQLException {
            ResultSet rs = null;
            try {
                DatabaseMetaData meta = conn.getMetaData();
                rs = meta.getColumns(null, schema, table, idName);
                if (!rs.next()) {
                    throw new SQLException("No id key for: " + schema + '.'
                            + table);
                }
                idType = rs.getInt("DATA_TYPE");
                idIndex = rs.getInt("ORDINAL_POSITION") - 1;
                rs.close();
                rs = meta.getColumns(null, schema, table, parentIdName);
                if (!rs.next()) {
                    throw new SQLException("No parentid in " + schema + '.'
                            + table);
                }
                parentIdIndex = rs.getInt("ORDINAL_POSITION") - 1;
                rs.close();
                rs = meta.getColumns(null, schema, table, isPropertyName);
                if (!rs.next()) {
                    throw new SQLException("No isproperty in " + schema + '.'
                            + table);
                }
                isPropertyIndex = rs.getInt("ORDINAL_POSITION") - 1;
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }

        }

        /**
         * Trigger interface.
         */
        @Override
        public void fire(Connection conn, Object[] oldRow, Object[] newRow)
                throws SQLException {
            if (newRow != null && !((Boolean) newRow[isPropertyIndex])) {
                PreparedStatement ps = null;
                try {
                    ps = conn.prepareStatement("INSERT INTO hierarchy_modified_acl VALUES(?, ?);");
                    ps.setString(1, newRow[idIndex].toString());
                    if (oldRow == null) {
                        // Insert
                        ps.setString(2, "t");
                        ps.executeUpdate();
                    } else if (oldRow[parentIdIndex] != newRow[parentIdIndex]) {
                        // Update with new parent
                        ps.setString(2, "f");
                        ps.executeUpdate();
                    }
                } finally {
                    if (ps != null) {
                        ps.close();
                    }
                }
            }
        }

        @Override
        public void close() throws SQLException {
        }

        @Override
        public void remove() {
        }

    }

    public static ResultSet rebuildReadAcls(Connection conn,
            String userSeparator) throws SQLException {
        SimpleResultSet result = new SimpleResultSet();
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("TRUNCATE TABLE hierarchy_read_acl;");
            ps.executeUpdate();
            ps = conn.prepareStatement(String.format(
                    "INSERT INTO hierarchy_read_acl" //
                            + "  SELECT id, nx_get_read_acl(id, '%s')"
                            + "  FROM (SELECT id FROM hierarchy WHERE NOT isproperty) AS uids;",
                    userSeparator));
            ps.executeUpdate();
            ps = conn.prepareStatement("TRUNCATE TABLE read_acls;");
            ps.executeUpdate();
            // TODO: use md5 of the acl as key
            ps = conn.prepareStatement(String.format(
                    "INSERT INTO read_acls" //
                            + "  SELECT acl, acl" //
                            + "  FROM (SELECT DISTINCT(nx_get_read_acl(id, '%s')) AS acl" //
                            + "        FROM  (SELECT DISTINCT(id) AS id" //
                            + "               FROM acls) AS uids) AS read_acls_input;",
                    userSeparator));
            ps.executeUpdate();
            ps = conn.prepareStatement("TRUNCATE TABLE hierarchy_modified_acl;");
            ps.executeUpdate();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
        return result;
    }

    public static ResultSet updateReadAcls(Connection conn,
            String usersSeparator) throws SQLException {
        SimpleResultSet result = new SimpleResultSet();
        PreparedStatement ps = null;
        try {
            // New hierarchy_read_acl entry
            ps = conn.prepareStatement(String.format(
                    "INSERT INTO hierarchy_read_acl" //
                            + " SELECT id, nx_get_read_acl(id , '%s')" //
                            + " FROM (SELECT DISTINCT(id) AS id" //
                            + "   FROM hierarchy_modified_acl" //
                            + "   WHERE is_new AND" //
                            + "     EXISTS (SELECT 1 FROM hierarchy WHERE hierarchy_modified_acl.id=hierarchy.id)) AS uids;",
                    usersSeparator));
            int rowCount = ps.executeUpdate();
            ps = conn.prepareStatement("DELETE FROM hierarchy_modified_acl WHERE is_new;");
            ps.executeUpdate();
            // Update hierarchy_read_acl entry
            // Mark acl that need to be updated (set to NULL)
            ps = conn.prepareStatement("UPDATE hierarchy_read_acl SET acl_id = NULL WHERE id IN (" //
                    + " SELECT DISTINCT(id) AS id FROM hierarchy_modified_acl WHERE NOT is_new);");
            rowCount = ps.executeUpdate();
            ps = conn.prepareStatement("DELETE FROM hierarchy_modified_acl WHERE NOT is_new;");
            ps.executeUpdate();
            if (rowCount > 0) {
                ps = conn.prepareStatement("TRUNCATE TABLE read_acls;");
                ps.executeUpdate();
                // TODO: use md5 of the acl as key
                ps = conn.prepareStatement(String.format(
                        "INSERT INTO read_acls" //
                                + " SELECT acl, acl" //
                                + " FROM (SELECT DISTINCT(nx_get_read_acl(id, '%s')) AS acl" //
                                + "       FROM  (SELECT DISTINCT(id) AS id" //
                                + "              FROM acls) AS uids) AS read_acls_input;",
                        usersSeparator));
                ps.executeUpdate();
            }
            ps = conn.prepareStatement("UPDATE hierarchy_read_acl SET acl_id = NULL WHERE id IN (" //
                    + " SELECT h.id" //
                    + " FROM hierarchy AS h" //
                    + " JOIN hierarchy_read_acl AS r ON h.id = r.id" //
                    + " WHERE r.acl_id IS NOT NULL" //
                    + " AND h.parentid IN (SELECT id FROM hierarchy_read_acl WHERE acl_id IS NULL));");
            // Mark all childrens
            do {
                rowCount = ps.executeUpdate();
            } while (rowCount > 0);
            // Update hierarchy_read_acl acl_ids
            // TODO use the md5 for acl_id
            ps = conn.prepareStatement(String.format(
                    "UPDATE hierarchy_read_acl SET acl_id = nx_get_read_acl(id, '%s') WHERE acl_id IS NULL;",
                    usersSeparator));
            ps.executeUpdate();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }

        return result;
    }

    public static ResultSet upgradeVersions(Connection conn) throws SQLException {
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

    public static ResultSet upgradeLastContributor(Connection conn) throws SQLException {
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
}
