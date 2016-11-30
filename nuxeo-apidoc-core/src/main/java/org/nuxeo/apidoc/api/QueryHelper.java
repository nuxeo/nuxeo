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
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Helper to generate queries with proper escaping.
 */
public class QueryHelper {

    // utility class
    private QueryHelper() {
    }

    public static final String NOT_DELETED = NXQL.ECM_LIFECYCLESTATE + " <> '" + LifeCycleConstants.DELETED_STATE + "'";

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
        return "SELECT * FROM " + type + " WHERE " + NXQL.ECM_PATH + " STARTSWITH "
                + NXQL.escapeString(doc.getPathAsString()) + " AND " + NOT_DELETED;
    }

    /**
     * SELECT * FROM type WHERE ecm:path STARTSWITH doc.getPathAsString AND prop = value
     */
    public static String select(String type, DocumentModel doc, String prop, String value) {
        return select(type, doc) + " AND " + prop + " = " + NXQL.escapeString(value);
    }

}
