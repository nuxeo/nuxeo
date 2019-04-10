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

import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.TEST_USER_NAME_PREFIX;
import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.TEST_WORKSPACE_NAME;
import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.TEST_WORKSPACE_TITLE;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Sets up the Nuxeo Drive integration tests environment for the given user names by:
 * <ul>
 * <li>Cleaning it up</li>
 * <li>Creating test users belonging to the members group</li>
 * <li>Creating a test workspace</li>
 * <li>Granting the given permission to the test users on the test workspace</li>
 * </ul>
 * Returns the test users' passwords as a JSON comma separated string.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveSetupIntegrationTests.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Setup integration tests")
public class NuxeoDriveSetupIntegrationTests {

    public static final String ID = "NuxeoDrive.SetupIntegrationTests";

    @Context
    protected CoreSession session;

    // Comma separated list of user names
    @Param(name = "userNames")
    protected String userNames;

    // Permission granted to the test users on test workspace
    @Param(name = "permission")
    protected String permission;

    // Put the test users in the members group to enable Read permission to the
    // whole repository.
    @Param(name = "useMembersGroup", required = false)
    protected boolean useMembersGroup = false;

    @OperationMethod
    public Blob run() throws ClientException {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        NuxeoDriveIntegrationTestsHelper.cleanUp(session);

        String[] userNamesArray = StringUtils.split(userNames, ",");
        String[] prefixedUserNames = new String[userNamesArray.length];
        for (int i = 0; i < userNamesArray.length; i++) {
            prefixedUserNames[i] = TEST_USER_NAME_PREFIX + userNamesArray[i].trim();
        }
        String testUserCredentials = createTestUsers(prefixedUserNames);
        createTestWorkspace(prefixedUserNames);

        return StreamingBlob.createFromString(testUserCredentials, "text/plain");
    }

    protected String createTestUsers(String[] testUserNames) throws ClientException {

        StringBuilder testUserCredentials = new StringBuilder();

        UserManager userManager = Framework.getLocalService(UserManager.class);
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        String userSchemaName = userManager.getUserSchemaName();
        String userNameField = directoryService.getDirectoryIdField(userManager.getUserDirectoryName());
        String passwordField = directoryService.getDirectoryPasswordField(userManager.getUserDirectoryName());

        for (int i = 0; i < testUserNames.length; i++) {
            String testUserName = testUserNames[i];

            // Generate random password
            String testUserPassword = UUID.randomUUID().toString().substring(0, 6);

            // Create test user
            DocumentModel testUserModel = userManager.getBareUserModel();
            testUserModel.setProperty(userSchemaName, userNameField, testUserName);
            testUserModel.setProperty(userSchemaName, passwordField, testUserPassword);
            if (useMembersGroup) {
                testUserModel.setProperty(userSchemaName, "groups", new String[] { "members" });
            }
            userManager.createUser(testUserModel);

            // Append test user's credentials
            testUserCredentials.append(testUserName);
            testUserCredentials.append(":");
            testUserCredentials.append(testUserPassword);
            if (i < testUserNames.length - 1) {
                testUserCredentials.append(",");
            }
        }
        return testUserCredentials.toString();
    }

    protected void createTestWorkspace(String[] testUserNames) throws ClientException {

        // Create test workspace
        String testWorkspaceParentPath = NuxeoDriveIntegrationTestsHelper.getDefaultDomainPath(session) + "/"
                + NuxeoDriveIntegrationTestsHelper.TEST_WORKSPACE_PARENT_NAME;
        DocumentModel testWorkspace = session.createDocumentModel(testWorkspaceParentPath, TEST_WORKSPACE_NAME,
                "Workspace");
        testWorkspace.setPropertyValue("dc:title", TEST_WORKSPACE_TITLE);
        session.createDocument(testWorkspace);

        // Grant the given permission to the test users on the test workspace
        String testWorkspacePath = testWorkspaceParentPath + "/" + TEST_WORKSPACE_NAME;
        DocumentRef testWorkspaceDocRef = new PathRef(testWorkspacePath);
        ACP acp = session.getACP(testWorkspaceDocRef);
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        for (String testUserName : testUserNames) {
            localACL.add(new ACE(testUserName, permission, true));
        }
        session.setACP(testWorkspaceDocRef, acp, false);
    }

}
