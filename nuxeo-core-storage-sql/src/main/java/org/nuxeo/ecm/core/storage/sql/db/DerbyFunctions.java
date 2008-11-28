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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Functions used as stored procedure for Derby.
 *
 * @author Florent Guillaume
 */
public class DerbyFunctions {

    private static final Log log = LogFactory.getLog(DerbyFunctions.class);

    private static void logDebug(String message) {
        // log.debug(message);
    }

    /**
     * Checks if an id is a (strict) descendant of a given base id.
     *
     * @param id the id to check for
     * @param baseId the base id
     */
    protected static boolean isInTree(Serializable id, Serializable baseId)
            throws SQLException {
        if (baseId == null || id == null || baseId.equals(id)) {
            // containment check is strict
            return false;
        }
        Connection connection = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement("SELECT PARENTID FROM HIERARCHY WHERE ID = ?");
            do {
                ps.setObject(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    // no such id
                    return false;
                }
                if (id instanceof String) {
                    id = rs.getString(1);
                } else {
                    id = Long.valueOf(rs.getLong(1));
                }
                if (rs.wasNull()) {
                    id = null;
                }
                rs.close();
                if (baseId.equals(id)) {
                    // found a match
                    return true;
                }
            } while (id != null);
            // got to the root
            return false;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } finally {
                connection.close();
            }
        }
    }

    /** Bound to a Derby function. */
    public static short isInTreeString(String id, String baseId)
            throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    /** Bound to a Derby function. */
    public static short isInTreeLong(Long id, Long baseId) throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    // -----------------------------------------------------------------
    // -----------------------------------------------------------------

    /**
     * Checks if access to a document is allowed.
     * <p>
     * This implements in SQL the ACL-based security policy logic.
     *
     * @param id the id of the document
     * @param principals the allowed identities
     * @param permissions the allowed permissions
     */
    protected static boolean isAccessAllowed(Serializable id,
            Set<String> principals, Set<String> permissions)
            throws SQLException {
        if (log.isDebugEnabled()) {
            logDebug("isAccessAllowed " + id + " " + principals + " " +
                    permissions);
        }
        Connection connection = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            ps1 = connection.prepareStatement( //
            "SELECT \"GRANT\", \"PERMISSION\", \"USER\" FROM \"ACLS\" "
                    + "WHERE ID = ? ORDER BY POS");
            ps2 = connection.prepareStatement("SELECT PARENTID FROM HIERARCHY WHERE ID = ?");
            boolean first = true;
            do {
                /*
                 * Check permissions at this level.
                 */
                ps1.setObject(1, id);
                ResultSet rs = ps1.executeQuery();
                while (rs.next()) {
                    boolean grant = rs.getShort(1) != 0;
                    String permission = rs.getString(2);
                    String user = rs.getString(3);
                    if (log.isDebugEnabled()) {
                        logDebug(" -> " + user + " " + permission + " " + grant);
                    }
                    if (principals.contains(user) &&
                            permissions.contains(permission)) {
                        if (log.isDebugEnabled()) {
                            logDebug(" => " + grant);
                        }
                        return grant;
                    }
                }
                /*
                 * Nothing conclusive found, repeat on the parent.
                 */
                ps2.setObject(1, id);
                rs = ps2.executeQuery();
                Serializable newId;
                if (rs.next()) {
                    newId = (Serializable) rs.getObject(1);
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
                    ps3 = connection.prepareStatement("SELECT VERSIONABLEID FROM VERSIONS WHERE ID = ?");
                    ps3.setObject(1, id);
                    rs = ps3.executeQuery();
                    if (rs.next()) {
                        newId = (Serializable) rs.getObject(1);
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
            /*
             * We reached the root, deny access.
             */
            if (log.isDebugEnabled()) {
                logDebug(" => false (root)");
            }
            return false;
        } finally {
            try {
                if (ps1 != null) {
                    ps1.close();
                }
                if (ps2 != null) {
                    ps2.close();
                }
                if (ps3 != null) {
                    ps3.close();
                }
            } finally {
                connection.close();
            }
        }
    }

    /** Bound to a Derby function. */
    public static short isAccessAllowedString(String id, String principals,
            String permissions) throws SQLException {
        return isAccessAllowed(id, split(principals), split(permissions)) ? (short) 1
                : (short) 0;
    }

    /** Bound to a Derby function. */
    public static short isAccessAllowedLong(Long id, String principals,
            String permissions) throws SQLException {
        return isAccessAllowed(id, split(principals), split(permissions)) ? (short) 1
                : (short) 0;
    }

    // -----------------------------------------------------------------
    // -----------------------------------------------------------------

    public static Set<String> split(String string) {
        return split(string, '|');
    }

    public static Set<String> split(String string, char sep) {
        int len = string.length();
        if (len == 0) {
            return Collections.emptySet();
        }
        int end = string.indexOf(sep);
        if (end == -1) {
            return Collections.singleton(string);
        }
        Set<String> set = new HashSet<String>();
        int start = 0;
        do {
            String segment = string.substring(start, end);
            set.add(segment);
            start = end + 1;
            end = string.indexOf(sep, start);
        } while (end != -1);
        if (start < len) {
            set.add(string.substring(start));
        } else {
            set.add("");
        }
        return set;
    }

    // public static class Test extends TestCase {
    // public static void check(String string, String... expected) {
    // assertEquals(new HashSet<String>(Arrays.asList(expected)),
    // split(string));
    // }
    // public void testSplit() throws Exception {
    // check("", new String[0]);
    // check("A", "A");
    // check("A|B|C", "A", "B", "C");
    // check("A||B", "A", "B", "");
    // check("|A|B|C", "A", "B", "C", "");
    // check("A|B|C|", "A", "B", "C", "");
    // check("||", "");
    // }
    // }

}
