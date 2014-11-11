/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;

/**
 * Abstract security policy
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public abstract class AbstractSecurityPolicy implements SecurityPolicy {

    public boolean isRestrictingPermission(String permission) {
        // by default, we don't know, so yes
        return true;
    }

    public boolean isExpressibleInQuery() {
        // by default, we don't know, so no
        return false;
    }

    public Transformer getQueryTransformer() {
        // implement this if isExpressibleInQuery is true
        throw new UnsupportedOperationException();
    }

}
