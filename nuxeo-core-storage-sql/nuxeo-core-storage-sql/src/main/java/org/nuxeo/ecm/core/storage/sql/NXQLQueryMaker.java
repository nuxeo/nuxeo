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
package org.nuxeo.ecm.core.storage.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;

/**
 * Compatibility class to avoid breaking existing configurations. Delegates to
 * the new location for this class.
 *
 * @deprecated but kept for backward compatibility
 * @see org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker
 */
@Deprecated
public class NXQLQueryMaker implements QueryMaker {

    private static final Log log = LogFactory.getLog(NXQLQueryMaker.class);

    private final org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker qm;

    public NXQLQueryMaker() {
        qm = new org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker();
        log.warn(NXQLQueryMaker.class.getName()
                + " is deprecated, use "
                + org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.class.getName()
                + " instead in configuration files");
    }

    public String getName() {
        return qm.getName();
    }

    public boolean accepts(String query) {
        return qm.accepts(query);
    }

    public Query buildQuery(SQLInfo sqlInfo, Model model, Session session,
            String query, QueryFilter queryFilter, Object... params)
            throws StorageException {
        return qm.buildQuery(sqlInfo, model, session, query, queryFilter,
                params);
    }

}
