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
 * $Id: ComposedNXQueryImpl.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query.impl;

import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.query.AbstractQuery;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;

/**
 * Composed NXQL query implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ComposedNXQueryImpl extends AbstractQuery implements
        ComposedNXQuery {

    private static final long serialVersionUID = 512840697498066283L;

    protected SQLQuery query;

    public ComposedNXQueryImpl() {
    }

    public ComposedNXQueryImpl(String nxqlQueryStr) {
        this(SQLQueryParser.parse(nxqlQueryStr));
    }

    public ComposedNXQueryImpl(SQLQuery query) {
        this.query = query;
    }

    public ComposedNXQueryImpl(SQLQuery query, SearchPrincipal principal) {
        super(principal);
        this.query = query;
    }

    public SQLQuery getQuery() {
        return query;
    }

    public void setQuery(SQLQuery query) {
        this.query = query;
    }

    public void parseAndUpdateNXQuery(String nxqlQueryStr) {
        query = SQLQueryParser.parse(nxqlQueryStr);
    }

}
