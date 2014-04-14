/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.center.profile;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @since 5.9.3
 */
public abstract class AbstractUserProfileImporterTest {

    @Inject
    protected RepositorySettings settings;

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserProfileService userProfileService;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected UserManager userManager;

    protected File tmpDir;

    @Before
    public void deleteAllUsers() throws ClientException {
        List<String> userIds = userManager.getUserIds();
        for (String userId : userIds) {
            NuxeoPrincipal principal = userManager.getPrincipal(userId);
            if (principal != null && !(principal instanceof SystemPrincipal)) {
                userManager.deleteUser(userId);
            }
        }
    }

    protected CoreSession openSession(NuxeoPrincipal principal)
            throws ClientException {
        return settings.openSessionAs(principal);
    }

    protected NuxeoPrincipal createUser(String username, String tenant)
            throws ClientException {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", username);
        user.setPropertyValue("user:tenantId", tenant);
        try {
            userManager.createUser(user);
        } catch (UserAlreadyExistsException e) {
            // do nothing
        } finally {
            session.save();
        }
        return userManager.getPrincipal(username);
    }

    protected NuxeoGroup createGroup(String groupName) throws ClientException {
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", groupName);
        String computedGroupName = groupName;
        try {
            computedGroupName = userManager.createGroup(group).getId();
        } finally {
            session.save();
        }
        return userManager.getGroup(computedGroupName);
    }

    protected File getBlobsFolder() {
        return new File(Framework.getProperty("nuxeo.userprofile.blobs.folder"));
    }

}
