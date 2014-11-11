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

/**
 * Functions used as stored procedure for Derby.
 *
 * @author Florent Guillaume
 */
public class DerbyFunctions {

    /**
     * Checks if an id is a (strict) descendant of a given base id.
     *
     * @param id the id to check for
     * @param baseId the base id
     */
    public static boolean isInTree(Serializable id, Serializable baseId)
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
    public static short isInTreeLog(Long id, Long baseId) throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    // -----------------------------------------------------------------
    // -----------------------------------------------------------------

    /**
     * Checks if a document can be browsed.
     */
    public static boolean canBrowse(Serializable id) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        try {
            PreparedStatement ps1 = conn.prepareStatement("VALUES 1+1");
            // ps1.setInt(1, p1);
            ps1.executeQuery();
            // data1[0] = ps1.executeQuery();
        } finally {
            conn.close();
        }

        return true;
    }

    /** Bound to a Derby function. */
    public static short canBrowseString(String id) throws SQLException {
        return canBrowse(id) ? (short) 1 : (short) 0;
    }

    /** Bound to a Derby function. */
    public static short canBrowseLong(Long id) throws SQLException {
        return canBrowse(id) ? (short) 1 : (short) 0;
    }

}
