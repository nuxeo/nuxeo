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
 *     mhilaire
 *     Funsho David
 *
 */

package org.nuxeo.directory.test;

import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, ClientLoginFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.tests:test-directories-security.xml" })
public class TestDirectorySecurityContrib {

    public static final String SUPER_USER = "superUser";

    public static final String READER_USER = "readerUser";

    @Inject
    protected ClientLoginFeature dummyLogin;

    @Inject
    protected DirectoryService directoryService;

    public Session getSession(String directory) throws Exception {
        return directoryService.open(directory);
    }

    @Test
    public void cantCreateEntry() throws Exception {
        // Given a reader user
        dummyLogin.login(READER_USER);
        try (Session userDirSession = getSession(DirectoryFeature.USER_DIRECTORY_NAME)) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("intField", 5L);
            map.put("groups", Arrays.asList("members", "administrators"));
            userDirSession.createEntry(map);
            fail("Should not be able to create entry");
            DocumentModel entry = userDirSession.getEntry("user_0");
            Assert.assertNull(entry);
        } catch (DirectorySecurityException e) {
            // ok
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void canCreateEntry() throws Exception {
        dummyLogin.login(SUPER_USER);
        try (Session userDirSession = getSession(DirectoryFeature.USER_DIRECTORY_NAME)) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("intField", 5L);
            map.put("groups", Arrays.asList("members", "administrators"));
            DocumentModel entry = userDirSession.createEntry(map);
            Assert.assertNotNull(entry);

            entry = userDirSession.getEntry("user_0");
            Assert.assertNotNull(entry);
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void cantGetEntry() throws Exception {
        // Given a user without right
        dummyLogin.login("aUser");
        try (Session userDirSession = getSession(DirectoryFeature.USER_DIRECTORY_NAME)) {
            DocumentModel entry = userDirSession.getEntry("user_1");
            // no DirectorySecurityException here, just null
            Assert.assertNull(entry);
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void canGetEntry() throws Exception {
        // Given a user without right
        dummyLogin.login(READER_USER);
        try (Session userDirSession = getSession(DirectoryFeature.USER_DIRECTORY_NAME)) {
            DocumentModel entry = userDirSession.getEntry("user_1");
            Assert.assertNotNull(entry);
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void cantSearch() throws Exception {
        // Given a user without right
        dummyLogin.login("aUser");
        try (Session userDirSession = getSession(DirectoryFeature.USER_DIRECTORY_NAME)) {
            // When I query entry
            Map<String, Serializable> map = new HashMap<>();
            map.put("username", "user_3");
            DocumentModelList results = userDirSession.query(map);
            // no DirectorySecurityException here, just an empty list
            Assert.assertEquals(0, results.size());
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void canSearch() throws Exception {
        // Given a user without right
        dummyLogin.login(SUPER_USER);
        try (Session userDirSession = getSession(DirectoryFeature.USER_DIRECTORY_NAME)) {
            // When I query entry
            Map<String, Serializable> map = new HashMap<>();
            map.put("username", "user_3");
            DocumentModelList results = userDirSession.query(map);
            Assert.assertEquals(1, results.size());
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void groupCanCreateAndGetEntry() throws Exception {
        // Given a user member of everyone group
        dummyLogin.login("aUserEveryone");
        try (Session groupDirSession = getSession(DirectoryFeature.GROUP_DIRECTORY_NAME)) {

            Map<String, Object> map = new HashMap<>();
            map.put("groupname", "newGroup");
            // When I create an entry
            DocumentModel entry = groupDirSession.createEntry(map);
            Assert.assertNotNull(entry);

            // I can read it too
            entry = groupDirSession.getEntry("newGroup");
            Assert.assertNotNull(entry);
        } finally {
            dummyLogin.logout();
        }
    }

}
