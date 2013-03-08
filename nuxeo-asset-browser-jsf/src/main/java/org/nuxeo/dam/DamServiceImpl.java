/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.dam;

import static org.nuxeo.dam.DamConstants.ASSET_FACET;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7
 */
public class DamServiceImpl extends DefaultComponent implements DamService {

    public static final String ASSET_LIBRARY_EP = "assetLibrary";

    private AssetLibrary assetLibrary;

    @Override
    public AssetLibrary getAssetLibrary() {
        return assetLibrary;
    }

    @Override
    public List<Type> getAllowedAssetTypes() {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        Set<String> docTypes = schemaManager.getDocumentTypeNamesForFacet(ASSET_FACET);
        TypeManager typeManager = Framework.getLocalService(TypeManager.class);

        List<Type> types = new ArrayList<Type>();
        for (String docType : docTypes) {
            types.add(typeManager.getType(docType));
        }
        return types;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (ASSET_LIBRARY_EP.equals(extensionPoint)) {
            assetLibrary = (AssetLibrary) contribution;
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (ASSET_LIBRARY_EP.equals(extensionPoint)) {
            assetLibrary = null;
        }
    }
}
