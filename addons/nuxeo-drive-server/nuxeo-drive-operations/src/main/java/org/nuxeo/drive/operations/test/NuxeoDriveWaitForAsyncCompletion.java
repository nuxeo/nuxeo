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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
    public void run() throws InterruptedException, ExecutionException, TimeoutException {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        NuxeoDriveIntegrationTestsHelper.waitForAsyncCompletion();
        NuxeoDriveIntegrationTestsHelper.waitForAuditIngestion();
    }

}
