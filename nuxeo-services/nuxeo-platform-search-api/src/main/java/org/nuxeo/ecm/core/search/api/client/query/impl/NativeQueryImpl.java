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
 * $Id: NativeQueryImpl.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query.impl;

import java.io.Serializable;

import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;

/**
 * Native query implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class NativeQueryImpl extends AbstractNativeQuery implements NativeQuery {

    private static final long serialVersionUID = -1343898015940639544L;

    protected Serializable queryObject;

    public NativeQueryImpl() {
    }

    public NativeQueryImpl(Serializable queryObject, String backendName) {
        super(backendName);
        this.queryObject = queryObject;
    }

    public NativeQueryImpl(Serializable queryObject, String backendName,
            SearchPrincipal principal) {
        super(backendName, principal);
        this.queryObject = queryObject;
    }

    public Serializable getQuery() {
        return queryObject;
    }

}
