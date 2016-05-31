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
import org.nuxeo.ecm.platform.login.test.DummyNuxeoLoginModule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ SQLDirectoryFeature.class, ClientLoginFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-bundle.xml" })
public class TestSQLDirectorySecurityDefault {

    @Inject
    ClientLoginFeature dummyLogin;

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory directory;

    Session session;

    @Before
    public void setUp() {
        session = directory.getSession();
    }

    @After
    public void tearDown() throws Exception {
        session.close();
    }

    // Default admin tests
    @Test
    public void adminCanCreateEntry() throws Exception {
        // Given the admin user
        dummyLogin.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("groups", Arrays.asList("members", "administrators"));

        // When I call the create entry
        DocumentModel entry = session.createEntry(map);

        // I have created an entry
        entry = session.getEntry(entry.getId());
        Assert.assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void adminCanDeleteEntry() throws Exception {
        // Given the admin user
        dummyLogin.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);

        // I can delete entry
        DocumentModel entry = session.getEntry("user_1");
        Assert.assertNotNull(entry);
        session.deleteEntry("user_1");
        entry = session.getEntry("user_1");
        Assert.assertNull(entry);

        dummyLogin.logout();
    }

    // Everyone tests
    @Test
    public void everyoneCantCreateEntry() throws LoginException {
        // Given a user
        dummyLogin.login("aUser");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "should-not-create");
        map.put("password", "should-not-create");
        map.put("groups", Arrays.asList("members", "administrators"));

        // When I call the create entry
        try {
            session.createEntry(map);
            fail("Should not be able to create entry");
        } catch (DirectorySecurityException e) {
            // ok
        }

        dummyLogin.logout();
    }

    @Test
    public void everyoneCanGetEntry() throws LoginException {
        // Given a user
        dummyLogin.login("aUser");

        // When I call get entry
        DocumentModel entry = session.getEntry("user_3");
        Assert.assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void everyoneCantDeleteEntry() throws Exception {
        dummyLogin.login("aUser");

        // When I call delete entry
        DocumentModel entry = session.getEntry("user_3");
        Assert.assertNotNull(entry);
        try {
            session.deleteEntry("user_3");
            fail("Should not be able to delete entry");
        } catch (DirectorySecurityException e) {
            // ok
        }
        entry = session.getEntry("user_3");
        Assert.assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void everyoneCanSearch() throws LoginException {
        dummyLogin.login("aUser");

        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = session.query(map);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());

        dummyLogin.logout();
    }

}
