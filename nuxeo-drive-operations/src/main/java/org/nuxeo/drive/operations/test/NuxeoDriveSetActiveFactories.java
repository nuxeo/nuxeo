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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations.test;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(NuxeoDriveSetActiveFactories.class);

    @Param(name = "profile")
    protected String profile;

    @Param(name = "enable", required = false)
    protected boolean enable = true;

    @OperationMethod
    public boolean run() throws Exception {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        String contrib = null;
        if ("userworkspace".equals(profile)) {
            contrib = "/OSGI-INF/nuxeodrive-hierarchy-userworkspace-contrib.xml";
        } else if ("permission".equals(profile)) {
            contrib = "/OSGI-INF/nuxeodrive-hierarchy-permission-contrib.xml";
        } else {
            log.warn(String.format("No active file system item factory contribution for profile '%s'.", profile));
            return false;
        }
        URL url = NuxeoDriveSetActiveFactories.class.getResource(contrib);
        if (enable) {
            Framework.getRuntime().getContext().deploy(url);
        } else {
            Framework.getRuntime().getContext().undeploy(url);
        }
        FileSystemItemAdapterServiceImpl fileSystemItemAdapterService = (FileSystemItemAdapterServiceImpl) Framework.getService(FileSystemItemAdapterService.class);
        fileSystemItemAdapterService.setActiveFactories();
        return true;
    }

}
