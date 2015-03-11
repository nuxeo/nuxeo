/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * Gets the information needed for the Nuxeo Drive client update:
 * <ul>
 * <li>Server version</li>
 * <li>Nuxeo Drive update site URL</li>
 * </ul>
 * 
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveGetClientUpdateInfo.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get client update information")
public class NuxeoDriveGetClientUpdateInfo {

    public static final String ID = "NuxeoDrive.GetClientUpdateInfo";

    @OperationMethod
    public Blob run() throws ClientException, IOException {

        String serverVersion = Framework.getProperty("org.nuxeo.ecm.product.version");
        String updateSiteURL = Framework.getProperty("org.nuxeo.drive.update.site.url");
        String betaUpdateSiteURL = Framework.getProperty("org.nuxeo.drive.beta.update.site.url");
        NuxeoDriveClientUpdateInfo info = new NuxeoDriveClientUpdateInfo(serverVersion, updateSiteURL,
                betaUpdateSiteURL);
        return NuxeoDriveOperationHelper.asJSONBlob(info);
    }

}
