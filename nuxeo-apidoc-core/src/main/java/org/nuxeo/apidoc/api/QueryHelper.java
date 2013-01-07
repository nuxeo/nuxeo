/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.apidoc.api;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;

/**
 * Helper to generate queries with proper escaping.
 */
public class QueryHelper {

    // utility class
    private QueryHelper() {
    }

    public static final String NOT_DELETED = NXQL.ECM_LIFECYCLESTATE + " <> "
            + LifeCycleConstants.DELETED_STATE;

    /**
     * @deprecated since 5.7, 5.6.0-HF08 use {{@link NXQL#escapeString} instead
     */
    @Deprecated
    public static String quoted(String string) {
        return NXQL.escapeString(string);
    }

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString()
     */
    public static String select(String type, DocumentModel doc) {
        return "SELECT * FROM " + type + " WHERE " + NXQL.ECM_PATH
                + " STARTSWITH " + NXQL.escapeString(doc.getPathAsString());
    }

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString AND prop
     * = value
     */
    public static String select(String type, DocumentModel doc, String prop,
            String value) {
        return select(type, doc) + " AND " + prop + " = "
                + NXQL.escapeString(value);
    }

}
