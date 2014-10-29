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
import javax.security.auth.login.LoginException;

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
@Features({ ClientLoginFeature.class, SQLDirectoryFeature.class,  })
@LocalDeploy({
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-bundle.xml" })
@ClientLoginFeature.Opener(TestDefaultSQLDirectorySecurity.Opener.class)
public class TestDefaultSQLDirectorySecurity {


    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory directory;

    Session session;

    public class Opener implements ClientLoginFeature.Listener {

        @Override
        public void onLogin(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context) {
            open();
        }

        @Override
        public void onLogout(FeaturesRunner runner, FrameworkMethod method,
                 LoginContext context) {
            close();
        }

    }

    public void open() {
        session = directory.getSession();
    }

    public void close() {
        session.close();
    }


    // Default admin tests
    @Test
    @Identity(administrator=true)
    public void adminCanCreateEntry() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("groups", Arrays.asList("members", "administrators"));

        // When I call the create entry
        DocumentModel entry = session.createEntry(map);

        // I have created an entry
        entry = session.getEntry(entry.getId());
            Assert.assertNotNull(entry);

    }

    @Test
    @Identity(administrator=true)
    public void adminCanDeleteEntry() throws Exception {
        // I can dele   te entry
        DocumentModel entry = session.getEntry("user_1");
        Assert.assertNotNull(entry);
        session.deleteEntry("user_1");
        entry = session.getEntry("user_1");
        Assert.assertNull(entry);
   }

    // Everyone tests
    @Test(expected=DirectorySecurityException.class)
    @Identity(groups={"everyone"})
    public void everyoneCantCreateEntry() throws LoginException {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "should-not-create");
        map.put("password", "should-not-create");
        map.put("groups", Arrays.asList("members", "administrators"));

        // When I call the create entry
        session.createEntry(map);
   }

    @Test
    @Identity(groups={"everyone"})
    public void everyoneCanGetEntry() throws LoginException {
        // When I call get entry
        DocumentModel entry = session.getEntry("user_3");
        Assert.assertNotNull(entry);

    }

    @Test(expected=DirectorySecurityException.class)
    @Identity(groups={"everyone"})
    public void everyoneCantDeleteEntry() throws Exception {
        // When I call delete entry
        DocumentModel entry = session.getEntry("user_3");
        Assert.assertNotNull(entry);
        session.deleteEntry("user_3");
        entry = session.getEntry("user_3");
        Assert.assertNotNull(entry);

    }

    @Test
    @Identity(groups={"everyone"})
    public void everyoneCanSearch() throws LoginException {
        // When I query entry
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("username", "user_3");
        DocumentModelList results = session.query(map);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
   }

}
