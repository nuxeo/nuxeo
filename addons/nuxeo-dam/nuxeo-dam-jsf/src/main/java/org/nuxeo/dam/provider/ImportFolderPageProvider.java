/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.dam.provider;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;

import java.util.Collection;
import java.util.List;

import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.PermissionFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Custom page provider configuring a post filter to have only folders with
 * WRITE permission and where assets can be created.
 *
 * @since 5.7.2
 */
public class ImportFolderPageProvider extends CoreQueryDocumentPageProvider {

    public static final Filter WRITE_PERMISSION_FILTER = new PermissionFilter(
            WRITE, true);

    public static final Filter ASSET_SUB_TYPE_FILTER = new AssetSubTypeFilter();

    public static final Filter COMPOUND_FILTER = new CompoundFilter(
            WRITE_PERMISSION_FILTER, ASSET_SUB_TYPE_FILTER);

    @Override
    protected Filter getFilter() {
        return COMPOUND_FILTER;
    }

    /**
     * Filter checking if assets can be created in the given document.
     */
    protected static class AssetSubTypeFilter implements Filter {

        @Override
        public boolean accept(DocumentModel doc) {
            TypeManager typeManager = Framework.getLocalService(TypeManager.class);
            DamService damService = Framework.getLocalService(DamService.class);
            List<Type> allowedAssetTypes = damService.getAllowedAssetTypes();
            Collection<Type> allowedSubTypes = typeManager.getAllowedSubTypes(
                    doc.getType(), doc);
            for (Type type : allowedAssetTypes) {
                if (allowedSubTypes.contains(type)) {
                    return true;
                }
            }
            return false;
        }

    }

}
