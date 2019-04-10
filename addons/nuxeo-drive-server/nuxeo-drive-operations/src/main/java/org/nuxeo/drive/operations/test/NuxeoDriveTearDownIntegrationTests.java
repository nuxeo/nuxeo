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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
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
    public void run() throws ClientException {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        NuxeoDriveIntegrationTestsHelper.cleanUp(session);
    }
}
