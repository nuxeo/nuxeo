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

import java.sql.SQLException;

/**
 * Functions used as stored procedures for Derby.
 *
 * @author Florent Guillaume
 */
public class DerbyFunctions extends EmbeddedFunctions {

    public static short isInTreeString(String id, String baseId) throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    public static short isInTreeLong(Long id, Long baseId) throws SQLException {
        return isInTree(id, baseId) ? (short) 1 : (short) 0;
    }

    public static short isAccessAllowedString(String id, String principals, String permissions) throws SQLException {
        return isAccessAllowed(id, split(principals), split(permissions)) ? (short) 1 : (short) 0;
    }

    public static short isAccessAllowedLong(Long id, String principals, String permissions) throws SQLException {
        return isAccessAllowed(id, split(principals), split(permissions)) ? (short) 1 : (short) 0;
    }

    public static short matchesFullTextDerby(String fulltext, String query) {
        return matchesFullText(fulltext, query) ? (short) 1 : (short) 0;
    }

}
