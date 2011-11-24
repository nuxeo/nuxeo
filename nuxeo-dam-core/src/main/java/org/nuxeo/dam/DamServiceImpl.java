/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.dam;

import org.nuxeo.dam.AssetLibrary;
import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class DamServiceImpl extends DefaultComponent implements DamService {

    public static final String ASSET_LIBRARY_EP = "assetLibrary";

    private AssetLibrary assetLibrary;

    @Override
    public AssetLibrary getAssetLibrary() {
        return assetLibrary;
    }

    @Override
    public String getAssetLibraryPath() {
        AssetLibrary assetLibrary = getAssetLibrary();
        if (assetLibrary == null) {
            throw new ClientRuntimeException("No Asset Library configured.");
        }
        return assetLibrary.getPath();
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
