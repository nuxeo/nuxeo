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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functions used as stored procedures for H2.
 *
 * @author Florent Guillaume
 */
public class H2Functions extends EmbeddedFunctions {

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

}
