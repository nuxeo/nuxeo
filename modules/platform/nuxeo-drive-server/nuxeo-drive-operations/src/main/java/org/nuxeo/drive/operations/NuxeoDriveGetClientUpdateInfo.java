/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.io.IOException;

import org.nuxeo.common.Environment;
import org.nuxeo.drive.NuxeoDriveConstants;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

/**
 * Gets the information needed for the Nuxeo Drive client update:
 * <ul>
 * <li>Server version</li>
 * <li>Nuxeo Drive update site URL</li>
 * <li>Nuxeo Drive beta update site URL</li>
 * </ul>
 *
 * @author Antoine Taillefer
 * @deprecated since 10.3
 */
@Deprecated
@Operation(id = NuxeoDriveGetClientUpdateInfo.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get client update information", description = "Get the information needed for the Nuxeo Drive client update." //
        + " Return the result as a JSON blob.", deprecatedSince = "10.3")
public class NuxeoDriveGetClientUpdateInfo {

    public static final String ID = "NuxeoDrive.GetClientUpdateInfo";

    @OperationMethod
    public Blob run() throws IOException {
        String serverVersion = Framework.getProperty(Environment.DISTRIBUTION_VERSION);
        String updateSiteURL = Framework.getProperty(NuxeoDriveConstants.UPDATE_SITE_URL_PROP_KEY);
        String betaUpdateSiteURL = Framework.getProperty(NuxeoDriveConstants.BETA_UPDATE_SITE_URL_PROP_KEY);
        NuxeoDriveClientUpdateInfo info = new NuxeoDriveClientUpdateInfo(serverVersion, updateSiteURL,
                betaUpdateSiteURL);
        return Blobs.createJSONBlobFromValue(info);
    }

}
