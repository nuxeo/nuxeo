/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: CompassBackend.java 15075 2007-03-31 22:45:31Z gracinet $
 */

package org.nuxeo.ecm.core.search.backend.compass;

import java.io.Serializable;

import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.query.AbstractQuery;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;

/**
 * Serializable wrapper for CompassQuery
 * <p>
 * Main use is to rewrapp a parsed query in the resulting @link{ResultSet}.
 * Since Compass queries are not serializable, we actually rewrap the original
 * @link{SQLQuery} or strinq query.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class CompassNativeQuery extends AbstractQuery implements NativeQuery {

    private static final long serialVersionUID = 4876919935022177715L;

    private final Serializable query;

    private final String backendName;

    private final boolean isNxql;

    /**
     * Wraps some NXQL query. If no security check is to be performed,
     * it has to be clearly stated by using a null principal
     *
     * @param query the SQLQuery to wrap
     * @param backendName the name the backend is known as in
     *        {@link org.nuxeo.ecm.core.search.api.client.SearchService}
     * @param principal the search principal to use for security checks.
     */
    public CompassNativeQuery(SQLQuery query, String backendName,
            SearchPrincipal principal) {
        super(principal);
        this.query = query;
        this.backendName = backendName;
        isNxql = true;
    }

    /**
     * Wraps some string query. If no security check is to be performed,
     * it has to be clearly stated by using a null principal
     *
     * @param query the SQLQuery to wrap
     * @param backendName the name the backend for
     *        {@link org.nuxeo.ecm.core.search.api.client.SearchService}
     * @param principal the search principal to use for security checks.
     */
    public CompassNativeQuery(String query, String backendName,
            SearchPrincipal principal) {
        super(principal);
        this.query = query;
        this.backendName = backendName;
        isNxql = false;
    }

    public String getBackendName() {
        return backendName;
    }

    public boolean isNxql() {
        return isNxql;
    }

    public Serializable getQuery() {
        return query;
    }

}
