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

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;

/**
 * A dummy QueryMaker usable to capture the low level sqlInfo, model and session from a high-level session, in order to
 * further test QueryMakers.
 *
 * @author Florent Guillaume
 */
public class CapturingQueryMaker implements QueryMaker {

    public static final String TYPE = "test-capturing";

    public static class Captured {
        public SQLInfo sqlInfo;

        public Model model;

        public PathResolver pathResolver;
    }

    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public boolean accepts(String queryType) {
        return TYPE.equals(queryType);
    }

    @Override
    public Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver, String query,
            QueryFilter queryFilter, Object... params) {
        Captured captured = (Captured) params[0];
        captured.sqlInfo = sqlInfo;
        captured.model = model;
        captured.pathResolver = pathResolver;
        return null;
    }
}
