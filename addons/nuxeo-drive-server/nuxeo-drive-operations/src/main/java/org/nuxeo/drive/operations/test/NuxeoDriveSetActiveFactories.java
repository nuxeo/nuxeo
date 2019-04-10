/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.operations.test;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.runtime.api.Framework;

/**
 * Activates / deactivates the {@link FileSystemItem} factories of the given profile.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveSetActiveFactories.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Activate or deactivate file system item factories")
public class NuxeoDriveSetActiveFactories {

    public static final String ID = "NuxeoDrive.SetActiveFactories";

    private static final Logger log = LogManager.getLogger(NuxeoDriveSetActiveFactories.class);

    @Param(name = "profile")
    protected String profile;

    @Param(name = "enable", required = false)
    protected boolean enable = true;

    @OperationMethod
    public boolean run() throws Exception {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        String contrib;
        if ("userworkspace".equals(profile)) {
            contrib = "/OSGI-INF/nuxeodrive-hierarchy-userworkspace-contrib.xml";
        } else if ("permission".equals(profile)) {
            contrib = "/OSGI-INF/nuxeodrive-hierarchy-permission-contrib.xml";
        } else {
            log.warn("No active file system item factory contribution for profile '{}'.", profile);
            return false;
        }
        URL url = NuxeoDriveSetActiveFactories.class.getResource(contrib);
        try {
            if (enable) {
                Framework.getRuntime().getContext().deploy(url);
            } else {
                Framework.getRuntime().getContext().undeploy(url);
            }
        } finally {
            Framework.getRuntime().getComponentManager().unstash();
        }
        FileSystemItemAdapterServiceImpl fileSystemItemAdapterService = (FileSystemItemAdapterServiceImpl) Framework.getService(
                FileSystemItemAdapterService.class);
        fileSystemItemAdapterService.setActiveFactories();
        return true;
    }

}
