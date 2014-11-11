/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.provider;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;

import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.impl.PermissionFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * @since 5.9.6
 */
public class ImportFolderPageProvider extends CoreQueryDocumentPageProvider {

    public static final Filter ADD_CHILDREN_PERMISSION_FILTER = new PermissionFilter(
            ADD_CHILDREN, true);

    @Override
    protected Filter getFilter() {
        return ADD_CHILDREN_PERMISSION_FILTER;
    }
}
