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

import java.io.StringWriter;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Creates a user with the given parameters for Nuxeo Drive test purpose. The
 * user belongs to the members group.
 * <p>
 * Returns the user's password as a JSON string.
 * <p>
 * Fails if the user exists.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveCreateTestUser.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Create test user")
public class NuxeoDriveCreateTestUser {

    public static final String ID = "NuxeoDrive.CreateTestUser";

    @Param(name = "userName", required = false)
    protected String userName = "nuxeoDriveTestUser";

    @Param(name = "firstName", required = false)
    protected String firstName = "NuxeoDrive";

    @Param(name = "lastName", required = false)
    protected String lastName = "TestUser";

    @OperationMethod
    public Blob run() throws Exception {

        UserManager userManager = Framework.getLocalService(UserManager.class);
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);

        // If a user exists with the given user name throw an exception
        if (userManager.getPrincipal(userName) != null) {
            throw new OperationException(String.format(
                    "A user with username = %s already exists.", userName));
        }

        String userSchemaName = userManager.getUserSchemaName();
        String userNameField = directoryService.getDirectoryIdField(userManager.getUserDirectoryName());
        String passwordField = directoryService.getDirectoryPasswordField(userManager.getUserDirectoryName());

        // Generate random password
        String password = UUID.randomUUID().toString();

        // Create test user
        DocumentModel testUserModel = userManager.getBareUserModel();
        testUserModel.setProperty(userSchemaName, userNameField, userName);
        testUserModel.setProperty(userSchemaName, "firstName", firstName);
        testUserModel.setProperty(userSchemaName, "lastName", lastName);
        testUserModel.setProperty(userSchemaName, passwordField, password);
        testUserModel.setProperty(userSchemaName, "groups",
                new String[] { "members" });
        testUserModel = userManager.createUser(testUserModel);

        // Write response
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, password);
        return StreamingBlob.createFromString(writer.toString(),
                "application/json");
    }
}
