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
 * $Id: AbstractNativeQuery.java 28925 2008-01-10 14:39:42Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query.impl;

import org.nuxeo.ecm.core.search.api.client.query.AbstractQuery;
import org.nuxeo.ecm.core.search.api.client.query.BaseNativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;

/**
 * Abtract native query.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractNativeQuery extends AbstractQuery implements
        BaseNativeQuery {

    private static final long serialVersionUID = -191269286052787249L;

    protected String backendName;

    protected AbstractNativeQuery() {
    }

    protected AbstractNativeQuery(String backendName) {
        this.backendName = backendName;
    }

    protected AbstractNativeQuery(String backendName, SearchPrincipal principal) {
        super(principal);
        this.backendName = backendName;
    }

    public String getBackendName() {
        return backendName;
    }

}
