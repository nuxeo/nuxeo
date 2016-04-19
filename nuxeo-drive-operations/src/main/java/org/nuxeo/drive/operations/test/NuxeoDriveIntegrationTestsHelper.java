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

import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
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

    public static final String TEST_USER_NAME_PREFIX = "drive";

    public static final String TEST_WORKSPACE_PARENT_NAME = "workspaces";

    public static final String TEST_WORKSPACE_NAME = "nuxeo-drive-test-workspace";

    public static final String TEST_WORKSPACE_TITLE = "Nuxeo Drive Test Workspace";

    public static final String USER_WORKSPACE_PARENT_NAME = "UserWorkspaces";

    private NuxeoDriveIntegrationTestsHelper() {
        // Helper class
    }

    public static void cleanUp(CoreSession session) {

        // Delete test users and their personal workspace if exist
        UserManager userManager = Framework.getLocalService(UserManager.class);
        DocumentModelList testUsers = userManager.searchUsers(TEST_USER_NAME_PREFIX);
        for (DocumentModel testUser : testUsers) {
            String testUserName = (String) testUser.getPropertyValue(userManager.getUserSchemaName() + ":"
                    + userManager.getUserIdField());
            if (userManager.getPrincipal(testUserName) != null) {
                userManager.deleteUser(testUserName);
            }
            String testUserWorkspaceName = IdUtils.generateId(testUserName, "-", false, 30);
            String testUserWorkspacePath = getDefaultDomainPath(session) + "/" + USER_WORKSPACE_PARENT_NAME + "/"
                    + testUserWorkspaceName;
            DocumentRef testUserWorkspaceRef = new PathRef(testUserWorkspacePath);
            if (session.exists(testUserWorkspaceRef)) {
                session.removeDocument(testUserWorkspaceRef);
                session.save();
            }
        }

        // Delete test workspace if exists
        String testWorkspacePath = getDefaultDomainPath(session) + "/" + TEST_WORKSPACE_PARENT_NAME + "/"
                + TEST_WORKSPACE_NAME;
        DocumentRef testWorkspaceDocRef = new PathRef(testWorkspacePath);
        if (session.exists(testWorkspaceDocRef)) {
            session.removeDocument(testWorkspaceDocRef);
            session.save();
        }

        // Invalidate user profile cache
        Framework.getLocalService(UserProfileService.class).clearCache();
    }

    public static String getDefaultDomainPath(CoreSession session) {
        String query = "SELECT * FROM Document where ecm:primaryType = 'Domain'";
        DocumentModelList results = session.query(query);
        if (results.isEmpty()) {
            throw new NuxeoException(String.format("Found no domains in repository %s", session.getRepositoryName()));
        }
        if (results.size() > 1) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Found more than one domain in repository %s, using first one.",
                        session.getRepositoryName()));
            }
        }
        DocumentModel defaultDomain = results.get(0);
        String defaultDomainPath = defaultDomain.getPathAsString();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Using default domain %s", defaultDomainPath));
        }
        return defaultDomainPath;
    }

    public static void checkOperationAllowed() {
        if (!(Framework.isDevModeSet() || Framework.isTestModeSet() || Framework.getProperty("org.nuxeo.ecm.tester.name") != null)) {
            throw new UnsupportedOperationException("This operation cannot be run unless test mode is set.");
        }
    }

    public static void waitForAsyncCompletion() throws InterruptedException {
        if (!Framework.getService(WorkManager.class).awaitCompletion(20, TimeUnit.SECONDS)) {
            throw new AssertionError("Cannot synch with work manager in 20 seconds");
        }
    }

    public static void waitForAuditIngestion() throws InterruptedException {
        if (Framework.getService(AuditLogger.class).await(20, TimeUnit.SECONDS)) {
            throw new AssertionError("Cannot synch with work manager in 20 seconds");
        }
    }

}
