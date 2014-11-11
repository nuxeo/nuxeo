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
 * $Id: AbstractQuery.java 28925 2008-01-10 14:39:42Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query;

/**
 * Abstract query.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractQuery implements BaseQuery {

    private static final long serialVersionUID = -940039025589525163L;

    protected SearchPrincipal principal;

    protected AbstractQuery() {
    }

    protected AbstractQuery(SearchPrincipal principal) {
        this.principal = principal;
    }

    public SearchPrincipal getSearchPrincipal() {
        return principal;
    }

    public void setSearchPrincipal(SearchPrincipal principal) {
        this.principal = principal;
    }

}
