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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for the Nuxeo Drive integration tests.
 *
 * @author Antoine Taillefer
 * @see NuxeoDriveSetupIntegrationTests
 * @see NuxeoDriveTearDownIntegrationTests
 */
public final class NuxeoDriveIntegrationTestsHelper {

    static final Log log = LogFactory.getLog(NuxeoDriveIntegrationTestsHelper.class);

    public static final String TEST_USER_NAME_PREFIX = "nuxeoDriveTestUser_";

    public static final String TEST_WORKSPACE_PARENT_NAME = "workspaces";

    public static final String TEST_WORKSPACE_NAME = "nuxeo-drive-test-workspace";

    public static final String TEST_WORKSPACE_TITLE = "Nuxeo Drive Test Workspace";

    public static final String USER_WORKSPACE_PARENT_NAME = "UserWorkspaces";

    private NuxeoDriveIntegrationTestsHelper() {
        // Helper class
    }

    public static void cleanUp(CoreSession session) throws ClientException {

        // Delete test users and their personal workspace if exist
        UserManager userManager = Framework.getLocalService(UserManager.class);
        DocumentModelList testUsers = userManager.searchUsers(TEST_USER_NAME_PREFIX);
        for (DocumentModel testUser : testUsers) {
            String testUserName = (String) testUser.getPropertyValue(userManager.getUserSchemaName()
                    + ":" + userManager.getUserIdField());
            if (userManager.getPrincipal(testUserName) != null) {
                userManager.deleteUser(testUserName);
            }
            String testUserWorkspaceName = IdUtils.generateId(testUserName,
                    "-", false, 30);
            String testUserWorkspacePath = getDefaultDomainPath(session) + "/"
                    + USER_WORKSPACE_PARENT_NAME + "/" + testUserWorkspaceName;
            DocumentRef testUserWorkspaceRef = new PathRef(
                    testUserWorkspacePath);
            if (session.exists(testUserWorkspaceRef)) {
                session.removeDocument(testUserWorkspaceRef);
                session.save();
            }
        }

        // Delete test workspace if exists
        String testWorkspacePath = getDefaultDomainPath(session) + "/"
                + TEST_WORKSPACE_PARENT_NAME + "/" + TEST_WORKSPACE_NAME;
        DocumentRef testWorkspaceDocRef = new PathRef(testWorkspacePath);
        if (session.exists(testWorkspaceDocRef)) {
            session.removeDocument(testWorkspaceDocRef);
            session.save();
        }

        // Invalidate user profile cache
        Framework.getLocalService(UserProfileService.class).clearCache();
    }

    public static String getDefaultDomainPath(CoreSession session)
            throws ClientException {
        String query = "SELECT * FROM Document where ecm:primaryType = 'Domain'";
        DocumentModelList results = session.query(query);
        if (results.isEmpty()) {
            throw new ClientException(String.format(
                    "Found no domains in repository %s",
                    session.getRepositoryName()));
        }
        if (results.size() > 1) {
            log.debug(String.format(
                    "Found more than one domain in repository %s, using first one.",
                    session.getRepositoryName()));
        }
        DocumentModel defaultDomain = results.get(0);
        String defaultDomainPath = defaultDomain.getPathAsString();
        log.debug(String.format("Using default domain %s", defaultDomainPath));
        return defaultDomainPath;
    }

}
