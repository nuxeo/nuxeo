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

package org.nuxeo.dam;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7
 */
public class DamServiceImpl extends DefaultComponent implements DamService {

    public static final String ASSET_LIBRARY_EP = "assetLibrary";

    public static final String ALLOWED_ASSET_TYPES_EP = "allowedAssetTypes";

    protected AssetLibrary assetLibrary;

    protected AllowedAssetTypeRegistry allowedAssetTypeRegistry;

    @Override
    public void activate(ComponentContext context) {
        allowedAssetTypeRegistry = new AllowedAssetTypeRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        allowedAssetTypeRegistry = null;
    }

    @Override
    public AssetLibrary getAssetLibrary() {
        return assetLibrary;
    }

    @Override
    public List<Type> getAllowedAssetTypes() {
        TypeManager typeManager = Framework.getLocalService(TypeManager.class);
        List<String> docTypes = allowedAssetTypeRegistry.getAllowedAssetTypes();
        List<Type> types = new ArrayList<Type>();
        for (String docType : docTypes) {
            Type type = typeManager.getType(docType);
            if (type != null) {
                types.add(type);
            }
        }
        return types;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ASSET_LIBRARY_EP.equals(extensionPoint)) {
            assetLibrary = (AssetLibrary) contribution;
        } else if (ALLOWED_ASSET_TYPES_EP.equals(extensionPoint)) {
            allowedAssetTypeRegistry.addContribution((AllowedAssetTypeDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ASSET_LIBRARY_EP.equals(extensionPoint)) {
            assetLibrary = null;
        } else if (ALLOWED_ASSET_TYPES_EP.equals(extensionPoint)) {
            allowedAssetTypeRegistry.removeContribution((AllowedAssetTypeDescriptor) contribution);
        }
    }
}
