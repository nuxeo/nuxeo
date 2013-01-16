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
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Deletes a user created for Nuxeo Drive test purpose.
 * <p>
 * Fails if the user doesn't exist.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveDeleteTestUser.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Delete test user")
public class NuxeoDriveDeleteTestUser {

    public static final String ID = "NuxeoDrive.DeleteTestUser";

    @Param(name = "userName", required = false)
    protected String userName = "nuxeoDriveTestUser";

    @OperationMethod
    public void run() throws Exception {

        UserManager userManager = Framework.getLocalService(UserManager.class);
        userManager.deleteUser(userName);
    }
}
