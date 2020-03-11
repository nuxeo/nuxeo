/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
public class TestUserToGroupObject {

    @Inject
    protected UserManager userManager;

    protected static void assertSetsEquals(List<String> expected, List<String> actual) {
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }
    protected void createUsersAndGroups() {
        createUser("user1", "User1", "User1");
        createGroup("group1", "Group 1");
        createGroup("group2", "Group 2");
        createGroup("group3", "Group 3");

        // add user1 to group1 and group2
        NuxeoPrincipal principal = userManager.getPrincipal("user1");
        principal.setGroups(Arrays.asList("group1", "group2"));
        userManager.updateUser(principal.getModel());
    }

    protected void createUser(String userId, String firstName, String lastName) {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", userId);
        userModel.setProperty("user", "firstName", firstName);
        userModel.setProperty("user", "lastName", lastName);
        userModel.setProperty("user", "password", userId);
        userManager.createUser(userModel);
    }

    protected void createGroup(String groupId, String groupLabel) {
        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupId);
        groupModel.setProperty("group", "grouplabel", groupLabel);
        groupModel.setProperty("group", "description", "description of " + groupId);
        groupModel = userManager.createGroup(groupModel);
    }

    @Test
    public void testAddUserToGroupRegular() {
        testAddUserToGroup(false);
    }

    @Test
    public void testAddUserToGroupReadOnly() {
        testAddUserToGroup(true);
    }

    @Test
    public void testRemoveUserFromGroupRegular() {
        testRemoveUserFromGroup(false);
    }

    @Test
    public void testRemoveUserFromGroupReadOnly() {
        testRemoveUserFromGroup(true);
    }

    protected void testAddUserToGroup(boolean makeUserReadOnly) {
        createUsersAndGroups();

        // initial state
        List<String> groups = userManager.getPrincipal("user1").getGroups();
        assertSetsEquals(Arrays.asList("group1", "group2"), groups);

        NuxeoPrincipal principal = userManager.getPrincipal("user1");
        if (makeUserReadOnly) {
            DocumentModel userModel = principal.getModel();
            userModel.setPropertyValue("groups", (Serializable) Arrays.asList("members"));
            BaseSession.setReadOnlyEntry(userModel);
        }
        NuxeoGroup group = userManager.getGroup("group3");

        // add user to group
        UserToGroupObject utg = new UserToGroupObject();
        utg.addUserToGroup(principal, group);

        // check
        groups = userManager.getPrincipal("user1").getGroups();
        assertSetsEquals(Arrays.asList("group1", "group2", "group3"), groups);
    }

    protected void testRemoveUserFromGroup(boolean makeUserReadOnly) {
        createUsersAndGroups();

        // initial state
        List<String> groups = userManager.getPrincipal("user1").getGroups();
        assertSetsEquals(Arrays.asList("group1", "group2"), groups);

        NuxeoPrincipal principal = userManager.getPrincipal("user1");
        if (makeUserReadOnly) {
            DocumentModel userModel = principal.getModel();
            userModel.setPropertyValue("groups", (Serializable) Arrays.asList("members"));
            BaseSession.setReadOnlyEntry(userModel);
        }
        NuxeoGroup group = userManager.getGroup("group2");

        // remove user from group
        UserToGroupObject utg = new UserToGroupObject();
        utg.removeUserFromGroup(principal, group);

        // check
        groups = userManager.getPrincipal("user1").getGroups();
        assertSetsEquals(Arrays.asList("group1"), groups);
    }

}
