/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ SQLDirectoryFeature.class, ClientLoginFeature.class })
@LocalDeploy({
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-security.xml" })
public class TestContribSQLDirectorySecurity {

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
        //Given a reader user
        dummyLogin.loginAs(READER_USER);
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("intField", Long.valueOf(5));
        map.put("groups", Arrays.asList("members", "administrators"));
        DocumentModel entry = userDirSession.createEntry(map);
        Assert.assertNull(entry);
        
        entry = userDirSession.getEntry("user_0");
        Assert.assertNull(entry);
        
        dummyLogin.logout();
    }

    @Test
    public void canCreateEntry() throws Exception {
        dummyLogin.loginAs(SUPER_USER);
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("intField", Long.valueOf(5));
        map.put("groups", Arrays.asList("members", "administrators"));
        DocumentModel entry = userDirSession.createEntry(map);
        Assert.assertNotNull(entry);
        
        entry = userDirSession.getEntry("user_0");
        Assert.assertNotNull(entry);
        
        dummyLogin.logout();
    }

    @Test
    public void cantGetEntry() throws LoginException {
        //Given a user without right
        dummyLogin.loginAs("aUser");
        
        DocumentModel entry = userDirSession.getEntry("user_1");
        Assert.assertNull(entry);
        
        dummyLogin.logout();
    }

    @Test
    public void canGetEntry() throws LoginException {
        //Given a user without right
        dummyLogin.loginAs(READER_USER);
        
        DocumentModel entry = userDirSession.getEntry("user_1");
        Assert.assertNotNull(entry);
        
        dummyLogin.logout();
    }

    @Test
    public void cantSearch() throws LoginException {
        //Given a user without right
        dummyLogin.loginAs("aUser");
        
        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = userDirSession.query(map);
        Assert.assertEquals(0, results.size());
        
        dummyLogin.logout();
    }

    @Test
    public void canSearch() throws LoginException {
        //Given a user without right
        dummyLogin.loginAs(SUPER_USER);
        
        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = userDirSession.query(map);
        Assert.assertEquals(1, results.size());
        
        dummyLogin.logout();
    }
    
    @Test
    public void groupCanCreateAndGetEntry() throws Exception {
        //Given a user member of everyone group
        dummyLogin.loginAs("aUserEveryone");
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupname", "newGroup");
        //When I create an entry
        DocumentModel entry = groupDirSession.createEntry(map);
        Assert.assertNotNull(entry);
        
        //I can read it too
        entry = groupDirSession.getEntry("newGroup");
        Assert.assertNotNull(entry);
        
        dummyLogin.logout();
    }

}
