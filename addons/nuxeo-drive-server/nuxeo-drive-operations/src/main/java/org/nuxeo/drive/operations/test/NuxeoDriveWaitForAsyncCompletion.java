/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

/**
 * Waits for asynchronous event listener completion.
 * 
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveWaitForAsyncCompletion.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Wait for async completion")
public class NuxeoDriveWaitForAsyncCompletion {

    public static final String ID = "NuxeoDrive.WaitForAsyncCompletion";

    @OperationMethod
    public void run() throws InterruptedException {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        NuxeoDriveIntegrationTestsHelper.waitForAsyncCompletion();
    }

}
