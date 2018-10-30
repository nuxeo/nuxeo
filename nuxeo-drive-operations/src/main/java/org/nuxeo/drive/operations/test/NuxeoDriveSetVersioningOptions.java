/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
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
 * @deprecated since 9.1 as the automatic versioning is handled by versioning system, versioning mechanism has been
 *             removed from drive, setting options is not supported anymore
 */
@Deprecated
@Operation(id = NuxeoDriveSetVersioningOptions.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Set versioning options")
public class NuxeoDriveSetVersioningOptions {

    public static final String ID = "NuxeoDrive.SetVersioningOptions";

    @Param(name = "delay", required = false)
    protected String delay;

    @Param(name = "option", required = false)
    protected String option;

    @OperationMethod
    public void run() {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        FileSystemItemAdapterService fileSystemItemAdapterService = Framework.getService(
                FileSystemItemAdapterService.class);
        VersioningFileSystemItemFactory defaultFileSystemItemFactory = (VersioningFileSystemItemFactory) ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory(
                "defaultFileSystemItemFactory");
        if (delay != null) {
            defaultFileSystemItemFactory.setVersioningDelay(Double.parseDouble(delay));
        }
        if (option != null) {
            defaultFileSystemItemFactory.setVersioningOption(VersioningOption.valueOf(option));
        }
    }
}
