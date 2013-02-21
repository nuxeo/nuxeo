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
package org.nuxeo.drive.hierarchy.permission;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the permission based hierarchy.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.drive.core", "org.nuxeo.drive.operations",
        "org.nuxeo.drive.hiererchy.permission" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestPermissionHierarchy {

    private static final String PERMISSION_TOP_LEVEL_FOLDER_ITEM_ID_SUFFIX = "PermissionTopLevelFolderItemFactory#";

    private static final String USER_WORKSPACE_FOLDER_ITEM_ID_PREFIX = "userWorkspaceFolderItemFactory#test#";

    private static final String PERMISSION_SYNC_ROOT_PARENT_FOLDER_ITEM_ID_PREFIX = "permissionSyncRootParentFolderItemFactory#test#";

    private static final String PERMISSION_SYNC_ROOT_FOLDER_ITEM_ID_PREFIX = "permissionSyncRootFolderItemFactory#test#";

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel file1;

    protected DocumentModel file2;

    protected DocumentModel file3;

    protected DocumentModel file4;

    protected DocumentModel subFolder1;

    protected Session clientSession;

    protected ObjectMapper mapper;

    /**
     * Initializes the test hierarchy.
     *
     * <pre>
     * Server side for user1
     * ==============================
     *
     * user1 (user workspace)
     *   |-- user1File1
     *   |-- user1File2
     *   |-- user1Folder1 (registered as a synchronization root with ReadWrite permission for user2)
     *   |     |-- user1File3
     *   |-- user1Folder2
     *   |     |-- user1File4
     *
     * Server side for user2
     * ==============================
     *
     * user2 (user workspace)
     *   |-- user2File1
     *   |-- user2File2
     *   |-- user2Folder1
     *   |     |-- user2File3
     *   |-- user2Folder2 (registered as a synchronization root with ReadWrite permission for user1)
     *   |     |-- user2File4
     *
     * Expected client side for user1
     * ==============================
     *
     * Nuxeo Drive
     *   |-- My Documents
     *   |     |-- user1File1
     *   |     |-- user1File2
     *   |     |-- user1Folder1
     *   |           |-- user1File3
     *   |     |-- user1Folder2
     *   |           |-- user1File4
     *   |
     *   |-- Other Documents
     *   |     |-- user2Folder2
     *   |           |-- user2File4
     *
     * Expected client side for user2
     * ==============================
     *
     * Nuxeo Drive
     *   |-- My Documents
     *   |     |-- user2File1
     *   |     |-- user2File2
     *   |     |-- user2Folder1
     *   |           |-- user2File3
     *   |     |-- user2Folder2
     *   |           |-- user2File4
     *   |
     *   |-- Other Documents
     *   |     |-- user1Folder1
     *   |           |-- user1File3
     * </pre>
     */
    @Before
    public void init() throws Exception {
        // TODO
    }

    @Test
    public void testPermissionHierarchy() throws Exception {
        // TODO
    }

    protected void createUser(String userName, String password)
            throws ClientException {
        org.nuxeo.ecm.directory.Session userDir = directoryService.getDirectory(
                "userDirectory").getSession();
        try {
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("username", userName);
            user.put("password", password);
            userDir.createEntry(user);
        } finally {
            userDir.close();
        }
    }

    protected void deleteUser(String userName) throws ClientException {
        org.nuxeo.ecm.directory.Session userDir = directoryService.getDirectory(
                "userDirectory").getSession();
        try {
            userDir.deleteEntry(userName);
        } finally {
            userDir.close();
        }
    }

    protected void setPermission(DocumentModel doc, String userName,
            String permission, boolean isGranted) throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected void resetPermissions(DocumentModel doc, String userName)
            throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        Iterator<ACE> localACLIt = localACL.iterator();
        while (localACLIt.hasNext()) {
            ACE ace = localACLIt.next();
            if (userName.equals(ace.getUsername())) {
                localACLIt.remove();
            }
        }
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

}
