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
 * $Id: NativeQueryStringImpl.java 21689 2007-06-30 22:11:58Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query.impl;

import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;

/**
 * Native query string implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class NativeQueryStringImpl extends AbstractNativeQuery implements
        NativeQueryString {

    private static final long serialVersionUID = 1L;

    protected final String queryString;

    public NativeQueryStringImpl(String backendName, String queryString) {
        super(backendName);
        this.queryString = queryString;
    }

    public NativeQueryStringImpl(String backendName, String queryString,
            SearchPrincipal principal) {
        super(backendName, principal);
        this.queryString = queryString;
    }

    public String getQuery() {
        return queryString;
    }

}
