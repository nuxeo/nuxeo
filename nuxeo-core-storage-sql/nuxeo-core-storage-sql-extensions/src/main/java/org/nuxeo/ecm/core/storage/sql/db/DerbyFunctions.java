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

    public static short matchesFullTextDerby(String fulltext, String query) {
        return matchesFullText(fulltext, query) ? (short) 1 : (short) 0;
    }

}
