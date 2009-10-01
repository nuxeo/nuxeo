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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    /**
     * Rebuild the complete descendants tree.
     */

    public static void initDescendants(Connection conn) throws SQLException {
        String sql;
        LinkedList<String> todo = new LinkedList<String>();

        // find roots
        Statement s = conn.createStatement();
        sql = "SELECT ID FROM REPOSITORIES";
        logDebug(sql);
        ResultSet rs = s.executeQuery(sql);
        while (rs.next()) {
            String rootId = rs.getString(1);
            todo.add(rootId);
        }
        rs.close();
        log.trace("SQL:   -> " + todo);
        if (todo.size() == 0) {
            // db not yet initialized, ignore
            s.close();
            return;
        }

        // truncate table
        String table = "DESCENDANTS";
        sql = String.format("TRUNCATE TABLE %s", table);
        logDebug(sql);
        s.execute(sql);
        s.close();

        // traverse from roots
        Map<String, Set<String>> ancestors = new HashMap<String, Set<String>>();
        Map<String, Set<String>> descendants = new HashMap<String, Set<String>>();
        do {
            String p = todo.remove();
            sql = "SELECT ID FROM HIERARCHY WHERE PARENTID = ? AND ISPROPERTY = 0";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, p);
            // logDebug(sql, p);
            rs = ps.executeQuery();
            // for each child
            while (rs.next()) {
                String c = rs.getString(1);
                todo.add(c);
                // child's ancestors
                Set<String> cans = ancestors.get(c);
                if (cans == null) {
                    ancestors.put(c, cans = new HashSet<String>());
                }
                Set<String> pans = ancestors.get(p);
                if (pans != null) {
                    cans.addAll(pans);
                }
                cans.add(p);
                // all ancestors have it as descendant
                for (String pp : cans) {
                    Set<String> desc = descendants.get(pp);
                    if (desc == null) {
                        descendants.put(pp, desc = new HashSet<String>());
                    }
                    desc.add(c);
                }
            }
            ps.close();
        } while (!todo.isEmpty());

        // insert descendants into table
        sql = String.format("INSERT INTO %s (ID, DESCENDANTID) VALUES (?, ?)",
                table);
        PreparedStatement ps = conn.prepareStatement(sql);
        int n = 0;
        for (Entry<String, Set<String>> e : descendants.entrySet()) {
            String p = e.getKey();
            for (String c : e.getValue()) {
                ps.setString(1, p);
                ps.setString(2, c);
                // logDebug(sql, p, c);
                ps.execute();
                n++;
            }
        }
        logDebug(String.format("-- inserted %s rows into %s", Long.valueOf(n),
                table));
        ps.close();
    }

}
