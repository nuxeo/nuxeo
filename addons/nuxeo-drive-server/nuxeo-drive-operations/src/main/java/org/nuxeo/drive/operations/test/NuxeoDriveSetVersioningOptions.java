/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.runtime.api.Framework;

/**
 * Sets versioning options on the {@link DefaultFileSystemItemFactory}:
 * <ul>
 * <li>delay (default: 1 hour)</li>
 * <li>option (default: MINOR)</li>
 * </ul>
 * 
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveSetVersioningOptions.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Set versioning options")
public class NuxeoDriveSetVersioningOptions {

    public static final String ID = "NuxeoDrive.SetVersioningOptions";

    @Param(name = "delay", required = false)
    protected String delay;

    @Param(name = "option", required = false)
    protected String option;

    @OperationMethod
    public void run() throws ClientException {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        FileSystemItemAdapterService fileSystemItemAdapterService = Framework.getLocalService(FileSystemItemAdapterService.class);
        VersioningFileSystemItemFactory defaultFileSystemItemFactory = (VersioningFileSystemItemFactory) ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory("defaultFileSystemItemFactory");
        if (delay != null) {
            defaultFileSystemItemFactory.setVersioningDelay(Double.parseDouble(delay));
        }
        if (option != null) {
            defaultFileSystemItemFactory.setVersioningOption(VersioningOption.valueOf(option));
        }
    }
}
