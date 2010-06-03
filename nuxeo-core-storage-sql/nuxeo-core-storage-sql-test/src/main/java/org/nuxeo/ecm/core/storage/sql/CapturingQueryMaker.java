/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;

/**
 * A dummy QueryMaker usable to capture the low level sqlInfo, model and session
 * from a high-level session, in order to further test QueryMakers.
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

    public String getName() {
        return TYPE;
    }

    public boolean accepts(String queryType) {
        return TYPE.equals(queryType);
    }

    public Query buildQuery(SQLInfo sqlInfo, Model model,
            PathResolver pathResolver, String query, QueryFilter queryFilter,
            Object... params) throws StorageException {
        Captured captured = (Captured) params[0];
        captured.sqlInfo = sqlInfo;
        captured.model = model;
        captured.pathResolver = pathResolver;
        return null;
    }
}
