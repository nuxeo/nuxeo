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
import javax.security.auth.login.LoginContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature.Identity;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ ClientLoginFeature.class, SQLDirectoryFeature.class })
@LocalDeploy({
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-security.xml" })
@ClientLoginFeature.Opener(TestContribSQLDirectorySecurity.Opener.class)
public class TestContribSQLDirectorySecurity {


    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;


    public static final String SUPER_USER = "superUser";

    public static final String READER_USER = "readerUser";

    public static final String USER_EVERYONE = "aUserEveryone";

    protected Session userDirSession;

    protected Session groupDirSession;

    protected void open() {
        userDirSession = userDir.getSession();
        groupDirSession = groupDir.getSession();
    }

    protected void close() {
        userDirSession.close();
        groupDirSession.close();
    }

    public class Opener implements ClientLoginFeature.Listener {

        @Override
        public void onLogin(FeaturesRunner runner, FrameworkMethod method, LoginContext context) {
            open();
        }

        @Override
        public void onLogout(FeaturesRunner runner, FrameworkMethod method, LoginContext context) {
            close();
        }

    }

    @Test(expected = DirectorySecurityException.class)
    @ClientLoginFeature.Identity(name = READER_USER)
    public void cantCreateEntry() throws Exception {
        // Given a reader user
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("intField", Long.valueOf(5));
        map.put("groups", Arrays.asList("members", "administrators"));
        userDirSession.createEntry(map);
    }

    @Test
    @Identity(name=SUPER_USER)
    public void canCreateEntry() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("intField", Long.valueOf(5));
        map.put("groups", Arrays.asList("members", "administrators"));
        DocumentModel entry = userDirSession.createEntry(map);
        Assert.assertNotNull(entry);

        entry = userDirSession.getEntry("user_0");
        Assert.assertNotNull(entry);
    }

    @Test(expected=DirectorySecurityException.class)
    public void cantGetEntry() throws Exception {
        userDirSession.getEntry("user_1");
    }

    @Test
    @Identity(name = READER_USER)
    public void canGetEntry() throws Exception {
        DocumentModel entry = userDirSession.getEntry("user_1");
        Assert.assertNotNull(entry);
    }

    @Test(expected=DirectorySecurityException.class)
    public void cantSearch() throws Exception {
        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = userDirSession.query(map);
        Assert.assertEquals(0, results.size());
    }

    @Test
    @Identity(name = READER_USER)
    public void canSearch() throws Exception {
        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = userDirSession.query(map);
        Assert.assertEquals(1, results.size());
    }

    @Test
    @Identity(groups={"everyone"})
    public void groupCanCreateAndGetEntry() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupname", "newGroup");
        // When I create an entry
        DocumentModel entry = groupDirSession.createEntry(map);
        Assert.assertNotNull(entry);

        // I can read it too
        entry = groupDirSession.getEntry("newGroup");
        Assert.assertNotNull(entry);
    }

}
