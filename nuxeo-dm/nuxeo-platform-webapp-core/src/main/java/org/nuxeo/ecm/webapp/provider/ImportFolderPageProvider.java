/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @since 6.0
 */
public class ImportFolderPageProvider extends CoreQueryDocumentPageProvider {

    private static final long serialVersionUID = 1L;

    public static final Filter ADD_CHILDREN_PERMISSION_FILTER = new PermissionFilter(ADD_CHILDREN, true);

    @Override
    protected Filter getFilter() {
        return ADD_CHILDREN_PERMISSION_FILTER;
    }
}
