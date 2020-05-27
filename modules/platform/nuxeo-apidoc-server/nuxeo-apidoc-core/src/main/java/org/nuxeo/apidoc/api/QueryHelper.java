/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.api;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Helper to generate queries with proper escaping.
 */
public class QueryHelper {

    // utility class
    private QueryHelper() {
    }

    public static final String NOT_DELETED = NXQL.ECM_ISTRASHED + " = 0";

    /**
     * @since 11.1
     */
    public static final String NOT_VERSION = NXQL.ECM_ISVERSION + " = 0";

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString()
     */
    public static String select(String type, DocumentModel doc) {
        return String.format("SELECT * FROM %s WHERE %s STARTSWITH %s AND %s", type, NXQL.ECM_PATH,
                NXQL.escapeString(doc.getPathAsString()), NOT_DELETED);
    }

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString() ORDER BY [...]
     *
     * @since 11.1
     */
    public static String select(String type, DocumentModel doc, String order) {
        return String.format("%s ORDER BY %s", select(type, doc), order);
    }

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString AND prop = value
     */
    public static String select(String type, DocumentModel doc, String prop, String value) {
        return String.format("%s AND %s = %s", select(type, doc), prop, NXQL.escapeString(value));
    }

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString AND prop = value ORDER BY [...]
     */
    public static String select(String type, DocumentModel doc, String prop, String value, String order) {
        return String.format("%s AND %s = %s ORDER BY %s", select(type, doc), prop, NXQL.escapeString(value), order);
    }
}
