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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Tears down the Nuxeo Drive integration tests environment by:
 * <ul>
 * <li>Deleting the test users and their personal workspace</li>
 * <li>Deleting the test workspace</li>
 * </ul>
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveTearDownIntegrationTests.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Tear down integration tests")
public class NuxeoDriveTearDownIntegrationTests {

    public static final String ID = "NuxeoDrive.TearDownIntegrationTests";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        NuxeoDriveIntegrationTestsHelper.cleanUp(session);
    }
}
