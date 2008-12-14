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

import java.sql.SQLException;

/**
 * Functions used as stored procedures for Derby.
 *
 * @author Florent Guillaume
 */
public class DerbyFunctions extends EmbeddedFunctions {

    public static short isInTreeString(String id, String baseId)
            throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    public static short isInTreeLong(Long id, Long baseId) throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    public static short isAccessAllowedString(String id, String principals,
            String permissions) throws SQLException {
        return isAccessAllowed(id, split(principals), split(permissions)) ? (short) 1
                : (short) 0;
    }

    public static short isAccessAllowedLong(Long id, String principals,
            String permissions) throws SQLException {
        return isAccessAllowed(id, split(principals), split(permissions)) ? (short) 1
                : (short) 0;
    }

}
