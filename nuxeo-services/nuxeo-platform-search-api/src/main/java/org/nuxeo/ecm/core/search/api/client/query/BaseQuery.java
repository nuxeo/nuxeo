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
 * $Id: BaseQuery.java 19476 2007-05-27 10:35:17Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query;

import java.io.Serializable;

/**
 * Base interface for NXSearch queries.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface BaseQuery extends Serializable {

    /**
     * Returns the caller principal who performed the query.
     *
     * @return the caller principal who performed the query.
     */
    SearchPrincipal getSearchPrincipal();

    /**
     * Set the caller principal who performed the query.
     *
     * @param principal : the caller principal
     */
    void setSearchPrincipal(SearchPrincipal principal);

}
