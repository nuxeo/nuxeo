/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ SQLDirectoryFeature.class, ClientLoginFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-security.xml" })
public class TestSQLDirectorySecurityContrib {

    @Inject
    ClientLoginFeature dummyLogin;

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;

    Session userDirSession;

    Session groupDirSession;

    public static final String SUPER_USER = "superUser";

    public static final String READER_USER = "readerUser";

    @Before
    public void setUp() {
        userDirSession = userDir.getSession();
        groupDirSession = groupDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        userDirSession.close();
        groupDirSession.close();
    }

    @Test
    public void cantCreateEntry() throws LoginException {
        // Given a reader user
        dummyLogin.login(READER_USER);
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("intField", Long.valueOf(5));
            map.put("groups", Arrays.asList("members", "administrators"));
            try {
                userDirSession.createEntry(map);
                fail("Should not be able to create entry");
            } catch (DirectorySecurityException e) {
                // ok
            }
            DocumentModel entry = userDirSession.getEntry("user_0");
            Assert.assertNull(entry);
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void canCreateEntry() throws Exception {
        dummyLogin.login(SUPER_USER);
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("intField", Long.valueOf(5));
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
    public void cantGetEntry() throws LoginException {
        // Given a user without right
        dummyLogin.login("aUser");

        DocumentModel entry = userDirSession.getEntry("user_1");
        // no DirectorySecurityException here, just null
        Assert.assertNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void canGetEntry() throws LoginException {
        // Given a user without right
        dummyLogin.login(READER_USER);

        DocumentModel entry = userDirSession.getEntry("user_1");
        Assert.assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void cantSearch() throws LoginException {
        // Given a user without right
        dummyLogin.login("aUser");

        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = userDirSession.query(map);
        // no DirectorySecurityException here, just an empty list
        Assert.assertEquals(0, results.size());

        dummyLogin.logout();
    }

    @Test
    public void canSearch() throws LoginException {
        // Given a user without right
        dummyLogin.login(SUPER_USER);

        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = userDirSession.query(map);
        Assert.assertEquals(1, results.size());

        dummyLogin.logout();
    }

    @Test
    public void groupCanCreateAndGetEntry() throws Exception {
        // Given a user member of everyone group
        dummyLogin.login("aUserEveryone");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupname", "newGroup");
        // When I create an entry
        DocumentModel entry = groupDirSession.createEntry(map);
        Assert.assertNotNull(entry);

        // I can read it too
        entry = groupDirSession.getEntry("newGroup");
        Assert.assertNotNull(entry);

        dummyLogin.logout();
    }

}
