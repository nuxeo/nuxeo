/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.api.objects.user.Group;
import org.nuxeo.client.api.objects.user.User;
import org.nuxeo.client.api.objects.user.UserManager;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestUserGroup extends TestBase {

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanGetUser() {
        User user = nuxeoClient.getUserManager().fetchUser("Administrator");
        assertNotNull(user);
        assertEquals("Administrator", user.getUserName());
    }

    @Test
    public void itCanGetGroup() {
        Group group = nuxeoClient.getUserManager().fetchGroup("administrators");
        assertNotNull(group);
        assertEquals("administrators", group.getGroupName());
    }

    @Test
    public void itCanCreateAUser() {
        UserManager userManager = nuxeoClient.getUserManager();
        assertNotNull(userManager);
        User newUser = createUser();
        User user = userManager.createUser(newUser);
        assertNotNull(user);
        assertEquals("toto", user.getId());
        assertEquals("to", user.getLastName());
        assertEquals("toto@nuxeo.com", user.getEmail());
        assertEquals("toto@nuxeo.com", user.getEmail());
    }

    protected User createUser() {
        User newUser = new User();
        newUser.setUserName("toto");
        newUser.setCompany("Nuxeo");
        newUser.setEmail("toto@nuxeo.com");
        newUser.setFirstName("to");
        newUser.setLastName("to");
        List<String> groups = new ArrayList<>();
        groups.add("members");
        newUser.setGroups(groups);
        return newUser;
    }

    @Test
    public void itCanUpdateAUser() {
        UserManager userManager = nuxeoClient.getUserManager();
        User user = userManager.fetchUser("Administrator");
        assertNotNull(user);
        assertEquals("Administrator", user.getUserName());
        user.setCompany("Nuxeo");
        User updatedUser = userManager.updateUser(user);
        assertNotNull(updatedUser);
        assertEquals("Nuxeo", updatedUser.getCompany());
    }

    @Test
    public void itCanDeleteAUser() {
        UserManager userManager = nuxeoClient.getUserManager();
        assertNotNull(userManager);
        User newUser = createUser();
        User user = userManager.createUser(newUser);
        assertNotNull(user);
        user = userManager.fetchUser("toto");
        assertNotNull(user);
        userManager.deleteUser("toto");
        try {
            userManager.fetchUser("toto");
            fail("User should not exist");
        } catch (NuxeoClientException reason) {
            Assert.assertEquals(404, reason.getStatus());
            Assert.assertEquals("user does not exist", reason.getException());
        }
    }

    @Test
    public void itCanCreateAGroup() {
        UserManager userManager = nuxeoClient.getUserManager();
        Group group = createGroup();
        group = userManager.createGroup(group);
        assertNotNull(group);
        assertEquals("totogroup", group.getGroupName());
        assertEquals("Toto Group", group.getGroupLabel());
        User user = userManager.fetchUser("Administrator");
        List<String> groups = user.getGroups();
        assertEquals("totogroup", groups.get(1));
    }

    protected Group createGroup() {
        Group group = new Group();
        group.setGroupName("totogroup");
        group.setGroupLabel("Toto Group");
        List<String> users = new ArrayList<>();
        users.add("Administrator");
        group.setMemberUsers(users);
        return group;
    }

    @Test
    public void itCanUpdateAGroup() {
        UserManager userManager = nuxeoClient.getUserManager();
        Group group = userManager.fetchGroup("administrators");
        assertNotNull(group);
        assertEquals("Administrators group", group.getGroupLabel());
        group.setGroupLabel("Le groupe Admin");
        Group updatedGroup = userManager.updateGroup(group);
        assertNotNull(updatedGroup);
        assertEquals("Le groupe Admin", updatedGroup.getGroupLabel());
    }

    @Test
    public void itCanDeleteAGroup() {
        UserManager userManager = nuxeoClient.getUserManager();
        Group group = createGroup();
        group = userManager.createGroup(group);
        assertNotNull(group);
        userManager.deleteGroup("totogroup");
        try {
            userManager.fetchGroup("totogroup");
            fail("Group should not exist");
        } catch (NuxeoClientException reason) {
            Assert.assertEquals(404, reason.getStatus());
            Assert.assertEquals("group does not exist", reason.getException());
        }
    }

    @Test
    public void itCanAttachAUserToAGroup() {
        UserManager userManager = nuxeoClient.getUserManager();
        Group group = createGroup();
        group = userManager.createGroup(group);
        assertNotNull(group);
        userManager.addUserToGroup("Administrator", "totogroup");
        User user = userManager.fetchUser("Administrator");
        assertEquals(2, user.getGroups().size());
        assertEquals("totogroup", user.getGroups().get(1));
        userManager.attachGroupToUser("members", "Administrator");
        user = userManager.fetchUser("Administrator");
        assertEquals(3, user.getGroups().size());
        assertEquals("members", user.getGroups().get(1));
    }
}